package dk.ek.gruppe2.chooseyourfate.repository.neo4j;

import dk.ek.gruppe2.chooseyourfate.dto.InventoryHasItemResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.ItemResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.LoadoutResponseDTO;
import dk.ek.gruppe2.chooseyourfate.enums.ItemType;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Repository
public class Neo4jLoadoutRepositoryImpl implements Neo4jLoadoutRepository {

    private final Neo4jClient neo4jClient;

    public Neo4jLoadoutRepositoryImpl(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @Override
    public LoadoutResponseDTO getLoadoutByCharacterId(Integer characterId) {
        Integer inventoryId = findInventoryId(characterId); // Finder inventory noden for at sikre os den eksiterer
        List<InventoryItemData> inventoryItemData = findInventoryItems(characterId);

        List<InventoryHasItemResponseDTO> itemsInInventory = inventoryItemData.stream()
                .filter(itemData -> itemData.itemId() != null)
                .map(this::toInventoryHasItemResponseDTO)
                .toList();
        List<ItemResponseDTO> equippedItems = new ArrayList<>();

        EquippedItemData headItem = findEquippedItem(characterId, EquipmentSlotRelation.HEAD);
        EquippedItemData chestItem = findEquippedItem(characterId, EquipmentSlotRelation.CHEST);
        EquippedItemData legsItem = findEquippedItem(characterId, EquipmentSlotRelation.LEGS);

        if (headItem != null) {
            equippedItems.add(toItemResponseDTO(headItem));
        }
        if (chestItem != null) {
            equippedItems.add(toItemResponseDTO(chestItem));
        }
        if (legsItem != null) {
            equippedItems.add(toItemResponseDTO(legsItem));
        }
        return new LoadoutResponseDTO(inventoryId, equippedItems, itemsInInventory);
    }

    private List<InventoryItemData> findInventoryItems(Integer charaterId){
        return new ArrayList<>(neo4jClient.query("""
                        MATCH (:Character {id: $characterId})-[:HAS_INVENTORY]->(inv:Inventory)
                        OPTIONAL MATCH (inv)-[r:CONTAINS]->(item:Item)
                        RETURN inv.id AS inventoryId,
                               item.id AS itemId,
                               item.name AS name,
                               item.description AS description,
                               item.type AS type,
                               r.amount AS amount
                        ORDER BY item.id
                        """).bind(charaterId).to("characterId")
                .fetchAs(InventoryItemData.class)
                .mappedBy(((typeSystem, itemRecord) -> {
                    if (itemRecord.get("itemId").isNull()) {
                        return new InventoryItemData(
                                itemRecord.get("inventoryId").asInt(),
                                null,
                                null,
                                null,
                                null,
                                null

                        );
                    }
                    return new InventoryItemData(
                            itemRecord.get("inventoryId").asInt(),
                            itemRecord.get("itemId").asInt(),
                            itemRecord.get("name").asString(),
                            itemRecord.get("description").asString(),
                            ItemType.valueOf(itemRecord.get("type").asString().toUpperCase()),
                            itemRecord.get("amount").asInt()
                    );
                })).all().stream().toList());
    }

    private EquippedItemData findEquippedItem(Integer characterId, EquipmentSlotRelation relationType) {
        String query = """
            MATCH (c:Character {id: $characterId})-[r:%s]->(item:Item)
            RETURN item.id AS itemId,
                   item.name AS name,
                   item.description AS description,
                   item.type AS type
            """.formatted(relationType.relationshipType());
        return neo4jClient.query(query)
                .bind(characterId).to("characterId")
                .fetchAs(EquippedItemData.class)
                .mappedBy((typeSystem, itemRecord) -> new EquippedItemData(
                        itemRecord.get("itemId").asInt(),
                        itemRecord.get("name").asString(),
                        itemRecord.get("description").asString(),
                        ItemType.valueOf(itemRecord.get("type").asString().toUpperCase())
                ))
                .one()
                .orElse(null);
    }

    private Integer findInventoryId(Integer characterId) {
        return neo4jClient.query("""
                    MATCH (:Character {id: $characterId})-[:HAS_INVENTORY]->(inventory:Inventory)
                    RETURN inventory.id AS inventoryId
                    """)
                .bind(characterId)
                .to("characterId")
                .fetchAs(Integer.class)
                .mappedBy((typeSystem, inventoryRecord) -> inventoryRecord.get("inventoryId").asInt())
                .one()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory not found for character id: " + characterId
                ));
    }

    @Override
    public LoadoutResponseDTO equipItem(Integer characterId, Integer itemId) {
        Integer inventoryId = findInventoryId(characterId);
        EquippedItemData itemToEquip = findItemById(itemId);
        EquipmentSlotRelation relationshipType = resolveEquipmentRelation(itemToEquip.type());
        Integer amountInInventory = findInventoryItemAmount(inventoryId, itemToEquip.itemId());
        if (amountInInventory < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Item is not present in inventory"
            );
        }
        EquippedItemData currentlyEquippedItem = findEquippedItem(characterId, relationshipType);
        decrementInventoryItem(inventoryId, itemToEquip.itemId());
        if(currentlyEquippedItem != null) {
            incrementInventoryItem(inventoryId,currentlyEquippedItem.itemId());
        }
        setEquippedItem(characterId, itemToEquip.itemId(), relationshipType);
        return getLoadoutByCharacterId(characterId);
    }

    @Override
    public LoadoutResponseDTO unequipItem(Integer characterId, Integer itemId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // Find ud af hvilken type item vi forsøger at equippe:
    private EquipmentSlotRelation resolveEquipmentRelation(ItemType itemType) {
        return switch (itemType) {
            case ARMOR_HEAD -> EquipmentSlotRelation.HEAD;
            case ARMOR_CHEST -> EquipmentSlotRelation.CHEST;
            case ARMOR_LEGS -> EquipmentSlotRelation.LEGS;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Item type is not supported for equipment"
            );
        };
    }
    // Har characterens inventory det her item, og hvor mange er der.
    // Vi returner 0 hvis item'et ikke er i inventory
    // (betyder er ikke er nogen relation mellem det item og characters inventory.)
    private Integer findInventoryItemAmount(Integer inventoryId, Integer itemId) {
        return neo4jClient.query("""
                    MATCH (inventory:Inventory {id: $inventoryId})-[relation:CONTAINS]->(item:Item {id: $itemId})
                    RETURN relation.amount AS amount
                    """)
                .bind(inventoryId).to("inventoryId")
                .bind(itemId).to("itemId")
                .fetchAs(Integer.class)
                .mappedBy((typeSystem, inventoryItemRecord) -> inventoryItemRecord.get("amount").asInt())
                .one()
                .orElse(0);
    }
    // Skal bruges til at sikre den item faktisk eksiterer.
    private EquippedItemData findItemById(Integer itemId) {
        return neo4jClient.query("""
                    MATCH (item:Item {id: $itemId})
                    RETURN item.id AS itemId,
                           item.name AS name,
                           item.description AS description,
                           item.type AS type
                    """)
                .bind(itemId).to("itemId")
                .fetchAs(EquippedItemData.class)
                .mappedBy((typeSystem, itemRecord) -> new EquippedItemData(
                        itemRecord.get("itemId").asInt(),
                        itemRecord.get("name").asString(),
                        itemRecord.get("description").asString(),
                        ItemType.valueOf(itemRecord.get("type").asString().toUpperCase())
                ))
                .one()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item not found with id: " + itemId
                ));
    }
    // reducerer amount på relationen mellem inventory og account med 1
    // og i tilfælde af at current amount er 1 bliver relationen slettet.
    private void decrementInventoryItem(Integer inventoryId, Integer itemId) {
        neo4jClient.query("""
                    MATCH (:Inventory {id: $inventoryId})-[relation:CONTAINS]->(:Item {id: $itemId})
                    WITH relation, relation.amount AS oldAmount
                    FOREACH (_ IN CASE WHEN oldAmount > 1 THEN [1] ELSE [] END |
                        SET relation.amount = oldAmount - 1
                    )
                    FOREACH (_ IN CASE WHEN oldAmount = 1 THEN [1] ELSE [] END |
                        DELETE relation
                    )
                    """)
                .bind(inventoryId).to("inventoryId")
                .bind(itemId).to("itemId")
                .run();

//        Har fundet en smartere måde at gøre det på.
//        neo4jClient.query("""
//                    MATCH (inventory:Inventory {id: $inventoryId})-[relation:CONTAINS]->(item:Item {id: $itemId})
//                    WITH relation
//                    WHERE relation.amount > 1
//                    SET relation.amount = relation.amount - 1
//                    """)
//                .bind(inventoryId).to("inventoryId")
//                .bind(itemId).to("itemId")
//                .run();
//
//        neo4jClient.query("""
//                    MATCH (inventory:Inventory {id: $inventoryId})-[relation:CONTAINS]->(item:Item {id: $itemId})
//                    WITH relation
//                    WHERE relation.amount = 1
//                    DELETE relation
//                    """)
//                .bind(inventoryId).to("inventoryId")
//                .bind(itemId).to("itemId")
//                .run();
    }
    // Tjekket om en item allerede er i inventory og ligger en til hvis det er,
    // ellers oprettes en relation med amount = 1
    // merge for at sikre at hvis relationen allerede eksistere addeder den eller
    // ellers starter den på 1.
    private void incrementInventoryItem(Integer inventoryId, Integer itemId) {
        neo4jClient.query("""
                    MATCH (inventory:Inventory {id: $inventoryId})
                    MATCH (item:Item {id: $itemId})
                    MERGE (inventory)-[relation:CONTAINS]->(item)
                    ON CREATE SET relation.amount = 1
                    ON MATCH SET relation.amount = relation.amount + 1
                    """)
                .bind(inventoryId).to("inventoryId")
                .bind(itemId).to("itemId")
                .run();
    }

    // Finder characteren, finder den nye item som skal equippes
    // Finder den gamle relation (hvis den eksitere) og sletter den
    // opretter ny relation.
    private void setEquippedItem(Integer characterId, Integer itemToEquipId, EquipmentSlotRelation slotRelation) {
        String relationshipType = slotRelation.relationshipType();

        String query = """
            MATCH (character:Character {id: $characterId})
            MATCH (item:Item {id: $itemId})
            OPTIONAL MATCH (character)-[oldRelation:%s]->(:Item)
            DELETE oldRelation
            CREATE (character)-[:%s]->(item)
            """.formatted(relationshipType, relationshipType);

        neo4jClient.query(query)
                .bind(characterId).to("characterId")
                .bind(itemToEquipId).to("itemId")
                .run();
    }

    private ItemResponseDTO toItemResponseDTO(EquippedItemData itemData) {
        if (itemData == null) {
            return null;
        }

        return new ItemResponseDTO(
                itemData.itemId(),
                itemData.name(),
                itemData.description(),
                itemData.type()
        );
    }

    private InventoryHasItemResponseDTO toInventoryHasItemResponseDTO(InventoryItemData itemData) {
        return new InventoryHasItemResponseDTO(
                itemData.inventoryId(),
                itemData.amount(),
                new ItemResponseDTO(
                        itemData.itemId(),
                        itemData.name(),
                        itemData.description(),
                        itemData.type()
                )
        );
    }
    private enum EquipmentSlotRelation {
        CHEST("EQUIPPED_CHEST"),
        HEAD("EQUIPPED_HEAD"),
        LEGS("EQUIPPED_LEGS");
        private final String relationshipType;
        EquipmentSlotRelation(String r) { this.relationshipType = r; }
        public String relationshipType() {
            return relationshipType;
        }
    }
    private record EquippedItemData(Integer itemId, String name, String description, ItemType type) {}
    private record InventoryItemData(Integer inventoryId,Integer itemId, String name,String description,ItemType type, Integer amount) {}
}

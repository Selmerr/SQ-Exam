package dk.ek.gruppe2.chooseyourfate.service.migration.mongodb;

import dk.ek.gruppe2.chooseyourfate.model.mongodb.EquipmentMongo;
import dk.ek.gruppe2.chooseyourfate.model.mongodb.InventoryEntryMongo;
import dk.ek.gruppe2.chooseyourfate.model.mongodb.InventoryMongo;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Inventory;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Item;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.EquipmentRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.InventoryHasItemRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.InventoryRepository;
import dk.ek.gruppe2.chooseyourfate.service.migration.IdMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentInventoryMigrationServiceMongo {

    private final EquipmentRepository equipmentRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHasItemRepository inventoryHasItemRepository;
    private final IdMappingService idMappingService;

    public EquipmentMongo transformEquipment(Integer characterId) {
        return equipmentRepository.findById(characterId)
                .map(equipment -> EquipmentMongo.builder()
                        .headItemId(resolveMongoItemId(equipment.getHead()))
                        .chestItemId(resolveMongoItemId(equipment.getChest()))
                        .legsItemId(resolveMongoItemId(equipment.getLegs()))
                        .build())
                .orElseGet(() -> EquipmentMongo.builder().build());
    }

    public InventoryMongo transformInventory(Integer characterId) {
        Inventory inventory = inventoryRepository.findByCharacter_Id(characterId);

        if (inventory == null) {
            return InventoryMongo.builder()
                    .inventoryEntries(List.of())
                    .build();
        }

        List<InventoryEntryMongo> entries = inventoryHasItemRepository
                .findByInventory_Id(inventory.getId())
                .stream()
                .map(inventoryItem -> InventoryEntryMongo.builder()
                        .itemId(resolveMongoItemId(inventoryItem.getItem()))
                        .amount(inventoryItem.getAmount())
                        .build())
                .toList();

        return InventoryMongo.builder()
                .inventoryEntries(entries)
                .build();
    }

    private String resolveMongoItemId(Item item) {
        if (item == null) {
            return null;
        }

        String mongoItemId = idMappingService.get(CollectionNames.ITEMS, item.getId());
        if (mongoItemId == null) {
            throw new IllegalStateException("Missing MongoDB item id mapping for MySQL item id: " + item.getId());
        }

        return mongoItemId;
    }
}

package dk.ek.gruppe2.chooseyourfate.service.mysql;

import dk.ek.gruppe2.chooseyourfate.dto.EquipmentResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.UpdateEquipmentRequestDTO;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.interfaces.EquipmentDataAccess;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Equipment;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Item;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.EquipmentRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SqlEquipmentService implements EquipmentDataAccess {

    private final EquipmentRepository equipmentRepository;
    private final ItemRepository itemRepository;
    private final InventoryService inventoryService;

    public SqlEquipmentService(EquipmentRepository equipmentRepository, ItemRepository itemRepository, InventoryService inventoryService) {
        this.equipmentRepository = equipmentRepository;
        this.itemRepository = itemRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    public List<EquipmentResponseDTO> getAllEquipment() {
        return equipmentRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public EquipmentResponseDTO getEquipmentByCharacterId(Integer characterId) {
        return toDto(getEquipmentEntity(characterId));
    }

    @Override
    public EquipmentResponseDTO updateEquipment(Integer characterId, UpdateEquipmentRequestDTO request) {
        Equipment equipment = getEquipmentEntity(characterId);

        if (request.getHeadItemId() != null &&
                !request.getHeadItemId().equals(equipment.getHead().getId())) {
            equipment.setHead(resolveItem(request.getHeadItemId()));
            inventoryService.removeItem(characterId, request.getHeadItemId());
        }

        if (request.getChestItemId() != null &&
                !request.getChestItemId().equals(equipment.getChest().getId())) {
            equipment.setChest(resolveItem(request.getChestItemId()));
            inventoryService.removeItem(characterId, request.getChestItemId());
        }

        if (request.getLegsItemId() != null &&
                !request.getLegsItemId().equals(equipment.getLegs().getId())) {
            equipment.setLegs(resolveItem(request.getLegsItemId()));
            inventoryService.removeItem(characterId, request.getLegsItemId());
        }

        return toDto(equipmentRepository.save(equipment));
    }

    private Equipment getEquipmentEntity(Integer characterId) {
        return equipmentRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found for character id: " + characterId));
    }

    private Item resolveItem(Integer itemId) {
        if (itemId == null) {
            return null;
        }

        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));
    }

    private EquipmentResponseDTO toDto(Equipment equipment) {
        return new EquipmentResponseDTO(
                equipment.getCharacterId(),
                equipment.getHead() == null ? null : equipment.getHead().getId(),
                equipment.getChest() == null ? null : equipment.getChest().getId(),
                equipment.getLegs() == null ? null : equipment.getLegs().getId()
        );
    }
}

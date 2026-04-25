package dk.ek.gruppe2.chooseyourfate.service.mysql;

import dk.ek.gruppe2.chooseyourfate.dto.ItemRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.ItemResponseDTO;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Item;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.ItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    ItemRepository itemRepository;
    ItemResponseDTO itemResponseDTO;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }
    public List<ItemResponseDTO> getItems() {
        List<Item> items = itemRepository.findAll();
        List<ItemResponseDTO> response = items.stream().map((item -> new ItemResponseDTO(item))).toList();
        return response;
    }

    public ResponseEntity<Boolean> CreateItem(ItemRequestDTO requestDTO) {
        Item item = requestDTO.getItemEntity(requestDTO);
        itemRepository.save(item);
        return ResponseEntity.ok(true);
    }

    public ItemResponseDTO updateItem(ItemRequestDTO requestDTO, Integer itemId) {
        Item item = itemRepository.getReferenceById(itemId);
        item.setName(requestDTO.getName());
        item.setDescription(requestDTO.getDescription());
        item.setType(requestDTO.getType());
        itemRepository.save(item);
        return new ItemResponseDTO(item);
    }

    public void DeleteItem(Integer itemId) {
        itemRepository.deleteById(itemId);
    }
}

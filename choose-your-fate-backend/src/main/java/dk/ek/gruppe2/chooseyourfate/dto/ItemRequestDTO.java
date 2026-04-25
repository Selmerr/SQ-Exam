package dk.ek.gruppe2.chooseyourfate.dto;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemRequestDTO {

    private String name;
    private String description;
    private String type;

    public Item getItemEntity(ItemRequestDTO requestDTO) {
        Item item = new Item();
        item.setName(requestDTO.getName());
        item.setDescription(requestDTO.getDescription());
        item.setType(requestDTO.getType());
        return item;
    }

}

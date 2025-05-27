package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.model.ItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "storage.id", target = "storageId")
    @Mapping(target = "tagIds", ignore = true)
    @Mapping(target = "relatedItemIds", ignore = true)
    @Mapping(target = "imageIds", ignore = true)
    Item toDto(ItemEntity entity);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "storage", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "relatedItems", ignore = true)
    @Mapping(target = "images", ignore = true)
    ItemEntity toEntity(Item dto);
}

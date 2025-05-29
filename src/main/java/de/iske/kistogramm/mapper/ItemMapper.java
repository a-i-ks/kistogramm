package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.model.ItemEntity;
import de.iske.kistogramm.model.TagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Named("mapRelatedEntitiesToIds")
    static Set<Integer> mapRelatedEntitiesToIds(Set<ItemEntity> items) {
        if (items == null) return Set.of();
        return items.stream().map(ItemEntity::getId).collect(Collectors.toSet());
    }

    @Named("mapTagsToIds")
    static Set<Integer> mapTagsToIds(Set<TagEntity> items) {
        if (items == null) return Set.of();
        return items.stream().map(TagEntity::getId).collect(Collectors.toSet());
    }

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "storage.id", target = "storageId")
    @Mapping(target = "tagIds", source = "tags", qualifiedByName = "mapTagsToIds")
    @Mapping(target = "relatedItemIds", source = "relatedItems", qualifiedByName = "mapRelatedEntitiesToIds")
    @Mapping(target = "imageIds", ignore = true)
    Item toDto(ItemEntity entity);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "storage", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "relatedItems", ignore = true) // Handhabung erfolgt im Service
    @Mapping(target = "images", ignore = true)
    ItemEntity toEntity(Item dto);
}

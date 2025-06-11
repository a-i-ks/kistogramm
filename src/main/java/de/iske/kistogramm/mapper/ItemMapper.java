package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.dto.export.ExportItem;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.model.ItemEntity;
import de.iske.kistogramm.model.TagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.UUID;
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

    @Named("mapImagesToIds")
    static Set<Integer> mapImagesToIds(Set<ImageEntity> images) {
        if (images == null) return Set.of();
        return images.stream().map(ImageEntity::getId).collect(Collectors.toSet());
    }

    @Named("mapReceiptsToIds")
    static Set<Integer> mapReceiptsToIds(Set<ImageEntity> images) {
        if (images == null) return Set.of();
        return images.stream().map(ImageEntity::getId).collect(Collectors.toSet());
    }

    @Named("mapTagsToUuids")
    static List<UUID> mapTagsToUuids(Set<TagEntity> tags) {
        if (tags == null) return List.of();
        return tags.stream().map(TagEntity::getUuid).collect(Collectors.toList());
    }

    @Named("mapRelatedEntitiesToUuids")
    static List<UUID> mapRelatedEntitiesToUuids(Set<ItemEntity> items) {
        if (items == null) return List.of();
        return items.stream().map(ItemEntity::getUuid).collect(Collectors.toList());
    }

    @Named("mapImagesToUuids")
    static List<UUID> mapImagesToUuids(Set<ImageEntity> images) {
        if (images == null) return List.of();
        return images.stream().map(ImageEntity::getUuid).collect(Collectors.toList());
    }

    @Named("mapReceiptsToUuids")
    static List<UUID> mapReceiptsToUuids(Set<ImageEntity> images) {
        if (images == null) return List.of();
        return images.stream().map(ImageEntity::getUuid).collect(Collectors.toList());
    }

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "storage.id", target = "storageId")
    @Mapping(target = "tagIds", source = "tags", qualifiedByName = "mapTagsToIds")
    @Mapping(target = "relatedItemIds", source = "relatedItems", qualifiedByName = "mapRelatedEntitiesToIds")
    @Mapping(target = "imageIds", source = "images", qualifiedByName = "mapImagesToIds")
    @Mapping(target = "receiptIds", source = "receipts", qualifiedByName = "mapReceiptsToIds")
    Item toDto(ItemEntity entity);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "storage", ignore = true)
    @Mapping(target = "tags", ignore = true)
    // Related items are processed in the service layer
    @Mapping(target = "relatedItems", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "receipts", ignore = true)
    ItemEntity toEntity(Item dto);

    @Mapping(target = "category", source = "category.uuid")
    @Mapping(target = "storage", source = "storage.uuid")
    @Mapping(target = "tags", qualifiedByName = "mapTagsToUuids")
    @Mapping(target = "relatedItems", qualifiedByName = "mapRelatedEntitiesToUuids")
    @Mapping(target = "images", qualifiedByName = "mapImagesToUuids")
    ExportItem toExportItem(ItemEntity entity);
}

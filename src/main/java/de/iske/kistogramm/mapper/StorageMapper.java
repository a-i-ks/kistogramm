package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.dto.export.ExportStorage;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.model.RoomEntity;
import de.iske.kistogramm.model.StorageEntity;
import de.iske.kistogramm.model.TagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface StorageMapper {

    @Named("mapImagesToIds")
    static List<Integer> mapImagesToIds(Set<ImageEntity> images) {
        if (images == null) return List.of();
        return images.stream().map(ImageEntity::getId).toList();
    }

    @Named("mapRoomToUuid")
    static UUID mapRoomToUuid(RoomEntity entity) {
        if (entity == null || entity.getUuid() == null) return null;
        return entity.getUuid();
    }

    @Named("mapParentStorageToUuid")
    static UUID mapParentStorageToUuid(StorageEntity entity) {
        if (entity == null || entity.getUuid() == null) return null;
        return entity.getUuid();
    }

    @Named("mapImagesToUuids")
    static List<UUID> mapImagesToUuids(Set<ImageEntity> images) {
        if (images == null) return List.of();
        return images.stream().map(ImageEntity::getUuid).toList();
    }

    @Named("mapTagsToUuids")
    static List<UUID> mapTagsToUuids(Set<TagEntity> tags) {
        if (tags == null) return List.of();
        return tags.stream().map(TagEntity::getUuid).toList();
    }

    @Mapping(target = "room", ignore = true)
    @Mapping(target = "parentStorage", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "subStorages", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "images", ignore = true)
    StorageEntity toEntity(Storage dto);

    @Mapping(source = "room.id", target = "roomId")
    @Mapping(source = "parentStorage.id", target = "parentStorageId")
    @Mapping(target = "tagIds", ignore = true)
    @Mapping(target = "imageIds", source = "images", qualifiedByName = "mapImagesToIds")
    Storage toDto(StorageEntity entity);

    @Mapping(target = "room", qualifiedByName = "mapRoomToUuid")
    @Mapping(target = "parentStorage", qualifiedByName = "mapParentStorageToUuid")
    @Mapping(target = "images", qualifiedByName = "mapImagesToUuids")
    @Mapping(target = "tags", qualifiedByName = "mapTagsToUuids")
    ExportStorage toExportStorage(StorageEntity storageEntity);
}

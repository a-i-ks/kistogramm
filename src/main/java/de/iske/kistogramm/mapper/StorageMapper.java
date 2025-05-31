package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.model.StorageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface StorageMapper {

    @Named("mapImagesToIds")
    static List<Integer> mapImagesToIds(Set<ImageEntity> images) {
        if (images == null) return List.of();
        return images.stream().map(ImageEntity::getId).toList();
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
}

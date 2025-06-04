package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.dto.export.ExportRoom;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.model.RoomEntity;
import de.iske.kistogramm.model.StorageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Named("mapImageToId")
    static Integer mapImageToId(ImageEntity entity) {
        if (entity == null) return null;
        return entity.getId();
    }

    @Named("mapStoragesToUuids")
    static List<UUID> mapStoragesToUuids(Set<StorageEntity> storages) {
        if (storages == null) return List.of();
        return storages.stream().map(StorageEntity::getUuid).toList();
    }

    @Mapping(target = "imageId", source = "image", qualifiedByName = "mapImageToId")
    Room toDto(RoomEntity entity);

    @Mapping(target = "storages", ignore = true)
    @Mapping(target = "image", ignore = true)
    RoomEntity toEntity(Room dto);

    @Mapping(target = "image", source = "image.uuid")
    @Mapping(target = "storages", qualifiedByName = "mapStoragesToUuids")
    ExportRoom toExportRoom(RoomEntity roomEntity);
}

package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.model.ImageEntity;
import de.iske.kistogramm.model.RoomEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Named("mapImageToId")
    static Integer mapImageToId(ImageEntity entity) {
        if (entity == null) return null;
        return entity.getId();
    }

    @Mapping(target = "imageId", source = "image", qualifiedByName = "mapImageToId")
    Room toDto(RoomEntity entity);

    @Mapping(target = "storages", ignore = true)
    @Mapping(target = "image", ignore = true)
    RoomEntity toEntity(Room dto);
}

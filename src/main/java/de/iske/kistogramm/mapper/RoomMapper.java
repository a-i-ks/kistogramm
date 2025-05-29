package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.model.RoomEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    Room toDto(RoomEntity entity);

    @Mapping(target = "storages", ignore = true)
    RoomEntity toEntity(Room dto);
}

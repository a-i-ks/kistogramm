package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Room;
import de.iske.kistogramm.model.RoomEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    Room toDto(RoomEntity entity);

    RoomEntity toEntity(Room dto);
}

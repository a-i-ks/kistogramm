package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.model.StorageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StorageMapper {

    @Mapping(target = "room", ignore = true)
    @Mapping(target = "parentStorage", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "subStorages", ignore = true)
    StorageEntity toEntity(Storage dto);

    @Mapping(source = "room.id", target = "roomId")
    @Mapping(source = "parentStorage.id", target = "parentStorageId")
    @Mapping(target = "tagIds", ignore = true)
        // wird im Service ergänzt
    Storage toDto(StorageEntity entity);
}

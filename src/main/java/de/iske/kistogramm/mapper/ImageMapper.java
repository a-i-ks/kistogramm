package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Image;
import de.iske.kistogramm.dto.export.ExportImage;
import de.iske.kistogramm.model.ImageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    Image toDto(ImageEntity entity);

    @Mapping(target = "item", ignore = true)
    @Mapping(target = "storage", ignore = true)
    @Mapping(target = "room", ignore = true)
    ImageEntity toEntity(Image dto);

    ExportImage toExportImage(ImageEntity imageEntity);
}

package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Image;
import de.iske.kistogramm.model.ImageEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    Image toDto(ImageEntity entity);

    ImageEntity toEntity(Image dto);
}

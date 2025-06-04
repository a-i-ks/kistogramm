package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Tag;
import de.iske.kistogramm.dto.export.ExportTag;
import de.iske.kistogramm.model.TagEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
    Tag toDto(TagEntity entity);

    TagEntity toEntity(Tag dto);

    ExportTag toExportTag(TagEntity tagEntity);
}

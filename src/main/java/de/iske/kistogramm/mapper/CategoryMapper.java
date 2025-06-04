package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.Category;
import de.iske.kistogramm.dto.export.ExportCategory;
import de.iske.kistogramm.model.CategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    Category toDto(CategoryEntity entity);

    CategoryEntity toEntity(Category dto);

    ExportCategory toExportCategory(CategoryEntity categoryEntity);
}

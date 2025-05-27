package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.CategoryAttributeTemplate;
import de.iske.kistogramm.model.CategoryAttributeTemplateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryAttributeTemplateMapper {

    @Mapping(source = "category.id", target = "categoryId")
    CategoryAttributeTemplate toDto(CategoryAttributeTemplateEntity entity);

    @Mapping(target = "category", ignore = true)
        // Service setzt Referenz
    CategoryAttributeTemplateEntity toEntity(CategoryAttributeTemplate dto);
}

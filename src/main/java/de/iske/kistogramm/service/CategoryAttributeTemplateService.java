package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.CategoryAttributeTemplate;
import de.iske.kistogramm.mapper.CategoryAttributeTemplateMapper;
import de.iske.kistogramm.model.CategoryAttributeTemplateEntity;
import de.iske.kistogramm.repository.CategoryAttributeTemplateRepository;
import de.iske.kistogramm.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CategoryAttributeTemplateService {

    private final CategoryAttributeTemplateRepository repository;
    private final CategoryRepository categoryRepository;
    private final CategoryAttributeTemplateMapper mapper;

    public CategoryAttributeTemplateService(CategoryAttributeTemplateRepository repository,
                                            CategoryRepository categoryRepository,
                                            CategoryAttributeTemplateMapper mapper) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    public List<CategoryAttributeTemplate> getTemplatesForCategory(Integer categoryId) {
        return repository.findByCategoryId(categoryId).stream()
                .map(mapper::toDto)
                .toList();
    }

    public CategoryAttributeTemplate createTemplate(CategoryAttributeTemplate dto) {
        CategoryAttributeTemplateEntity entity = mapper.toEntity(dto);
        entity.setCategory(categoryRepository.findById(dto.getCategoryId()).orElseThrow());
        entity.setDateAdded(LocalDate.now());
        entity.setDateModified(LocalDate.now());
        return mapper.toDto(repository.save(entity));
    }

    public void deleteTemplate(Integer id) {
        repository.deleteById(id);
    }
}

package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Category;
import de.iske.kistogramm.dto.CategoryAttributeTemplate;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.mapper.CategoryAttributeTemplateMapper;
import de.iske.kistogramm.mapper.CategoryMapper;
import de.iske.kistogramm.mapper.ItemMapper;
import de.iske.kistogramm.model.CategoryAttributeTemplateEntity;
import de.iske.kistogramm.model.CategoryEntity;
import de.iske.kistogramm.repository.CategoryAttributeTemplateRepository;
import de.iske.kistogramm.repository.CategoryRepository;
import de.iske.kistogramm.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final CategoryMapper categoryMapper;
    private final CategoryAttributeTemplateRepository templateRepository;
    private final ItemMapper itemMapper;
    private final CategoryAttributeTemplateMapper templateMapper;

    public CategoryService(
            CategoryRepository categoryRepository,
            CategoryAttributeTemplateRepository templateRepository,
            ItemRepository itemRepository,
            CategoryMapper categoryMapper,
            CategoryAttributeTemplateMapper templateMapper,
            ItemMapper itemMapper) {
        this.categoryRepository = categoryRepository;
        this.templateRepository = templateRepository;
        this.itemRepository = itemRepository;
        this.categoryMapper = categoryMapper;
        this.templateMapper = templateMapper;
        this.itemMapper = itemMapper;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    public Optional<Category> getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toDto);
    }

    public Category createCategory(Category category) {
        CategoryEntity entity = categoryMapper.toEntity(category);
        entity.setDateAdded(LocalDateTime.now());
        entity.setDateModified(LocalDateTime.now());
        return categoryMapper.toDto(categoryRepository.save(entity));
    }

    public void deleteCategory(Integer id) {
        categoryRepository.deleteById(id);
    }

    public List<Item> getItemsByCategoryId(Integer catId) {
        return itemRepository.findByCategoryId(catId)
                .stream().map(itemMapper::toDto).toList();
    }

    public List<CategoryAttributeTemplate> getTemplatesForCategory(Integer categoryId) {
        return templateRepository.findByCategoryId(categoryId).stream()
                .map(templateMapper::toDto)
                .toList();
    }

    public CategoryAttributeTemplate createTemplate(CategoryAttributeTemplate dto) {
        CategoryAttributeTemplateEntity entity = templateMapper.toEntity(dto);
        entity.setCategory(categoryRepository.findById(dto.getCategoryId()).orElseThrow());
        entity.setDateAdded(LocalDateTime.now());
        entity.setDateModified(LocalDateTime.now());
        return templateMapper.toDto(templateRepository.save(entity));
    }

    public void deleteTemplate(Integer id) {
        templateRepository.deleteById(id);
    }
}

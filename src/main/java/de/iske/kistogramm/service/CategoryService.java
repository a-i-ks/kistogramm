package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Category;
import de.iske.kistogramm.mapper.CategoryMapper;
import de.iske.kistogramm.model.CategoryEntity;
import de.iske.kistogramm.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
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
}

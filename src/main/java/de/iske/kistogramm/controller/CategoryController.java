package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.Category;
import de.iske.kistogramm.dto.CategoryAttributeTemplate;
import de.iske.kistogramm.dto.Item;
import de.iske.kistogramm.service.CategoryAttributeTemplateService;
import de.iske.kistogramm.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryAttributeTemplateService attributeTemplateService;


    public CategoryController(CategoryService categoryService,
                              CategoryAttributeTemplateService categoryAttributeTemplateService) {
        this.categoryService = categoryService;
        this.attributeTemplateService = categoryAttributeTemplateService;
    }

    @GetMapping("/template/category/{categoryId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CategoryAttributeTemplate>> getTemplateByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(attributeTemplateService.getTemplatesForCategory(categoryId));
    }

    @PostMapping("/template")
    @Transactional
    public ResponseEntity<CategoryAttributeTemplate> createTemplate(@RequestBody CategoryAttributeTemplate template) {
        return ResponseEntity.ok(attributeTemplateService.createTemplate(template));
    }

    @DeleteMapping("/template/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        attributeTemplateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Category> getCategoryById(@PathVariable Integer id) {
        return categoryService.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/items")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Item>> getItemsByCategoryId(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getItemsByCategoryId(id));
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryService.createCategory(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}

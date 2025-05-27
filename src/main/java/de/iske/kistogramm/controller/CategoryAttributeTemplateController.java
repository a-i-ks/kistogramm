package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.CategoryAttributeTemplate;
import de.iske.kistogramm.service.CategoryAttributeTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category-templates")
public class CategoryAttributeTemplateController {

    private final CategoryAttributeTemplateService service;

    public CategoryAttributeTemplateController(CategoryAttributeTemplateService service) {
        this.service = service;
    }

    @GetMapping("/category/{categoryId}")
    public List<CategoryAttributeTemplate> getByCategory(@PathVariable Integer categoryId) {
        return service.getTemplatesForCategory(categoryId);
    }

    @PostMapping
    public ResponseEntity<CategoryAttributeTemplate> create(@RequestBody CategoryAttributeTemplate template) {
        return ResponseEntity.ok(service.createTemplate(template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}

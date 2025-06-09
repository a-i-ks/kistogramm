package de.iske.kistogramm.config;

import de.iske.kistogramm.model.CategoryAttributeTemplateEntity;
import de.iske.kistogramm.model.CategoryEntity;
import de.iske.kistogramm.repository.CategoryAttributeTemplateRepository;
import de.iske.kistogramm.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class CategoryInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryInitializer.class);
    private final CategoryRepository categoryRepository;
    private final CategoryAttributeTemplateRepository categoryAttributeTemplateRepository;
    private final DefaultCategoryConfig config;

    public CategoryInitializer(CategoryRepository categoryRepository,
                               CategoryAttributeTemplateRepository categoryAttributeTemplateRepository,
                               DefaultCategoryConfig config) {
        this.categoryRepository = categoryRepository;
        this.categoryAttributeTemplateRepository = categoryAttributeTemplateRepository;
        this.config = config;
    }

    @PostConstruct
    public void initializeDefaults() {
        List<DefaultCategoryConfig.CategoryInit> defaults = config.getDefaultCategories();
        for (var defaultCategoryToAdd : defaults) {
            Optional<CategoryEntity> existing = categoryRepository.findByName(defaultCategoryToAdd.getName());
            CategoryEntity category;

            if (existing.isPresent()) {
                category = existing.get();
            } else {
                category = new CategoryEntity();
                category.setName(defaultCategoryToAdd.getName());
                category.setDateAdded(LocalDateTime.now());
                category.setDateModified(LocalDateTime.now());
                category = categoryRepository.save(category);
                LOG.info("Added new default category: {}", category.getName());
            }

            if (defaultCategoryToAdd.getAttributes() != null) {
                for (String attr : defaultCategoryToAdd.getAttributes()) {
                    boolean exists = categoryAttributeTemplateRepository.existsByCategoryIdAndAttributeName(category.getId(), attr);
                    if (!exists) {
                        CategoryAttributeTemplateEntity template = new CategoryAttributeTemplateEntity();
                        template.setCategory(category);
                        template.setAttributeName(attr);
                        template.setDateAdded(LocalDateTime.now());
                        template.setDateModified(LocalDateTime.now());
                        categoryAttributeTemplateRepository.save(template);
                        LOG.info("Added new default attribute for category \"{}\": {}", category.getName(), template.getAttributeName());
                    }
                }
            }
        }
    }
}

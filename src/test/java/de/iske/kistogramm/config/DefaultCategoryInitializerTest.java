package de.iske.kistogramm.config;

import de.iske.kistogramm.model.CategoryAttributeTemplateEntity;
import de.iske.kistogramm.model.CategoryEntity;
import de.iske.kistogramm.repository.CategoryAttributeTemplateRepository;
import de.iske.kistogramm.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
class DefaultCategoryInitializerTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryAttributeTemplateRepository categoryAttributeTemplateRepository;

    @Test
    void shouldCreateDefaultCategoriesWithAttributes() {
        // Beispiel: Existenz von "Kleidung"
        Optional<CategoryEntity> clothingOpt = categoryRepository.findByName("Kleidung");
        assertThat(clothingOpt).isPresent();

        CategoryEntity clothing = clothingOpt.get();
        List<CategoryAttributeTemplateEntity> clothingAttrs = categoryAttributeTemplateRepository.findByCategoryId(clothing.getId());

        // Attribute prüfen
        assertThat(clothingAttrs)
                .extracting(CategoryAttributeTemplateEntity::getAttributeName)
                .contains("Größe", "Zuletzt getragen");

        // Prüfen auf weitere Standardkategorien
        assertThat(categoryRepository.findByName("Elektronik")).isPresent();
        assertThat(categoryRepository.findByName("Lebensmittel")).isPresent();
        assertThat(categoryRepository.findByName("Möbelstück")).isPresent();
        assertThat(categoryRepository.findByName("Pflanze")).isPresent();
    }

    @Test
    void shouldNotCreateDuplicateCategories() {
        // Testet, dass keine doppelten Kategorien erstellt werden
        List<CategoryEntity> allCategories = categoryRepository.findAll();
        long uniqueCategoryCount = allCategories.stream()
                .map(CategoryEntity::getName)
                .distinct()
                .count();

        assertThat(uniqueCategoryCount).isEqualTo(allCategories.size());
    }
}

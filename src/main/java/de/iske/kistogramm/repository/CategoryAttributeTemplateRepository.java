package de.iske.kistogramm.repository;

import de.iske.kistogramm.model.CategoryAttributeTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryAttributeTemplateRepository extends JpaRepository<CategoryAttributeTemplateEntity, Integer> {
    List<CategoryAttributeTemplateEntity> findByCategoryId(Integer categoryId);

    boolean existsByCategoryIdAndAttributeName(Integer categoryId, String attributeName);
}

package de.iske.kistogramm.repository;

import de.iske.kistogramm.model.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Integer> {

    Optional<CategoryEntity> findByName(String name);

    Optional<CategoryEntity> findByUuid(UUID uuid);

}

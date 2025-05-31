package de.iske.kistogramm.repository;

import de.iske.kistogramm.model.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, Integer> {

    List<ItemEntity> findByCategoryId(Integer categoryId);

    List<ItemEntity> findByTagsId(Integer tagId);

    Optional<ItemEntity> findByUuid(UUID uuid);
}

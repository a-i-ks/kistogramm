package de.iske.kistogramm.repository;

import de.iske.kistogramm.model.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, Integer> {

    List<ItemEntity> findByCategoryId(Integer categoryId);


}

package de.iske.kistogramm.repository;

import de.iske.kistogramm.model.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Integer> {

    Optional<ImageEntity> findByUuid(UUID uuid);

}

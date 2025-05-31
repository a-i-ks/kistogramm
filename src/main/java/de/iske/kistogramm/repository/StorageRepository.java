package de.iske.kistogramm.repository;

import de.iske.kistogramm.model.StorageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorageRepository extends JpaRepository<StorageEntity, Integer> {

    Optional<StorageEntity> findByUuid(UUID uuid);

}

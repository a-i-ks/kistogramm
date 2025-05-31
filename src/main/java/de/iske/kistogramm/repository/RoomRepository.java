package de.iske.kistogramm.repository;

import de.iske.kistogramm.model.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Integer> {

    Optional<RoomEntity> findByUuid(UUID uuid);

}

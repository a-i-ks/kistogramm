package de.iske.kistogramm.repository;

import de.iske.kistogramm.model.AiJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJobEntity, UUID> {
}

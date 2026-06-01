package de.iske.kistogramm.repository;

import de.iske.kistogramm.model.AppSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingsRepository extends JpaRepository<AppSettingsEntity, Integer> {
}

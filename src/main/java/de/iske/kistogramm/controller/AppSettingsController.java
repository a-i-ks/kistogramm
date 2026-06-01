package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.AppSettingsDto;
import de.iske.kistogramm.service.AppSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class AppSettingsController {

    private final AppSettingsService settingsService;

    public AppSettingsController(AppSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<AppSettingsDto> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<AppSettingsDto> updateSettings(@Valid @RequestBody AppSettingsDto dto) {
        return ResponseEntity.ok(settingsService.updateSettings(dto));
    }
}

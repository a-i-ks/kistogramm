package de.iske.kistogramm.controller;

import de.iske.kistogramm.dto.ImportResult;
import de.iske.kistogramm.service.ImportService;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/import")
public class ImportController {

  private final ImportService importService;

  public ImportController(ImportService importService) {
    this.importService = importService;
  }

  @PostMapping
  @Transactional
  public ResponseEntity<ImportResult> importData(
          @RequestParam(name = "overwrite", required = false, defaultValue = "false") boolean overwrite,
          @RequestParam(name = "failOnError", required = false, defaultValue = "true") boolean failOnError,
          @RequestParam("file") MultipartFile file) throws IOException {


    return ResponseEntity.ok(importService.importArchive(file, overwrite, failOnError));
  }
}

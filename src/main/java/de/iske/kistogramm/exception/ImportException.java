package de.iske.kistogramm.exception;

import de.iske.kistogramm.dto.ImportResult;

import java.io.IOException;

public class ImportException extends IOException {

  private final ImportResult importResult;

  public ImportException(ImportResult importResult) {
    super("Import failed with errors: " + importResult.getErrors());
    this.importResult = importResult;
  }

  public ImportResult getImportResult() {
    return importResult;
  }
}

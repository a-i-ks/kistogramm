package de.iske.kistogramm.dto;

import java.util.List;

public class ImportResult {

  private boolean success;
  private boolean overwriteMode;

  private int importedTotalCount;
  private int updatedTotalCount;
  private int skippedTotalCount;
  private int failedTotalCount;

  private int importedItemCount;
  private int updatedItemCount;
  private int importedImageCount;
  private int importedCategoryCount;
  private int updatedCategoryCount;
  private int importedStorageCount;
  private int updatedStorageCount;
  private int importedRoomCount;
  private int updatedRoomCount;
  private int importedTagCount;
  private int updatedTagCount;

  private List<String> errors;
  private List<String> warnings;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public boolean isOverwriteMode() {
    return overwriteMode;
  }

  public void setOverwriteMode(boolean overwriteMode) {
    this.overwriteMode = overwriteMode;
  }

  public int getImportedTotalCount() {
    return importedTotalCount;
  }

  public void setImportedTotalCount(int importedTotalCount) {
    this.importedTotalCount = importedTotalCount;
  }

  public int getUpdatedTotalCount() {
    return updatedTotalCount;
  }

  public void setUpdatedTotalCount(int updatedTotalCount) {
    this.updatedTotalCount = updatedTotalCount;
  }

  public int getSkippedTotalCount() {
    return skippedTotalCount;
  }

  public void setSkippedTotalCount(int skippedTotalCount) {
    this.skippedTotalCount = skippedTotalCount;
  }

  public int getFailedTotalCount() {
    return failedTotalCount;
  }

  public void setFailedTotalCount(int failedTotalCount) {
    this.failedTotalCount = failedTotalCount;
  }

  public int getImportedItemCount() {
    return importedItemCount;
  }

  public void setImportedItemCount(int importedItemCount) {
    this.importedItemCount = importedItemCount;
  }

  public int getUpdatedItemCount() {
    return updatedItemCount;
  }

  public void setUpdatedItemCount(int updatedItemCount) {
    this.updatedItemCount = updatedItemCount;
  }

  public int getImportedImageCount() {
    return importedImageCount;
  }

  public void setImportedImageCount(int importedImageCount) {
    this.importedImageCount = importedImageCount;
  }

  public int getImportedCategoryCount() {
    return importedCategoryCount;
  }

  public void setImportedCategoryCount(int importedCategoryCount) {
    this.importedCategoryCount = importedCategoryCount;
  }

  public int getUpdatedCategoryCount() {
    return updatedCategoryCount;
  }

  public void setUpdatedCategoryCount(int updatedCategoryCount) {
    this.updatedCategoryCount = updatedCategoryCount;
  }

  public int getImportedStorageCount() {
    return importedStorageCount;
  }

  public void setImportedStorageCount(int importedStorageCount) {
    this.importedStorageCount = importedStorageCount;
  }

  public int getUpdatedStorageCount() {
    return updatedStorageCount;
  }

  public void setUpdatedStorageCount(int updatedStorageCount) {
    this.updatedStorageCount = updatedStorageCount;
  }

  public int getImportedRoomCount() {
    return importedRoomCount;
  }

  public void setImportedRoomCount(int importedRoomCount) {
    this.importedRoomCount = importedRoomCount;
  }

  public int getUpdatedRoomCount() {
    return updatedRoomCount;
  }

  public void setUpdatedRoomCount(int updatedRoomCount) {
    this.updatedRoomCount = updatedRoomCount;
  }

  public int getImportedTagCount() {
    return importedTagCount;
  }

  public void setImportedTagCount(int importedTagCount) {
    this.importedTagCount = importedTagCount;
  }

  public int getUpdatedTagCount() {
    return updatedTagCount;
  }

  public void setUpdatedTagCount(int updatedTagCount) {
    this.updatedTagCount = updatedTagCount;
  }

  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }
}

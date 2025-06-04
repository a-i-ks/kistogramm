package de.iske.kistogramm.mapper;

import de.iske.kistogramm.dto.export.ExportItem;
import de.iske.kistogramm.model.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ItemMapperTest {

    private final ItemMapper mapper = Mappers.getMapper(ItemMapper.class);

    @Test
    void shouldMapItemEntityToItemExport() {
        // Arrange
        UUID itemUuid = UUID.randomUUID();
        UUID categoryUuid = UUID.randomUUID();
        UUID storageUuid = UUID.randomUUID();
        UUID tag1Uuid = UUID.randomUUID();
        UUID tag2Uuid = UUID.randomUUID();
        UUID relatedItem1Uuid = UUID.randomUUID();
        UUID relatedItem2Uuid = UUID.randomUUID();

        UUID imageUuid = UUID.randomUUID();

        CategoryEntity category = new CategoryEntity();
        category.setUuid(categoryUuid);

        StorageEntity storage = new StorageEntity();
        storage.setUuid(storageUuid);

        TagEntity tag1 = new TagEntity();
        tag1.setUuid(tag1Uuid);
        TagEntity tag2 = new TagEntity();
        tag2.setUuid(tag2Uuid);

        ItemEntity relatedItem1 = new ItemEntity();
        relatedItem1.setUuid(relatedItem1Uuid);

        ItemEntity relatedItem2 = new ItemEntity();
        relatedItem2.setUuid(relatedItem2Uuid);

        ImageEntity image = new ImageEntity();
        image.setUuid(imageUuid);

        Map<String, String> customAttrs = new HashMap<>();
        customAttrs.put("Farbe", "Blau");
        customAttrs.put("Zustand", "Neu");

        ItemEntity item = new ItemEntity();
        item.setUuid(itemUuid);
        item.setName("Test Item");
        item.setDescription("Beschreibung");
        item.setPurchaseDate(LocalDate.of(2023, 5, 20));
        item.setPurchasePrice(49.99);
        item.setQuantity(3);
        item.setDateAdded(LocalDateTime.of(2023, 5, 21, 10, 0));
        item.setDateModified(LocalDateTime.of(2023, 5, 22, 12, 0));
        item.setCategory(category);
        item.setStorage(storage);
        item.setTags(Set.of(tag1, tag2));
        item.setRelatedItems(Set.of(relatedItem1, relatedItem2));
        item.setImages(Set.of(image));
        item.setCustomAttributes(customAttrs);

        // Act
        ExportItem export = mapper.toExportItem(item);

        // Assert
        assertThat(export.getName()).isEqualTo("Test Item");
        assertThat(export.getDescription()).isEqualTo("Beschreibung");
        assertThat(export.getPurchaseDate()).isEqualTo(LocalDate.of(2023, 5, 20));
        assertThat(export.getPurchasePrice()).isEqualTo(49.99);
        assertThat(export.getQuantity()).isEqualTo(3);
        assertThat(export.getDateAdded()).isEqualTo(LocalDateTime.of(2023, 5, 21, 10, 0));
        assertThat(export.getDateModified()).isEqualTo(LocalDateTime.of(2023, 5, 22, 12, 0));

        assertThat(export.getCategory()).isEqualTo(categoryUuid);
        assertThat(export.getStorage()).isEqualTo(storageUuid);
        assertThat(export.getTags()).containsExactlyInAnyOrder(tag1Uuid, tag2Uuid);
        assertThat(export.getRelatedItems()).containsExactlyInAnyOrder(relatedItem1Uuid, relatedItem2Uuid);
        assertThat(export.getImages()).containsExactly(imageUuid);
        assertThat(export.getCustomAttributes()).containsEntry("Farbe", "Blau").containsEntry("Zustand", "Neu");
    }
}

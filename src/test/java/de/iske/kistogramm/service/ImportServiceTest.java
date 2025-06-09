package de.iske.kistogramm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.iske.kistogramm.dto.export.ExportResult;
import de.iske.kistogramm.dto.export.ExportRoom;
import de.iske.kistogramm.dto.export.ExportStorage;
import de.iske.kistogramm.model.StorageEntity;
import de.iske.kistogramm.repository.StorageRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class ImportServiceTest {

    @Autowired
    private ImportService importService;

    @Autowired
    private StorageRepository storageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldImportStoragesWithChildrenBeforeParents() throws Exception {
        // Prepare export data with child storage appearing before parent
        UUID roomUuid = UUID.randomUUID();

        ExportRoom room = new ExportRoom();
        room.setUuid(roomUuid);
        room.setName("Room");
        room.setDateAdded(LocalDateTime.now());
        room.setDateModified(LocalDateTime.now());

        UUID parentUuid = UUID.randomUUID();
        UUID childUuid = UUID.randomUUID();

        ExportStorage child = new ExportStorage();
        child.setUuid(childUuid);
        child.setName("Child");
        child.setRoom(roomUuid);
        child.setParentStorage(parentUuid);
        child.setDateAdded(LocalDateTime.now());
        child.setDateModified(LocalDateTime.now());

        ExportStorage parent = new ExportStorage();
        parent.setUuid(parentUuid);
        parent.setName("Parent");
        parent.setRoom(roomUuid);
        parent.setDateAdded(LocalDateTime.now());
        parent.setDateModified(LocalDateTime.now());

        ExportResult exportResult = new ExportResult();
        exportResult.setRooms(List.of(room));
        // child first
        exportResult.setStorages(List.of(child, parent));

        byte[] json = objectMapper.writeValueAsBytes(exportResult);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("data.json"));
            zos.write(json);
            zos.closeEntry();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "import.zip",
                "application/zip",
                baos.toByteArray()
        );

        importService.importArchive(file, false, true);

        StorageEntity parentEntity = storageRepository.findByUuid(parentUuid).orElseThrow();
        StorageEntity childEntity = storageRepository.findByUuid(childUuid).orElseThrow();

        assertThat(childEntity.getParentStorage()).isEqualTo(parentEntity);
    }
}

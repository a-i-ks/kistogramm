package de.iske.kistogramm.service;

import de.iske.kistogramm.dto.Storage;
import de.iske.kistogramm.model.RoomEntity;
import de.iske.kistogramm.repository.RoomRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class StorageServiceTest {

    @Autowired
    private StorageService storageService;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void testCreateAndFindStorage() {
        RoomEntity room = new RoomEntity();
        room.setName("Test Room");
        room.setDateAdded(LocalDate.now());
        room.setDateModified(LocalDate.now());
        room = roomRepository.save(room);

        Storage dto = new Storage();
        dto.setName("Box A");
        dto.setDescription("Test Box");
        dto.setRoomId(room.getId());

        Storage created = storageService.create(dto);
        assertNotNull(created.getId());

        Optional<Storage> loaded = storageService.getById(created.getId());
        assertTrue(loaded.isPresent());
        assertEquals("Box A", loaded.get().getName());
    }
}

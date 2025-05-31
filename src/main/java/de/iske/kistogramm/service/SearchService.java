package de.iske.kistogramm.service;

import de.iske.kistogramm.mapper.*;
import de.iske.kistogramm.repository.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class SearchService {

    private final ItemRepository itemRepository;
    private final StorageRepository storageRepository;
    private final RoomRepository roomRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;

    private final ItemMapper itemMapper;
    private final StorageMapper storageMapper;
    private final RoomMapper roomMapper;
    private final TagMapper tagMapper;
    private final CategoryMapper categoryMapper;

    public SearchService(ItemRepository itemRepository,
                         StorageRepository storageRepository,
                         RoomRepository roomRepository,
                         TagRepository tagRepository,
                         CategoryRepository categoryRepository,
                         ItemMapper itemMapper,
                         StorageMapper storageMapper,
                         RoomMapper roomMapper,
                         TagMapper tagMapper,
                         CategoryMapper categoryMapper) {
        this.itemRepository = itemRepository;
        this.storageRepository = storageRepository;
        this.roomRepository = roomRepository;
        this.tagRepository = tagRepository;
        this.categoryRepository = categoryRepository;
        this.itemMapper = itemMapper;
        this.storageMapper = storageMapper;
        this.roomMapper = roomMapper;
        this.tagMapper = tagMapper;
        this.categoryMapper = categoryMapper;
    }

    public Optional<Map<String, Object>> search(UUID uuid, String type) {
        if (type != null) {
            return switch (type.toLowerCase()) {
                case "item" -> itemRepository.findByUuid(uuid)
                        .map(i -> wrap("Item", itemMapper.toDto(i)));
                case "storage" -> storageRepository.findByUuid(uuid)
                        .map(s -> wrap("Storage", storageMapper.toDto(s)));
                case "room" -> roomRepository.findByUuid(uuid)
                        .map(r -> wrap("Room", roomMapper.toDto(r)));
                case "tag" -> tagRepository.findByUuid(uuid)
                        .map(t -> wrap("Tag", tagMapper.toDto(t)));
                case "category" -> categoryRepository.findByUuid(uuid)
                        .map(c -> wrap("Category", categoryMapper.toDto(c)));
                default -> Optional.empty();
            };
        }

        return Stream.of(
                itemRepository.findByUuid(uuid).map(i -> wrap("Item", itemMapper.toDto(i))),
                storageRepository.findByUuid(uuid).map(s -> wrap("Storage", storageMapper.toDto(s))),
                roomRepository.findByUuid(uuid).map(r -> wrap("Room", roomMapper.toDto(r))),
                tagRepository.findByUuid(uuid).map(t -> wrap("Tag", tagMapper.toDto(t))),
                categoryRepository.findByUuid(uuid).map(c -> wrap("Category", categoryMapper.toDto(c)))
        ).flatMap(Optional::stream).findFirst();
    }

    private Map<String, Object> wrap(String type, Object payload) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("payload", payload);
        return result;
    }
}

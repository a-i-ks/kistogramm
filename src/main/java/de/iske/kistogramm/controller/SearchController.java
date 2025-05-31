package de.iske.kistogramm.controller;

import de.iske.kistogramm.service.SearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<?> searchByUuid(
            @PathVariable UUID uuid,
            @RequestParam(value = "type", required = false) String type) {

        return searchService.search(uuid, type)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Entity not found")));
    }

}
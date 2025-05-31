package de.iske.kistogramm.controller;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    // Endpoint to export data in json format
    @GetMapping("/json")
    @Transactional(readOnly = true)
    public String exportJson() {
        // Logic to export data in JSON format
        return "Exported data in JSON format";
    }

}

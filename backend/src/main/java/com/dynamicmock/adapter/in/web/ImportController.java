package com.dynamicmock.adapter.in.web;

import com.dynamicmock.application.service.PostmanImportService;
import com.dynamicmock.application.service.PostmanImportService.ImportReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ADAPTER LAYER - Input Controller
 * REST endpoint for importing Postman Collection v2.1 exports.
 */
@Slf4j
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final PostmanImportService postmanImportService;

    /**
     * Import a Postman collection (and optional environment files).
     *
     * @param file   the Postman collection v2.1 JSON export (required)
     * @param files  optional Postman environment JSON exports (multiple allowed)
     * @return an import report with counts, warnings and errors
     */
    @PostMapping("/postman")
    public ResponseEntity<ImportReport> importPostman(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] collectionJson = file.getBytes();
            List<byte[]> environmentJsons = new ArrayList<>();
            if (files != null) {
                for (MultipartFile envFile : files) {
                    if (!envFile.isEmpty()) {
                        environmentJsons.add(envFile.getBytes());
                    }
                }
            }

            ImportReport report = postmanImportService.importPostmanCollection(collectionJson, environmentJsons);
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            log.error("Failed to read uploaded Postman files", e);
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException e) {
            // Malformed collection JSON - surface as 400 with the report-shaped error
            log.warn("Rejected Postman import: {}", e.getMessage());
            ImportReport report = new ImportReport(0, 0, 0, List.of(), List.of(e.getMessage()));
            return ResponseEntity.badRequest().body(report);
        }
    }
}

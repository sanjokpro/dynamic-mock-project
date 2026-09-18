package com.dynamicmock.controller;

import com.dynamicmock.adapter.in.web.ImportController;
import com.dynamicmock.application.service.PostmanImportService;
import com.dynamicmock.application.service.PostmanImportService.ImportReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ImportControllerTest {

    @Mock
    private PostmanImportService postmanImportService;

    @InjectMocks
    private ImportController importController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(importController).build();
    }

    private ImportReport sampleReport() {
        return new ImportReport(1, 2, 4,
                List.of("Request 'X': scripts skipped"), List.of());
    }

    @Test
    void importPostman_shouldReturnReport() throws Exception {
        MockMultipartFile collection = new MockMultipartFile(
                "file", "collection.json", "application/json",
                "{\"info\":{\"name\":\"Test\"},\"item\":[]}".getBytes());

        when(postmanImportService.importPostmanCollection(any(byte[].class), any()))
                .thenReturn(sampleReport());

        mockMvc.perform(multipart("/api/import/postman").file(collection))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environments").value(1))
                .andExpect(jsonPath("$.collections").value(2))
                .andExpect(jsonPath("$.routes").value(4))
                .andExpect(jsonPath("$.warnings.length()").value(1))
                .andExpect(jsonPath("$.errors.length()").value(0));

        verify(postmanImportService).importPostmanCollection(any(byte[].class), any());
    }

    @Test
    void importPostman_shouldPassEnvironmentFiles() throws Exception {
        MockMultipartFile collection = new MockMultipartFile(
                "file", "collection.json", "application/json",
                "{\"info\":{\"name\":\"Test\"},\"item\":[]}".getBytes());
        MockMultipartFile env = new MockMultipartFile(
                "files", "uat.json", "application/json",
                "{\"name\":\"UAT\",\"values\":[]}".getBytes());

        when(postmanImportService.importPostmanCollection(any(byte[].class), any()))
                .thenReturn(sampleReport());

        mockMvc.perform(multipart("/api/import/postman").file(collection).file(env))
                .andExpect(status().isOk());
    }

    @Test
    void importPostman_shouldReturn400WhenCollectionFileMissing() throws Exception {
        mockMvc.perform(multipart("/api/import/postman"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importPostman_shouldReturn400WithErrorsOnMalformedJson() throws Exception {
        MockMultipartFile collection = new MockMultipartFile(
                "file", "collection.json", "application/json",
                "{ not json ".getBytes());

        when(postmanImportService.importPostmanCollection(any(byte[].class), any()))
                .thenThrow(new IllegalArgumentException("Invalid Postman collection JSON: bad token"));

        mockMvc.perform(multipart("/api/import/postman").file(collection))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1));
    }
}

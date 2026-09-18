package com.dynamicmock.application.service;

import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.application.service.PostmanCollectionModel.Body;
import com.dynamicmock.application.service.PostmanCollectionModel.Header;
import com.dynamicmock.application.service.PostmanCollectionModel.Item;
import com.dynamicmock.application.service.PostmanCollectionModel.PostmanCollection;
import com.dynamicmock.application.service.PostmanCollectionModel.PostmanEnvironment;
import com.dynamicmock.application.service.PostmanCollectionModel.PostmanResponse;
import com.dynamicmock.application.service.PostmanCollectionModel.Request;
import com.dynamicmock.application.service.PostmanCollectionModel.Url;
import com.dynamicmock.domain.entity.Collection;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * APPLICATION LAYER - Importer for Postman Collection v2.1 exports.
 *
 * Converts a Postman collection (+ optional environment files) into
 * mock routes, workspace collections and environments:
 * - top-level folders become workspace collections (nested folders are flattened
 *   into the parent collection with "Parent/Child" item names)
 * - requests become mock routes (headers/query params become matchers; saved
 *   example responses become response templates)
 * - Postman scripts (prerequest/test) are intentionally NOT imported
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostmanImportService {

    private static final String IMPORT_USER_ID = "default-user";
    private static final String DEFAULT_RESPONSE_TEMPLATE = "{\"message\": \"Mocked from Postman collection\"}";

    private final RouteService routeService;
    private final WorkspaceCollectionService collectionService;
    private final WorkspaceEnvironmentService environmentService;
    private final ObjectMapper objectMapper;

    @Data
    public static class ImportReport {
        private int environments;
        private int collections;
        private int routes;
        private List<String> warnings = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public ImportReport() {
        }

        public ImportReport(int environments, int collections, int routes,
                            List<String> warnings, List<String> errors) {
            this.environments = environments;
            this.collections = collections;
            this.routes = routes;
            this.warnings = warnings;
            this.errors = errors;
        }
    }

    /**
     * Import a Postman collection and optional environment exports.
     *
     * @param collectionJson   raw bytes of the Postman collection v2.1 JSON
     * @param environmentJsons raw bytes of optional Postman environment JSON files
     * @return an {@link ImportReport} with counts, warnings and per-item errors
     */
    public ImportReport importPostmanCollection(byte[] collectionJson, List<byte[]> environmentJsons) {
        ImportReport report = new ImportReport();
        try {
            PostmanCollection collection = objectMapper.readValue(collectionJson, PostmanCollection.class);

            if (environmentJsons != null) {
                for (byte[] envJson : environmentJsons) {
                    importEnvironment(envJson, report);
                }
            }

            String collectionDisplayName = collection.getInfo() != null && collection.getInfo().getName() != null
                    ? collection.getInfo().getName()
                    : "Imported Collection";

            List<Item> items = collection.getItem() == null ? List.of() : collection.getItem();

            // Folder items at the top level each become a workspace collection.
            // Bare requests at the top level go into one collection named after the Postman collection.
            List<Item> bareRequests = new ArrayList<>();
            for (Item item : items) {
                if (isFolder(item)) {
                    importFolder(item, report);
                } else if (item.getRequest() != null) {
                    bareRequests.add(item);
                } else {
                    report.getWarnings().add("Skipped item with no folder or request: " + safeName(item));
                }
            }

            if (!bareRequests.isEmpty()) {
                Collection target = collectionService.createCollection(
                        collectionDisplayName, IMPORT_USER_ID, "Imported from Postman collection");
                report.setCollections(report.getCollections() + 1);
                for (Item request : bareRequests) {
                    importRequest(request, target.getId(), "", report);
                }
            }

            log.info("Postman import complete: {} environments, {} collections, {} routes, {} warnings, {} errors",
                    report.getEnvironments(), report.getCollections(), report.getRoutes(),
                    report.getWarnings().size(), report.getErrors().size());
            return report;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid Postman collection JSON: " + e.getMessage(), e);
        }
    }

    private void importEnvironment(byte[] envJson, ImportReport report) {
        try {
            PostmanEnvironment environment = objectMapper.readValue(envJson, PostmanEnvironment.class);
            Map<String, String> variables = new LinkedHashMap<>();
            if (environment.getValues() != null) {
                for (PostmanEnvironment.EnvironmentValue value : environment.getValues()) {
                    if (value.getKey() == null || value.getKey().isBlank()) {
                        continue;
                    }
                    if (Boolean.FALSE.equals(value.getEnabled())) {
                        continue; // skip disabled variables
                    }
                    variables.put(value.getKey(), value.getValue() == null ? "" : value.getValue());
                }
            }
            String name = environment.getName() == null || environment.getName().isBlank()
                    ? "Imported Environment" : environment.getName();
            environmentService.createEnvironment(name, variables);
            report.setEnvironments(report.getEnvironments() + 1);
        } catch (IOException e) {
            report.getErrors().add("Failed to parse environment file: " + e.getMessage());
        }
    }

    private void importFolder(Item folder, ImportReport report) {
        Collection created = collectionService.createCollection(
                safeName(folder), IMPORT_USER_ID, "Imported from Postman folder");
        report.setCollections(report.getCollections() + 1);

        List<Item> children = folder.getItem() == null ? List.of() : folder.getItem();
        for (Item child : children) {
            importChildItem(child, created.getId(), "", report);
        }
    }

    /**
     * Recursively import children of a folder. Nested folders are flattened:
     * their requests land in the same collection with "Parent/Child" item names.
     */
    private void importChildItem(Item item, String collectionId, String folderPrefix, ImportReport report) {
        if (isFolder(item)) {
            String newPrefix = folderPrefix.isEmpty() ? safeName(item) : folderPrefix + "/" + safeName(item);
            List<Item> children = item.getItem() == null ? List.of() : item.getItem();
            for (Item child : children) {
                importChildItem(child, collectionId, newPrefix, report);
            }
        } else if (item.getRequest() != null) {
            importRequest(item, collectionId, folderPrefix, report);
        } else {
            report.getWarnings().add("Skipped item with no folder or request: " + safeName(item));
        }
    }

    private void importRequest(Item item, String collectionId, String folderPrefix, ImportReport report) {
        String displayName = folderPrefix.isEmpty()
                ? safeName(item)
                : folderPrefix + "/" + safeName(item);

        try {
            Request request = item.getRequest();
            String method = request.getMethod() == null || request.getMethod().isBlank()
                    ? null : request.getMethod().toUpperCase();
            String path = extractPath(request.getUrl());

            if (method == null || path == null) {
                report.getWarnings().add("Skipped request '" + displayName + "': missing method or URL path");
                return;
            }

            if (item.getEvent() != null && !item.getEvent().isEmpty()
                    || request.getEvent() != null && !request.getEvent().isEmpty()) {
                report.getWarnings().add("Request '" + displayName + "': Postman scripts (prerequest/test) were skipped");
            }

            Body body = request.getBody();
            if (body != null && body.getMode() != null
                    && List.of("file", "formdata", "urlencoded", "graphql").contains(body.getMode())) {
                report.getWarnings().add("Request '" + displayName + "': body mode '" + body.getMode()
                        + "' was not converted (no body matcher created)");
            }

            Map<String, Object> matchers = buildMatchers(request);

            // Saved examples usually live on the item (Postman v2.1 standard),
            // but some exports (and older clients) nest them inside the request.
            List<PostmanResponse> examples = item.getResponse() != null && !item.getResponse().isEmpty()
                    ? item.getResponse()
                    : (request.getResponse() != null ? request.getResponse() : List.of());
            PostmanResponse example = findExample(examples);
            String responseTemplate;
            Integer responseStatus;
            Map<String, String> responseHeaders;
            if (example != null) {
                responseTemplate = example.getBody() != null ? example.getBody() : DEFAULT_RESPONSE_TEMPLATE;
                responseStatus = example.getCode();
                responseHeaders = headersToMap(example.getHeader());
            } else {
                responseTemplate = DEFAULT_RESPONSE_TEMPLATE;
                responseStatus = 200;
                responseHeaders = null;
            }

            CreateRouteRequest createRequest = CreateRouteRequest.builder()
                    .path(path)
                    .method(method)
                    .matchers(matchers.isEmpty() ? null : matchers)
                    .responseTemplate(responseTemplate)
                    .responseStatus(responseStatus)
                    .responseHeaders(responseHeaders)
                    .build();

            RouteResponse created = routeService.createRoute(createRequest);
            // createRoute saves routes inactive; register them in the live registry immediately
            routeService.activateRoute(created.getId());

            collectionService.addItemToCollection(collectionId, Collection.CollectionItem.builder()
                    .protocol("HTTP")
                    .resourceId(created.getId())
                    .name(displayName)
                    .build());

            report.setRoutes(report.getRoutes() + 1);
        } catch (Exception e) {
            log.warn("Failed to import Postman request '{}': {}", displayName, e.getMessage());
            report.getErrors().add("Request '" + displayName + "': " + e.getMessage());
        }
    }

    private boolean isFolder(Item item) {
        return item.getItem() != null && !item.getItem().isEmpty();
    }

    private String safeName(Item item) {
        return item.getName() == null || item.getName().isBlank() ? "Unnamed" : item.getName();
    }

    /**
     * Derive the mock path from a Postman URL: strip scheme+host, keep path and
     * query string, and convert Postman-style :param segments to {param}.
     */
    private String extractPath(Url url) {
        if (url == null) {
            return null;
        }
        String raw = url.getRaw();
        String candidate;
        if (raw == null || raw.isBlank()) {
            candidate = url.getPath() == null ? "" : "/" + String.join("/", url.getPath());
        } else {
            candidate = raw;
            int schemeIdx = candidate.indexOf("://");
            if (schemeIdx >= 0) {
                candidate = candidate.substring(schemeIdx + 3);
            }
            int slash = candidate.indexOf('/');
            if (slash < 0) {
                return null; // host only, no path
            }
            candidate = candidate.substring(slash);
        }
        if (candidate.isBlank() || "/".equals(candidate)) {
            return null;
        }
        return candidate.replaceAll("(?<=[/?]):([A-Za-z0-9_]+)", "{$1}");
    }

    /**
     * Convert Postman request headers and query params into route matchers.
     * Disabled entries and entries with blank values are skipped, because a
     * blank matcher value would never match a real request.
     */
    private Map<String, Object> buildMatchers(Request request) {
        Map<String, String> headerMatchers = new LinkedHashMap<>();
        if (request.getHeader() != null) {
            for (Header header : request.getHeader()) {
                if (header.getKey() == null || header.getKey().isBlank()) {
                    continue;
                }
                if (Boolean.FALSE.equals(header.getDisabled())) {
                    continue;
                }
                if (header.getValue() == null || header.getValue().isBlank()) {
                    continue;
                }
                headerMatchers.put(header.getKey(), header.getValue());
            }
        }

        Map<String, String> queryMatchers = new LinkedHashMap<>();
        Url url = request.getUrl();
        if (url != null && url.getQuery() != null) {
            for (PostmanCollectionModel.QueryParam param : url.getQuery()) {
                if (param.getKey() == null || param.getKey().isBlank()) {
                    continue;
                }
                if (Boolean.FALSE.equals(param.getDisabled())) {
                    continue;
                }
                if (param.getValue() == null || param.getValue().isBlank()) {
                    continue;
                }
                queryMatchers.put(param.getKey(), param.getValue());
            }
        }

        Map<String, Object> matchers = new LinkedHashMap<>();
        if (!headerMatchers.isEmpty()) {
            matchers.put("headers", headerMatchers);
        }
        if (!queryMatchers.isEmpty()) {
            matchers.put("queryParams", queryMatchers);
        }
        return matchers;
    }

    /**
     * Pick the saved example response to use as the mock template:
     * the first example with a 2xx status code and a body, else the first
     * example with a body, else null.
     */
    private PostmanResponse findExample(List<PostmanResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return null;
        }
        for (PostmanResponse response : responses) {
            if (response.getCode() != null && response.getCode() >= 200 && response.getCode() < 300
                    && response.getBody() != null && !response.getBody().isBlank()) {
                return response;
            }
        }
        for (PostmanResponse response : responses) {
            if (response.getBody() != null && !response.getBody().isBlank()) {
                return response;
            }
        }
        return null;
    }

    private Map<String, String> headersToMap(List<Header> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Header header : headers) {
            if (header.getKey() == null || header.getKey().isBlank()) {
                continue;
            }
            if (Boolean.FALSE.equals(header.getDisabled())) {
                continue;
            }
            result.put(header.getKey(), header.getValue() == null ? "" : header.getValue());
        }
        return result.isEmpty() ? null : result;
    }
}

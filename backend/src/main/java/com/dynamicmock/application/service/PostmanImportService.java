package com.dynamicmock.application.service;

import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.application.service.PostmanCollectionModel.*;
import com.dynamicmock.domain.entity.Collection;
import com.dynamicmock.domain.entity.Environment;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * APPLICATION LAYER - Importer for Postman Collection v2.1 exports.
 *
 * <p>Converts a Postman collection (+ optional environment files) into
 * mock routes, workspace collections and environments:
 * <ul>
 *   <li>Top-level folders each become a workspace collection (nested folders flattened with Parent/Child names).</li>
 *   <li>Each saved example in an item becomes a distinct conditional {@code MockRoute} with matchers extracted
 *       from {@code originalRequest}. Requests with zero saved examples become a single default route.</li>
 *   <li>Postman scripts (prerequest/test) are intentionally NOT imported (skipped with warning).</li>
 *   <li>Deterministic route IDs (UUIDv5) enable idempotent re-import with in-place updates.</li>
 *   <li>Postman {@code {{$timestamp}}} is translated to {@code {{$timestampSeconds}}} at import time.</li>
 *   <li>Collection variables are ingested into a {@code [CollectionName] Variables} environment.</li>
 * </ul>
 */
@Slf4j
@Service
public class PostmanImportService {

    private static final String DEFAULT_RESPONSE_TEMPLATE = "{\"message\": \"Mocked from Postman collection\"}";
    private static final UUID UUIDV5_NAMESPACE = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "transfer-encoding", "content-length", "connection", "keep-alive",
            "proxy-authenticate", "proxy-authorization", "te", "trailers",
            "trailer", "upgrade", "host"
    );

    private final RouteService routeService;
    private final WorkspaceCollectionService collectionService;
    private final WorkspaceEnvironmentService environmentService;
    private final ObjectMapper objectMapper;
    private final com.dynamicmock.domain.port.out.UserRepository userRepository;

    public PostmanImportService(RouteService routeService,
                                WorkspaceCollectionService collectionService,
                                WorkspaceEnvironmentService environmentService,
                                ObjectMapper objectMapper) {
        this(routeService, collectionService, environmentService, objectMapper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PostmanImportService(RouteService routeService,
                                WorkspaceCollectionService collectionService,
                                WorkspaceEnvironmentService environmentService,
                                ObjectMapper objectMapper,
                                com.dynamicmock.domain.port.out.UserRepository userRepository) {
        this.routeService = routeService;
        this.collectionService = collectionService;
        this.environmentService = environmentService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    private String resolveUserId(String explicitUserId) {
        if (explicitUserId != null && !explicitUserId.trim().isEmpty()) {
            return explicitUserId.trim();
        }
        if (userRepository != null) {
            return userRepository.findByUsername("admin")
                    .map(com.dynamicmock.domain.entity.User::getId)
                    .orElse("system");
        }
        return "system";
    }

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

    public ImportReport importPostmanCollection(byte[] collectionJson, List<byte[]> environmentJsons) {
        return importPostmanCollection(collectionJson, environmentJsons, null);
    }

    /**
     * Import a Postman collection and optional environment exports.
     *
     * @param collectionJson   raw bytes of the Postman collection v2.1 JSON
     * @param environmentJsons raw bytes of optional Postman environment JSON files
     * @param explicitUserId   optional user ID to own imported collections
     * @return an {@link ImportReport} with counts, warnings and per-item errors
     */
    public ImportReport importPostmanCollection(byte[] collectionJson, List<byte[]> environmentJsons, String explicitUserId) {
        ImportReport report = new ImportReport();
        String ownerId = resolveUserId(explicitUserId);
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

            // Ingest enabled collection variables into [CollectionName] Variables environment
            if (collection.getVariable() != null && !collection.getVariable().isEmpty()) {
                Map<String, String> collectionVars = new LinkedHashMap<>();
                for (PostmanCollectionModel.CollectionVariable var : collection.getVariable()) {
                    if (var.getKey() == null || var.getKey().isBlank()) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(var.getDisabled())) {
                        continue; // skip disabled variables
                    }
                    collectionVars.put(var.getKey(), var.getValue() != null ? var.getValue() : "");
                }
                if (!collectionVars.isEmpty()) {
                    String envName = collectionDisplayName + " Variables";
                    List<Environment> envs = environmentService.getAllEnvironments();
                    Environment existingEnv = envs != null
                            ? envs.stream().filter(e -> envName.equals(e.getName())).findFirst().orElse(null)
                            : null;
                    if (existingEnv != null) {
                        existingEnv.setVariables(collectionVars);
                        environmentService.updateEnvironment(existingEnv.getId(), existingEnv);
                    } else {
                        environmentService.createEnvironment(envName, collectionVars);
                        report.setEnvironments(report.getEnvironments() + 1);
                    }
                }
            }

            List<Item> items = collection.getItem() == null ? List.of() : collection.getItem();

            // Folder items at the top level each become a workspace collection.
            // Bare requests at the top level go into one collection named after the Postman collection.
            List<Item> bareRequests = new ArrayList<>();
            for (Item item : items) {
                if (isFolder(item)) {
                    importFolder(item, ownerId, report);
                } else if (item.getRequest() != null) {
                    bareRequests.add(item);
                } else {
                    report.getWarnings().add("Skipped item with no folder or request: " + safeName(item));
                }
            }

            if (!bareRequests.isEmpty()) {
                Collection target = findOrCreateCollection(
                        collectionDisplayName, ownerId, "Imported from Postman collection");
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
                        continue; // skip disabled variables (Postman uses enabled: false)
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

    private void importFolder(Item folder, String ownerId, ImportReport report) {
        Collection created = findOrCreateCollection(
                safeName(folder), ownerId, "Imported from Postman folder");
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

    /**
     * Translate Postman-specific template variables during import so that
     * Postman-imported templates produce semantically correct output in Dynamic Mock.
     *
     * <p>Specifically: Postman's {@code {{$timestamp}}} is documented as 10-digit epoch seconds,
     * whereas Dynamic Mock's native {@code {{$timestamp}}} produces 13-digit epoch milliseconds.
     * During import we rewrite Postman's variant to {@code {{$timestampSeconds}}} so the imported
     * route behaves identically to Postman without changing the native Dynamic Mock helper.
     *
     * <p>Only called during Postman collection import — never applied to native Dynamic Mock routes.
     */
    public String translatePostmanTemplate(String template) {
        if (template == null || template.isEmpty()) return template;
        return template.replaceAll(
                "\\{\\{\\s*\\$timestamp\\s*\\}\\}",
                java.util.regex.Matcher.quoteReplacement("{{$timestampSeconds}}")
        );
    }

    private void importRequest(Item item, String collectionId, String folderPrefix, ImportReport report) {
        String displayName = folderPrefix.isEmpty()
                ? safeName(item)
                : folderPrefix + "/" + safeName(item);

        try {
            Request request = item.getRequest();
            String defaultMethod = request.getMethod() == null || request.getMethod().isBlank()
                    ? null : request.getMethod().toUpperCase();
            String defaultPath = extractPath(request.getUrl());

            if (defaultMethod == null || defaultPath == null) {
                report.getWarnings().add("Skipped request '" + displayName + "': missing method or URL path");
                return;
            }

            if ((item.getEvent() != null && !item.getEvent().isEmpty())
                    || (request.getEvent() != null && !request.getEvent().isEmpty())) {
                report.getWarnings().add("Request '" + displayName + "': Postman scripts (prerequest/test) were skipped");
            }

            Body body = request.getBody();
            if (body != null && body.getMode() != null
                    && List.of("file", "formdata", "urlencoded", "graphql").contains(body.getMode())) {
                report.getWarnings().add("Request '" + displayName + "': body mode '" + body.getMode()
                        + "' was not converted (no body matcher created)");
            }

            // Saved examples usually live on the item (Postman v2.1 standard),
            // but some exports (and older clients) nest them inside the request.
            List<PostmanResponse> examples = item.getResponse() != null && !item.getResponse().isEmpty()
                    ? item.getResponse()
                    : (request.getResponse() != null ? request.getResponse() : List.of());

            if (examples.isEmpty()) {
                // Single route when no saved examples exist
                Map<String, Object> matchers = buildMatchers(request, defaultMethod);
                String matcherSig = buildMatcherSignature(matchers, body != null ? body.getRaw() : null);
                String compositeKey = collectionId + "/" + folderPrefix + "/" + defaultMethod + ":" + defaultPath + "#__default__@" + matcherSig;
                String routeId = generateUuidV5(UUIDV5_NAMESPACE, compositeKey).toString();

                Map<String, String> metadata = new LinkedHashMap<>();
                metadata.put("source", "postman");
                metadata.put("itemName", displayName);
                if (body != null && body.getRaw() != null && !body.getRaw().isBlank()) {
                    metadata.put("sampleRequestBody", body.getRaw());
                }

                @SuppressWarnings("unchecked")
                Map<String, String> qParams = (Map<String, String>) matchers.get("queryParams");

                CreateRouteRequest createRequest = CreateRouteRequest.builder()
                        .id(routeId)
                        .path(defaultPath)
                        .method(defaultMethod)
                        .matchers(matchers.isEmpty() ? null : matchers)
                        .queryParams(qParams != null && !qParams.isEmpty() ? qParams : null)
                        .responseTemplate(DEFAULT_RESPONSE_TEMPLATE)
                        .responseStatus(200)
                        .responseHeaders(null)
                        .priority(0)
                        .metadata(metadata)
                        .build();

                RouteResponse created = routeService.createRoute(createRequest);
                routeService.activateRoute(created.getId());

                addItemToCollectionIfNotPresent(collectionId, created.getId(), displayName, null, "REQUEST", extractMatchSummary(matchers));
                report.setRoutes(report.getRoutes() + 1);
            } else {
                // Detect ambiguous matchers among examples for the same method/path
                checkAndReportAmbiguities(displayName, defaultMethod, defaultPath, examples, report);

                // Multi-example splitting: each saved example becomes a distinct conditional MockRoute
                for (int i = 0; i < examples.size(); i++) {
                    PostmanResponse example = examples.get(i);
                    String exampleName = example.getName() != null && !example.getName().isBlank()
                            ? example.getName() : "Example " + (i + 1);

                    Request origReq = example.getOriginalRequest();
                    String exPath = origReq != null && origReq.getUrl() != null
                            ? extractPath(origReq.getUrl()) : defaultPath;
                    if (exPath == null) exPath = defaultPath;

                    String exMethod = origReq != null && origReq.getMethod() != null && !origReq.getMethod().isBlank()
                            ? origReq.getMethod().toUpperCase() : defaultMethod;
                    if (exMethod == null) exMethod = defaultMethod;

                    Map<String, Object> matchers = origReq != null ? buildMatchers(origReq, exMethod) : buildMatchers(request, defaultMethod);

                    String template = example.getBody() != null
                            ? translatePostmanTemplate(example.getBody())
                            : DEFAULT_RESPONSE_TEMPLATE;
                    Integer status = example.getCode() != null ? example.getCode() : 200;
                    Map<String, String> headers = headersToMap(example.getHeader());

                    // Stable deterministic identity
                    String routeId;
                    if (example.getPostmanId() != null && !example.getPostmanId().isBlank()) {
                        routeId = "pm-" + example.getPostmanId();
                    } else if (example.getId() != null && !example.getId().isBlank()) {
                        routeId = "pm-" + example.getId();
                    } else {
                        String origBody = origReq != null && origReq.getBody() != null ? origReq.getBody().getRaw() : null;
                        String matcherSig = buildMatcherSignature(matchers, origBody);
                        String compositeKey = collectionId + "/" + folderPrefix + "/" + exMethod + ":" + exPath + "#" + exampleName + "@" + matcherSig;
                        routeId = generateUuidV5(UUIDV5_NAMESPACE, compositeKey).toString();
                    }

                    Map<String, String> metadata = new LinkedHashMap<>();
                    metadata.put("source", "postman");
                    metadata.put("itemName", displayName);
                    metadata.put("exampleName", exampleName);
                    if (example.getPostmanId() != null) metadata.put("postmanExampleId", example.getPostmanId());
                    else if (example.getId() != null) metadata.put("postmanExampleId", example.getId());

                    String sampleBody = origReq != null && origReq.getBody() != null ? origReq.getBody().getRaw()
                            : (body != null ? body.getRaw() : null);
                    if (sampleBody != null && !sampleBody.isBlank()) {
                        metadata.put("sampleRequestBody", sampleBody);
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, String> qParams = (Map<String, String>) matchers.get("queryParams");

                    CreateRouteRequest createRequest = CreateRouteRequest.builder()
                            .id(routeId)
                            .path(exPath)
                            .method(exMethod)
                            .matchers(matchers.isEmpty() ? null : matchers)
                            .queryParams(qParams != null && !qParams.isEmpty() ? qParams : null)
                            .responseTemplate(template)
                            .responseStatus(status)
                            .responseHeaders(headers)
                            .priority(0)
                            .metadata(metadata)
                            .build();

                    RouteResponse created = routeService.createRoute(createRequest);
                    routeService.activateRoute(created.getId());

                    String collectionItemName = examples.size() > 1 && !displayName.equalsIgnoreCase(exampleName)
                            ? displayName + " - " + exampleName
                            : displayName;
                    addItemToCollectionIfNotPresent(collectionId, created.getId(), collectionItemName,
                            displayName, "EXAMPLE", extractMatchSummary(matchers));
                    report.setRoutes(report.getRoutes() + 1);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to import Postman request '{}': {}", displayName, e.getMessage());
            report.getErrors().add("Request '" + displayName + "': " + e.getMessage());
        }
    }

    private void addItemToCollectionIfNotPresent(String collectionId, String routeId, String itemName,
                                                 String parentItemId, String itemType, Map<String, String> matchSummary) {
        try {
            Collection target = collectionService.getCollection(collectionId);
            if (target != null && target.getItems() != null) {
                boolean alreadyPresent = target.getItems().stream()
                        .anyMatch(it -> routeId.equals(it.getResourceId()));
                if (alreadyPresent) {
                    return;
                }
            }
            collectionService.addItemToCollection(collectionId, Collection.CollectionItem.builder()
                    .protocol("HTTP")
                    .resourceId(routeId)
                    .name(itemName)
                    .parentItemId(parentItemId)
                    .itemType(itemType)
                    .matchSummary(matchSummary)
                    .build());
        } catch (Exception e) {
            log.warn("Could not register route {} in collection {}: {}", routeId, collectionId, e.getMessage());
        }
    }

    private Map<String, String> extractMatchSummary(Map<String, Object> matchers) {
        Map<String, String> summary = new LinkedHashMap<>();
        if (matchers != null) {
            Object qp = matchers.get("queryParams");
            if (qp instanceof Map<?, ?> qm) {
                qm.forEach((k, v) -> summary.put(String.valueOf(k), String.valueOf(v)));
            }
        }
        return summary.isEmpty() ? null : summary;
    }

    private void checkAndReportAmbiguities(String displayName, String method, String path,
                                           List<PostmanResponse> examples, ImportReport report) {
        if (examples == null || examples.size() < 2) return;
        for (int i = 0; i < examples.size(); i++) {
            for (int j = i + 1; j < examples.size(); j++) {
                PostmanResponse ex1 = examples.get(i);
                PostmanResponse ex2 = examples.get(j);
                Request req1 = ex1.getOriginalRequest();
                Request req2 = ex2.getOriginalRequest();
                int c1 = req1 != null ? countConstraints(req1, method) : 0;
                int c2 = req2 != null ? countConstraints(req2, method) : 0;
                if (c1 == c2) {
                    String name1 = ex1.getName() != null ? ex1.getName() : "Example " + (i + 1);
                    String name2 = ex2.getName() != null ? ex2.getName() : "Example " + (j + 1);
                    report.getWarnings().add(String.format(
                            "Ambiguous example matchers detected for %s %s: '%s' and '%s' both have %d constraint(s). Transparent deterministic fallback applied.",
                            method, path, name1, name2, c1));
                }
            }
        }
    }

    private int countConstraints(Request req, String method) {
        int count = 0;
        if (req.getHeader() != null) {
            for (Header h : req.getHeader()) {
                if (h.getKey() != null && !h.getKey().isBlank() && !Boolean.TRUE.equals(h.getDisabled())) {
                    if (isIgnoredHeader(h.getKey(), method)) {
                        continue;
                    }
                    count++;
                }
            }
        }
        if (req.getUrl() != null && req.getUrl().getQuery() != null) {
            for (PostmanCollectionModel.QueryParam q : req.getUrl().getQuery()) {
                if (q.getKey() != null && !q.getKey().isBlank() && !Boolean.TRUE.equals(q.getDisabled())) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isBodylessMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    private boolean isIgnoredHeader(String key, String method) {
        if (key == null || key.isBlank()) {
            return true;
        }
        String lower = key.trim().toLowerCase();
        // Body-related headers describe payload content; on bodyless HTTP methods (GET, HEAD)
        // they are non-applicable and should never be required as request matchers.
        if (isBodylessMethod(method)) {
            if ("content-type".equals(lower) || "content-length".equals(lower)) {
                return true;
            }
        }
        // Ephemeral or client-internal headers that should never become route matching constraints
        if ("postman-token".equals(lower) || "host".equals(lower) || "connection".equals(lower)) {
            return true;
        }
        return false;
    }

    private Collection findOrCreateCollection(String name, String ownerId, String description) {
        try {
            List<Collection> existing = collectionService.getCollectionsByUserId(ownerId);
            if (existing != null) {
                for (Collection col : existing) {
                    if (name.equalsIgnoreCase(col.getName())) {
                        log.info("Re-using existing collection '{}' ({})", name, col.getId());
                        return col;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error checking existing collections for user {}: {}", ownerId, e.getMessage());
        }
        return collectionService.createCollection(name, ownerId, description);
    }

    private boolean isFolder(Item item) {
        return item.getItem() != null && !item.getItem().isEmpty();
    }

    private String safeName(Item item) {
        return item.getName() == null || item.getName().isBlank() ? "Unnamed" : item.getName();
    }

    /**
     * Derive the mock path from a Postman URL: strip scheme+host, query string, hash fragment,
     * normalize trailing slashes, and convert Postman-style :param segments to {param}.
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

        // Strip fragment (#) and query string (?)
        int hashIdx = candidate.indexOf('#');
        if (hashIdx >= 0) {
            candidate = candidate.substring(0, hashIdx);
        }
        int queryIdx = candidate.indexOf('?');
        if (queryIdx >= 0) {
            candidate = candidate.substring(0, queryIdx);
        }
        candidate = candidate.trim();

        // Strip trailing slash if path is longer than 1 character (e.g. /mbank-verification/ -> /mbank-verification)
        if (candidate.length() > 1 && candidate.endsWith("/")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }

        return candidate.replaceAll("(?<=[/?]):([A-Za-z0-9_]+)", "{$1}");
    }

    /**
     * Convert Postman request headers and query params into route matchers.
     * Disabled entries, entries with blank values, and method-incompatible headers are skipped.
     * Redundant "Header-Name: " prefixes in header values are sanitized.
     */
    private Map<String, Object> buildMatchers(Request request) {
        return buildMatchers(request, request != null ? request.getMethod() : null);
    }

    private Map<String, Object> buildMatchers(Request request, String method) {
        Map<String, String> headerMatchers = new LinkedHashMap<>();
        if (request.getHeader() != null) {
            for (Header header : request.getHeader()) {
                if (header.getKey() == null || header.getKey().isBlank()) {
                    continue;
                }
                if (Boolean.TRUE.equals(header.getDisabled())) {
                    continue; // Skip disabled headers
                }
                if (isIgnoredHeader(header.getKey(), method)) {
                    continue; // Skip entity/ephemeral headers not applicable for this method
                }
                if (header.getValue() == null || header.getValue().isBlank()) {
                    continue;
                }
                String sanitizedVal = sanitizeHeaderValue(header.getKey(), header.getValue());
                headerMatchers.put(header.getKey(), sanitizedVal);
            }
        }

        Map<String, String> queryMatchers = new LinkedHashMap<>();
        Url url = request.getUrl();
        if (url != null && url.getQuery() != null) {
            for (PostmanCollectionModel.QueryParam param : url.getQuery()) {
                if (param.getKey() == null || param.getKey().isBlank()) {
                    continue;
                }
                if (Boolean.TRUE.equals(param.getDisabled())) {
                    continue; // Skip disabled query params
                }
                if (param.getValue() == null || param.getValue().isBlank()) {
                    continue;
                }
                queryMatchers.put(param.getKey(), param.getValue().trim());
            }
        }

        // Fallback: parse raw URL query string if url.getQuery() is missing/empty
        if (url != null && (url.getQuery() == null || url.getQuery().isEmpty()) && url.getRaw() != null && url.getRaw().contains("?")) {
            String raw = url.getRaw();
            int qIdx = raw.indexOf('?');
            int hIdx = raw.indexOf('#');
            String qStr = hIdx > qIdx ? raw.substring(qIdx + 1, hIdx) : raw.substring(qIdx + 1);
            for (String pair : qStr.split("&")) {
                if (!pair.isBlank()) {
                    int eq = pair.indexOf('=');
                    String k = eq >= 0 ? pair.substring(0, eq) : pair;
                    String v = eq >= 0 ? pair.substring(eq + 1) : "";
                    if (!k.isBlank() && !v.isBlank()) {
                        queryMatchers.putIfAbsent(k.trim(), v.trim());
                    }
                }
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

    private Map<String, String> headersToMap(List<Header> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Header header : headers) {
            if (header.getKey() == null || header.getKey().isBlank()) {
                continue;
            }
            if (Boolean.TRUE.equals(header.getDisabled())) {
                continue; // Skip disabled response headers
            }
            String sanitizedVal = sanitizeHeaderValue(header.getKey(), header.getValue() == null ? "" : header.getValue());
            result.put(header.getKey(), sanitizedVal);
        }
        return result.isEmpty() ? null : result;
    }

    private String sanitizeHeaderValue(String key, String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (key != null && !key.isBlank() && trimmed.toLowerCase().startsWith(key.toLowerCase() + ":")) {
            return trimmed.substring(key.length() + 1).trim();
        }
        return trimmed;
    }

    @SuppressWarnings("unchecked")
    private String buildMatcherSignature(Map<String, Object> matchers, String rawBody) {
        StringBuilder sb = new StringBuilder();

        // 1. Query Parameters: sorted alphabetically key ASC, value ASC
        sb.append("q=[");
        if (matchers != null && matchers.get("queryParams") instanceof Map<?, ?> qp) {
            List<String> sortedParams = qp.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null)
                    .map(e -> String.valueOf(e.getKey()) + "=" + String.valueOf(e.getValue()))
                    .sorted()
                    .toList();
            sb.append(String.join("&", sortedParams));
        }
        sb.append("]");

        // 2. Headers: filtered and sorted alphabetically by lowercase name ASC, value ASC
        sb.append("&h=[");
        if (matchers != null && matchers.get("headers") instanceof Map<?, ?> hdrs) {
            List<String> sortedHeaders = hdrs.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null)
                    .map(e -> Map.entry(String.valueOf(e.getKey()).toLowerCase(), String.valueOf(e.getValue())))
                    .filter(e -> !HOP_BY_HOP_HEADERS.contains(e.getKey()))
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .sorted()
                    .toList();
            sb.append(String.join("|", sortedHeaders));
        }
        sb.append("]");

        // 3. Body: SHA-256 hash
        sb.append("&b=[");
        if (rawBody != null && !rawBody.trim().isEmpty()) {
            sb.append(sha256Hex(rawBody.trim()));
        } else {
            sb.append("none");
        }
        sb.append("]");

        return sb.toString();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return "hash-err";
        }
    }

    public static UUID generateUuidV5(UUID namespace, String name) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(namespace.getMostSignificantBits());
            bb.putLong(namespace.getLeastSignificantBits());
            md.update(bb.array());
            md.update(name.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = md.digest();
            bytes[6] = (byte) ((bytes[6] & 0x0f) | 0x50); // version 5
            bytes[8] = (byte) ((bytes[8] & 0x3f) | 0x80); // variant RFC 4122
            ByteBuffer res = ByteBuffer.wrap(bytes);
            return new UUID(res.getLong(), res.getLong());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }
}

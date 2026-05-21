package com.dynamicmock.application.service;

import com.dynamicmock.adapter.in.web.dto.GraphQLEndpointRequest;
import com.dynamicmock.adapter.out.script.ScriptContext;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.domain.entity.GraphQLEndpoint;
import com.dynamicmock.domain.entity.GraphQLEndpoint.ResolverConfig;
import com.dynamicmock.domain.port.out.GraphQLEndpointRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQLService {
    
    private final GraphQLEndpointRepository repository;
    private final ResponseTemplateEngine templateEngine;
    private final ScriptEngine scriptEngine;
    private final ObjectMapper objectMapper;
    
    // Cache for compiled GraphQL schemas
    private final Map<String, GraphQL> graphqlCache = new ConcurrentHashMap<>();
    
    public List<GraphQLEndpoint> findAll() {
        return repository.findAll();
    }
    
    public GraphQLEndpoint findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("GraphQL endpoint not found: " + id));
    }
    
    public GraphQLEndpoint create(GraphQLEndpointRequest request) {
        if (repository.existsByName(request.getName())) {
            throw new RuntimeException("GraphQL endpoint with name already exists: " + request.getName());
        }
        
        GraphQLEndpoint endpoint = GraphQLEndpoint.builder()
                .name(request.getName())
                .description(request.getDescription())
                .schema(request.getSchema())
                .resolvers(request.getResolvers())
                .active(request.getActive() != null ? request.getActive() : false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        GraphQLEndpoint saved = repository.save(endpoint);
        
        if (Boolean.TRUE.equals(saved.getActive())) {
            activateEndpoint(saved);
        }
        
        return saved;
    }
    
    public GraphQLEndpoint update(String id, GraphQLEndpointRequest request) {
        GraphQLEndpoint existing = findById(id);
        
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setSchema(request.getSchema());
        existing.setResolvers(request.getResolvers());
        existing.setActive(request.getActive());
        existing.setUpdatedAt(LocalDateTime.now());
        
        GraphQLEndpoint saved = repository.save(existing);
        
        // Update cache
        if (Boolean.TRUE.equals(saved.getActive())) {
            activateEndpoint(saved);
        } else {
            deactivateEndpoint(saved.getId());
        }
        
        return saved;
    }
    
    public void delete(String id) {
        deactivateEndpoint(id);
        repository.deleteById(id);
    }
    
    public GraphQLEndpoint activate(String id) {
        GraphQLEndpoint endpoint = findById(id);
        endpoint.setActive(true);
        endpoint.setUpdatedAt(LocalDateTime.now());
        GraphQLEndpoint saved = repository.save(endpoint);
        activateEndpoint(saved);
        return saved;
    }
    
    public GraphQLEndpoint deactivate(String id) {
        GraphQLEndpoint endpoint = findById(id);
        endpoint.setActive(false);
        endpoint.setUpdatedAt(LocalDateTime.now());
        deactivateEndpoint(id);
        return repository.save(endpoint);
    }
    
    /**
     * Execute a GraphQL query/mutation against an endpoint
     */
    public Map<String, Object> execute(String endpointId, String query, String operationName, Map<String, Object> variables) {
        GraphQL graphql = graphqlCache.get(endpointId);
        if (graphql == null) {
            GraphQLEndpoint endpoint = findById(endpointId);
            graphql = buildGraphQL(endpoint);
            graphqlCache.put(endpointId, graphql);
        }
        
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .operationName(operationName)
                .variables(variables != null ? variables : new HashMap<>())
                .build();
        
        ExecutionResult result = graphql.execute(executionInput);
        
        Map<String, Object> response = new HashMap<>();
        if (result.getData() != null) {
            response.put("data", result.getData());
        }
        if (!result.getErrors().isEmpty()) {
            response.put("errors", result.getErrors());
        }
        
        return response;
    }
    
    private void activateEndpoint(GraphQLEndpoint endpoint) {
        try {
            GraphQL graphql = buildGraphQL(endpoint);
            graphqlCache.put(endpoint.getId(), graphql);
            log.info("Activated GraphQL endpoint: {}", endpoint.getName());
        } catch (Exception e) {
            log.error("Failed to activate GraphQL endpoint: {}", endpoint.getName(), e);
            throw new RuntimeException("Failed to compile GraphQL schema: " + e.getMessage(), e);
        }
    }
    
    private void deactivateEndpoint(String id) {
        graphqlCache.remove(id);
        log.info("Deactivated GraphQL endpoint: {}", id);
    }
    
    private GraphQL buildGraphQL(GraphQLEndpoint endpoint) {
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeRegistry = schemaParser.parse(endpoint.getSchema());
        
        RuntimeWiring.Builder wiringBuilder = RuntimeWiring.newRuntimeWiring();
        
        // Build data fetchers for each resolver
        if (endpoint.getResolvers() != null) {
            Map<String, Map<String, DataFetcher<?>>> fetchersByType = new HashMap<>();
            
            for (ResolverConfig resolver : endpoint.getResolvers()) {
                String typeName = getTypeName(resolver.getOperationType());
                fetchersByType.computeIfAbsent(typeName, k -> new HashMap<>())
                        .put(resolver.getFieldName(), createDataFetcher(resolver));
            }
            
            for (Map.Entry<String, Map<String, DataFetcher<?>>> entry : fetchersByType.entrySet()) {
                wiringBuilder.type(entry.getKey(), builder -> {
                    for (Map.Entry<String, DataFetcher<?>> fetcher : entry.getValue().entrySet()) {
                        builder.dataFetcher(fetcher.getKey(), fetcher.getValue());
                    }
                    return builder;
                });
            }
        }
        
        RuntimeWiring wiring = wiringBuilder.build();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema schema = schemaGenerator.makeExecutableSchema(typeRegistry, wiring);
        
        return GraphQL.newGraphQL(schema).build();
    }
    
    private String getTypeName(String operationType) {
        if (operationType == null) return "Query";
        return switch (operationType.toUpperCase()) {
            case "MUTATION" -> "Mutation";
            case "SUBSCRIPTION" -> "Subscription";
            default -> "Query";
        };
    }
    
    private DataFetcher<?> createDataFetcher(ResolverConfig resolver) {
        return environment -> {
            // Apply delay if configured
            if (resolver.getDelayMs() != null && resolver.getDelayMs() > 0) {
                Thread.sleep(resolver.getDelayMs());
            }
            
            // Build context for template/script
            Map<String, Object> context = new HashMap<>();
            context.put("arguments", environment.getArguments());
            context.put("source", environment.getSource());
            context.put("fieldName", environment.getField().getName());
            
            String responseJson;
            
            // Execute script if provided
            if (resolver.getScript() != null && !resolver.getScript().trim().isEmpty()) {
                ScriptContext scriptContext = new ScriptContext();
                scriptContext.setBody(objectMapper.writeValueAsString(environment.getArguments()));
                scriptEngine.execute(resolver.getScript(), 
                        resolver.getScriptLanguage() != null ? resolver.getScriptLanguage() : "js", 
                        scriptContext);
                responseJson = scriptContext.getResponseBody();
            } else {
                // Use template
                responseJson = templateEngine.render(resolver.getResponseTemplate(), context);
            }
            
            // Parse JSON response
            try {
                return objectMapper.readValue(responseJson, Object.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse resolver response: {}", responseJson, e);
                throw new RuntimeException("Invalid JSON response from resolver", e);
            }
        };
    }
    
    /**
     * Reload all active endpoints on startup
     */
    public void reloadActiveEndpoints() {
        List<GraphQLEndpoint> activeEndpoints = repository.findByActiveTrue();
        for (GraphQLEndpoint endpoint : activeEndpoints) {
            try {
                activateEndpoint(endpoint);
            } catch (Exception e) {
                log.error("Failed to reload GraphQL endpoint: {}", endpoint.getName(), e);
            }
        }
        log.info("Reloaded {} active GraphQL endpoints", activeEndpoints.size());
    }
}


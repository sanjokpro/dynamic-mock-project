package com.dynamicmock.adapter.out.protocol.grpc;

import com.dynamicmock.adapter.out.script.ScriptContext;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.domain.entity.GrpcEndpoint;
import com.dynamicmock.domain.entity.GrpcEndpoint.MethodConfig;
import tools.jackson.databind.ObjectMapper;
import io.grpc.*;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dynamic gRPC server that can create mock services at runtime
 * without pre-compiled protobuf classes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicGrpcServer {
    
    private final ResponseTemplateEngine templateEngine;
    private final ScriptEngine scriptEngine;
    private final ObjectMapper objectMapper;
    
    @Value("${grpc.default-port:9090}")
    private int defaultPort;
    
    // Track running servers by port
    private final Map<Integer, Server> servers = new ConcurrentHashMap<>();
    private final AtomicInteger portCounter = new AtomicInteger(9090);
    
    /**
     * Start a gRPC service for the given endpoint
     * Returns the port the service is running on
     */
    public int startService(GrpcEndpoint endpoint) throws IOException {
        int port = endpoint.getPort() != null ? endpoint.getPort() : portCounter.getAndIncrement();
        
        // Build service definition
        ServerServiceDefinition serviceDefinition = buildServiceDefinition(endpoint);
        
        // Create and start the server
        Server server = ServerBuilder.forPort(port)
                .addService(serviceDefinition)
                .build()
                .start();
        
        servers.put(port, server);
        log.info("Started gRPC server for service '{}' on port {}", endpoint.getServiceName(), port);
        
        return port;
    }
    
    /**
     * Stop a gRPC service running on the specified port
     */
    public void stopService(int port) {
        Server server = servers.remove(port);
        if (server != null) {
            server.shutdown();
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("Stopped gRPC server on port {}", port);
        }
    }
    
    /**
     * Shutdown all running servers
     */
    @PreDestroy
    public void shutdownAll() {
        for (Map.Entry<Integer, Server> entry : servers.entrySet()) {
            try {
                entry.getValue().shutdown();
                if (!entry.getValue().awaitTermination(5, TimeUnit.SECONDS)) {
                    entry.getValue().shutdownNow();
                }
            } catch (InterruptedException e) {
                entry.getValue().shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        servers.clear();
        log.info("Shutdown all gRPC servers");
    }
    
    private ServerServiceDefinition buildServiceDefinition(GrpcEndpoint endpoint) {
        ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder(endpoint.getServiceName());
        
        if (endpoint.getMethods() != null) {
            for (MethodConfig method : endpoint.getMethods()) {
                MethodDescriptor<byte[], byte[]> methodDescriptor = createMethodDescriptor(
                        endpoint.getServiceName(), 
                        method.getMethodName(),
                        getMethodType(method.getMethodType())
                );
                
                ServerCallHandler<byte[], byte[]> handler = createHandler(method);
                builder.addMethod(methodDescriptor, handler);
            }
        }
        
        return builder.build();
    }
    
    private MethodDescriptor<byte[], byte[]> createMethodDescriptor(
            String serviceName, String methodName, MethodDescriptor.MethodType methodType) {
        
        return MethodDescriptor.<byte[], byte[]>newBuilder()
                .setType(methodType)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
                .setRequestMarshaller(new ByteArrayMarshaller())
                .setResponseMarshaller(new ByteArrayMarshaller())
                .build();
    }
    
    private MethodDescriptor.MethodType getMethodType(String type) {
        if (type == null) return MethodDescriptor.MethodType.UNARY;
        return switch (type.toUpperCase()) {
            case "SERVER_STREAMING" -> MethodDescriptor.MethodType.SERVER_STREAMING;
            case "CLIENT_STREAMING" -> MethodDescriptor.MethodType.CLIENT_STREAMING;
            case "BIDI_STREAMING" -> MethodDescriptor.MethodType.BIDI_STREAMING;
            default -> MethodDescriptor.MethodType.UNARY;
        };
    }
    
    private ServerCallHandler<byte[], byte[]> createHandler(MethodConfig method) {
        String methodType = method.getMethodType() != null ? method.getMethodType().toUpperCase() : "UNARY";
        
        return switch (methodType) {
            case "SERVER_STREAMING" -> ServerCalls.asyncServerStreamingCall(
                    (request, responseObserver) -> handleServerStreaming(method, request, responseObserver)
            );
            case "CLIENT_STREAMING" -> ServerCalls.asyncClientStreamingCall(
                    responseObserver -> handleClientStreaming(method, responseObserver)
            );
            case "BIDI_STREAMING" -> ServerCalls.asyncBidiStreamingCall(
                    responseObserver -> handleBidiStreaming(method, responseObserver)
            );
            default -> ServerCalls.asyncUnaryCall(
                    (request, responseObserver) -> handleUnary(method, request, responseObserver)
            );
        };
    }
    
    private void handleUnary(MethodConfig method, byte[] request, StreamObserver<byte[]> responseObserver) {
        try {
            // Apply delay if configured
            if (method.getDelayMs() != null && method.getDelayMs() > 0) {
                Thread.sleep(method.getDelayMs());
            }
            
            // Check for error status
            if (method.getStatusCode() != null && !"OK".equalsIgnoreCase(method.getStatusCode())) {
                Status status = Status.fromCode(Status.Code.valueOf(method.getStatusCode()));
                if (method.getErrorMessage() != null) {
                    status = status.withDescription(method.getErrorMessage());
                }
                responseObserver.onError(status.asRuntimeException());
                return;
            }
            
            // Generate response
            String responseJson = generateResponse(method, request);
            byte[] responseBytes = responseJson.getBytes();
            
            responseObserver.onNext(responseBytes);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error handling gRPC unary call", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
    
    private void handleServerStreaming(MethodConfig method, byte[] request, StreamObserver<byte[]> responseObserver) {
        try {
            // Apply delay if configured
            if (method.getDelayMs() != null && method.getDelayMs() > 0) {
                Thread.sleep(method.getDelayMs());
            }
            
            // Send multiple responses if configured
            if (method.getStreamResponses() != null && !method.getStreamResponses().isEmpty()) {
                for (String template : method.getStreamResponses()) {
                    Map<String, Object> context = new HashMap<>();
                    context.put("request", new String(request));
                    String responseJson = templateEngine.render(template, context);
                    responseObserver.onNext(responseJson.getBytes());
                }
            } else {
                // Single response
                String responseJson = generateResponse(method, request);
                responseObserver.onNext(responseJson.getBytes());
            }
            
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error handling gRPC server streaming call", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
    
    private StreamObserver<byte[]> handleClientStreaming(MethodConfig method, StreamObserver<byte[]> responseObserver) {
        return new StreamObserver<>() {
            private final StringBuilder requestBuffer = new StringBuilder();
            
            @Override
            public void onNext(byte[] request) {
                requestBuffer.append(new String(request));
            }
            
            @Override
            public void onError(Throwable t) {
                log.error("Client streaming error", t);
            }
            
            @Override
            public void onCompleted() {
                try {
                    String responseJson = generateResponse(method, requestBuffer.toString().getBytes());
                    responseObserver.onNext(responseJson.getBytes());
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    responseObserver.onError(Status.INTERNAL
                            .withDescription(e.getMessage())
                            .asRuntimeException());
                }
            }
        };
    }
    
    private StreamObserver<byte[]> handleBidiStreaming(MethodConfig method, StreamObserver<byte[]> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(byte[] request) {
                try {
                    String responseJson = generateResponse(method, request);
                    responseObserver.onNext(responseJson.getBytes());
                } catch (Exception e) {
                    responseObserver.onError(Status.INTERNAL
                            .withDescription(e.getMessage())
                            .asRuntimeException());
                }
            }
            
            @Override
            public void onError(Throwable t) {
                log.error("Bidi streaming error", t);
            }
            
            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
    
    private String generateResponse(MethodConfig method, byte[] request) {
        Map<String, Object> context = new HashMap<>();
        context.put("request", new String(request));
        
        // Execute script if provided
        if (method.getScript() != null && !method.getScript().trim().isEmpty()) {
            ScriptContext scriptContext = new ScriptContext();
            scriptContext.setBody(new String(request));
            scriptEngine.execute(method.getScript(),
                    method.getScriptLanguage() != null ? method.getScriptLanguage() : "js",
                    scriptContext);
            return scriptContext.getResponseBody() != null ? scriptContext.getResponseBody() : "{}";
        }
        
        // Use template
        return templateEngine.render(method.getResponseTemplate(), context);
    }
    
    /**
     * Simple byte array marshaller for dynamic gRPC handling
     */
    private static class ByteArrayMarshaller implements MethodDescriptor.Marshaller<byte[]> {
        @Override
        public java.io.InputStream stream(byte[] value) {
            return new java.io.ByteArrayInputStream(value);
        }
        
        @Override
        public byte[] parse(java.io.InputStream stream) {
            try {
                return stream.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read request", e);
            }
        }
    }
}


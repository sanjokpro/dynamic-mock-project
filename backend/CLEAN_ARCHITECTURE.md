# Clean Architecture Implementation

## Overview

This project follows **Clean Architecture** principles by Robert C. Martin (Uncle Bob), organizing code into concentric layers with dependencies pointing inward.

```
┌─────────────────────────────────────────────────────────────────┐
│                     FRAMEWORKS & DRIVERS                        │
│  (Spring Boot, MongoDB, Redis, jPOS, gRPC, GraphQL)            │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │               INTERFACE ADAPTERS                          │  │
│  │  (Controllers, Repository Impls, Protocol Handlers)       │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │          APPLICATION BUSINESS RULES                 │  │  │
│  │  │  (Use Cases, Application Services)                  │  │  │
│  │  │  ┌───────────────────────────────────────────────┐  │  │  │
│  │  │  │   ENTERPRISE BUSINESS RULES                   │  │  │  │
│  │  │  │   (Entities, Domain Services)                 │  │  │  │
│  │  │  └───────────────────────────────────────────────┘  │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Package Structure

### 1. Domain Layer (`com.dynamicmock.domain`)
**Purpose**: Core business logic, framework-independent

- **`domain.entity`** - Core entities
  - `MockRoute` - HTTP mock route configuration
  - `Scenario` - Stateful workflow entity
  - `RouteVersion` - Version history snapshot
  - `GraphQLEndpoint` - GraphQL service configuration
  - `GrpcEndpoint` - gRPC service configuration
  - `Iso8583Endpoint` - ISO8583 financial message configuration

- **`domain.port.in`** - Input ports (use case interfaces)
  - Defines what the application can do
  - Called by adapters (controllers)
  
- **`domain.port.out`** - Output ports (repository/cache interfaces)
  - `MockRouteRepository`
  - `ScenarioRepository`
  - `RouteVersionRepository`
  - `GraphQLEndpointRepository`
  - `GrpcEndpointRepository`
  - `Iso8583EndpointRepository`

### 2. Application Layer (`com.dynamicmock.application`)
**Purpose**: Orchestrates domain objects to fulfill use cases

- **`application.service`** - Application services
  - `RouteService` - Route management orchestration
  - `ScenarioService` - Scenario state management
  - `VersionService` - Version control orchestration
  - `GraphQLService` - GraphQL endpoint management
  - `GrpcService` - gRPC endpoint management
  - `Iso8583Service` - ISO8583 endpoint management

### 3. Adapter Layer (`com.dynamicmock.adapter`)
**Purpose**: Convert data between use cases and external systems

#### Input Adapters (`adapter.in`)
- **`adapter.in.web`** - REST API controllers
  - `RouteController` - Route management REST API
  - `ScenarioController` - Scenario management REST API
  - `GraphQLController` - GraphQL endpoint management REST API
  - `GrpcController` - gRPC endpoint management REST API
  - `Iso8583Controller` - ISO8583 endpoint management REST API
  - **`dto/`** - Data Transfer Objects for API communication

#### Output Adapters (`adapter.out`)
- **`adapter.out.persistence`** - Database adapters (future: impl classes if needed)
- **`adapter.out.cache`** - Redis cache adapters (future)
- **`adapter.out.protocol`** - Protocol-specific handlers
  - `grpc/DynamicGrpcServer` - gRPC server adapter
  - `iso8583/Iso8583Server` - ISO8583 TCP server adapter
  - `iso8583/Q2ServerManager` - jPOS Q2 framework adapter
  - `iso8583/DynamicMockRequestListener` - jPOS request handler
- **`adapter.out.template`** - Template rendering adapter
  - `ResponseTemplateEngine` - Handlebars template adapter
  - `RandomDataFunctions` - Random data generation
- **`adapter.out.script`** - Script execution adapter
  - `ScriptEngine` - GraalVM polyglot adapter
  - `ScriptContext` - Script execution context

### 4. Infrastructure Layer (`com.dynamicmock.infrastructure`)
**Purpose**: Framework-specific configuration and cross-cutting concerns

- **`infrastructure.config`** - Spring Boot configuration
  - `MongoConfig` - MongoDB configuration
  - `RedisConfig` - Redis configuration
  - `WebConfig` - Web MVC configuration
  - `ProtocolInitializer` - Protocol services initialization
  - `RouteRegistryInitializer` - Route registry startup
  - `Iso8583Config` - ISO8583/jPOS integration
  - `DemoDataLoader` - Demo data loader

- **`infrastructure.filter`** - HTTP filters and interceptors
  - `DynamicRouteDispatcher` - Main request interceptor for `/mock/**`
  - `RequestMatcher` - Request matching logic
  - `RouteRegistry` - In-memory route registry
  - `CachedBodyFilter` - Request body caching filter
  - `CachedBodyHttpServletRequest` - Cached request wrapper

## Dependency Rule

**The Dependency Rule**: Source code dependencies must point **inward only**.

- ✅ **Domain** depends on: Nothing (pure Java)
- ✅ **Application** depends on: Domain only
- ✅ **Adapters** depend on: Domain, Application
- ✅ **Infrastructure** depends on: All layers (wires everything together)

## Key Principles

1. **Independence of Frameworks**
   - Domain and application logic don't depend on Spring, MongoDB, Redis
   - Can swap frameworks without touching core logic

2. **Testability**
   - Domain and application layers can be tested without Spring context
   - Mock adapters easily for unit testing

3. **Independence of UI**
   - Business logic doesn't know about REST/HTTP
   - Can add gRPC, GraphQL, CLI without changing core

4. **Independence of Database**
   - Domain doesn't know about MongoDB
   - Can swap to PostgreSQL, DynamoDB, etc.

5. **Independence of External Services**
   - Business logic doesn't depend on external APIs
   - Protocol adapters can be swapped

## Benefits for This Project

1. **Multi-Protocol Support**: Easy to add new protocols (WebSocket, MQTT, etc.) as new adapters
2. **Testing**: Domain logic tested independently of MongoDB/Redis/Spring
3. **Evolution**: Can change databases, frameworks, or UIs without affecting core logic
4. **Clarity**: Clear boundaries between layers, easy to understand responsibilities

## Migration Status

- [x] Package structure created
- [x] Entities moved to `domain.entity`
- [x] Repository interfaces moved to `domain.port.out`
- [x] Services moved to `application.service`
- [x] Controllers moved to `adapter.in.web`
- [x] DTOs moved to `adapter.in.web.dto`
- [x] Template/Script engines moved to `adapter.out.*`
- [x] Protocol handlers moved to `adapter.out.protocol`
- [x] Configuration moved to `infrastructure.config`
- [x] Filters/interceptors moved to `infrastructure.filter`
- [ ] Update all package declarations
- [ ] Update all imports across codebase
- [ ] Update test imports
- [ ] Verify compilation
- [ ] Run full test suite

## Next Steps

Due to the scope of this refactoring (100+ files with updated imports), the remaining work includes:

1. **Update package declarations** in all moved files
2. **Update imports** across entire codebase (using bulk find/replace)
3. **Update test files** in `backend/src/test/java/com/dynamicmock/**`  
4. **Recompile and fix** any remaining issues
5. **Verify all tests pass**

This refactoring is in progress. The physical files have been moved to their new locations following Clean Architecture layers.


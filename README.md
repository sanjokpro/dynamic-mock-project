# Dynamic Mock API Server

A powerful, dynamic mock API server with GraalVM polyglot scripting support, combining the flexibility of Postman, WireMock, and Apidog.

## Features

- **Dynamic Route Creation**: Create mock endpoints without server restart
- **Polyglot Scripting**: Execute JavaScript and Python scripts via GraalVM
- **Multiple Protocols**: REST, gRPC, GraphQL, and ISO8583 support
- **Response Templating**: Handlebars templates with built-in random data functions
- **Stateful Mocking**: Support for scenarios and variable resolution
- **Versioning**: Mock API versioning with rollback and diff capabilities
- **Built-in Random Functions**: `{{$randomInt}}`, `{{$randomBool}}`, `{{$randomString}}`, `{{$timestamp}}`, etc.

## Architecture

This project follows **Clean Architecture** principles (Uncle Bob), ensuring maintainability, testability, and framework independence.

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│  Infrastructure (Frameworks & Drivers)                      │
│  ├─ Spring Boot, MongoDB, Redis, jPOS, gRPC                │
│  └─ infrastructure.config, infrastructure.filter           │
│     ┌───────────────────────────────────────────────────┐   │
│     │  Adapters (Interface Adapters)                    │   │
│     │  ├─ IN:  adapter.in.web (REST Controllers)        │   │
│     │  └─ OUT: adapter.out.* (DB, Cache, Protocols)     │   │
│     │     ┌─────────────────────────────────────────┐   │   │
│     │     │  Application (Use Cases)                │   │   │
│     │     │  └─ application.service                 │   │   │
│     │     │     ┌───────────────────────────────┐   │   │   │
│     │     │     │  Domain (Entities & Ports)    │   │   │   │
│     │     │     │  ├─ domain.entity             │   │   │   │
│     │     │     │  └─ domain.port.out           │   │   │   │
│     │     │     └───────────────────────────────┘   │   │   │
│     │     └─────────────────────────────────────────┘   │   │
│     └───────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**Package Structure:**
- `domain.entity` - Core entities (MockRoute, Scenario, GraphQLEndpoint, etc.)
- `domain.port.out` - Repository interfaces (MongoDB abstraction)
- `application.service` - Business logic orchestration
- `adapter.in.web` - REST API controllers + DTOs
- `adapter.out.*` - External system adapters (DB, cache, protocols, scripting)
- `infrastructure.*` - Framework configuration and filters

**Benefits:**
- ✅ **Testable**: Domain logic tests without Spring/MongoDB
- ✅ **Flexible**: Swap databases, frameworks, or protocols easily
- ✅ **Maintainable**: Clear boundaries and responsibilities
- ✅ **Independent**: Core logic has zero framework dependencies

See `backend/CLEAN_ARCHITECTURE.md` for detailed documentation.

### Technology Stack

#### Backend
- **Framework**: Spring Boot 3.x with GraalVM Runtime
- **Database**: MongoDB for route definitions
- **Cache**: Redis for runtime state
- **Scripting**: GraalVM Polyglot API (JavaScript, Python)
- **ISO8583**: jPOS framework with Q2 hot-deployment

#### Frontend
- **Framework**: Angular 17+ with TypeScript
- **UI**: Angular Material with custom Nepali rupee papernote theme
- **Editor**: Monaco Editor for script editing

## Getting Started

### Prerequisites

- Java 21+
- Gradle 8.6+ (build tool)
- MongoDB 6.0+
- Redis 7.0+
- GraalVM 23.1+ (for polyglot scripting)
- Node.js 18+ and npm (for frontend)
- Angular CLI 17+ (for frontend)

### One-Command Demo (Docker)

If you just want to see the server running with sample data:

```bash
docker compose up --build
```

What this does:
- Builds the backend with GraalVM and starts `mock-server`, `mongo`, and `redis`
- Enables the `demo` Spring profile, which auto-seeds example routes and a scenario
- Persists Mongo/Redis data in named Docker volumes
- Serves the Angular UI at `http://localhost:8080/`
- You do NOT need Node locally; the Angular UI is built inside the Docker image

Try it out:
- `curl http://localhost:8080/mock/hello?name=Alex`
- Scenario flow:
  - `curl http://localhost:8080/mock/orders/demo`
  - `curl "http://localhost:8080/mock/orders/demo?action=pay"`
  - `curl "http://localhost:8080/mock/orders/demo?action=ship"` (auto-resets after the final state)
UI:
- Open `http://localhost:8080/` for the web UI (served by Spring Boot)
- API base remains `http://localhost:8080/api`

Stop the demo with `docker compose down` (add `--volumes` to wipe data).

### Backend Setup (Default Dev Mode)

1. Clone the repository:
```bash
git clone <repository-url>
cd dynamic-mock-project
```

2. Configure MongoDB and Redis in `backend/src/main/resources/application.yml`:
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/dynamicmock
    redis:
      host: localhost
      port: 6379
```

3. Run the backend with **Gradle**:
```bash
# Gradle
cd backend
gradle bootRun
```

The server will start on `http://localhost:8080`

### Frontend Setup (Dev Mode)

1. Install dependencies:
```bash
cd frontend
npm install
```

2. Start the development server:
```bash
npm start
```

The frontend will be available at `http://localhost:4200`

For more details, see [frontend/README.md](frontend/README.md)

---

## Build & Run Modes (Backend + UI)

The project supports multiple build modes so you can either:
- run **backend and Angular separately** during development, or
- build a **single self-contained artifact** (JAR/WAR) with the UI embedded, or
- build a **WAR for external Tomcat**.

All of these are controlled via **Gradle properties** (`packagingType`, `buildWithEmbeddedUi`) defined in `backend/build.gradle`.

### 1. Dev: Backend + Angular Separate

- **Backend** (Spring Boot):
  ```bash
  cd backend
  gradle bootRun
  ```

- **Frontend** (Angular dev server):
  ```bash
  cd frontend
  npm install --legacy-peer-deps
  npm start     # or: ng serve
  ```

- Angular app runs on `http://localhost:4200`
- Backend API runs on `http://localhost:8080/api`

This is the recommended mode for daily development (hot reload on the Angular side).

### 2. Self-Contained JAR with Embedded UI

Build Angular and bundle the `dist` output into Spring Boot static resources:

```bash
cd backend
gradle clean bootJar -PbuildWithEmbeddedUi=true
```

Run:
```bash
java -jar build/libs/dynamic-mock-server-1.0.0-SNAPSHOT.jar
```

- Backend API: `http://localhost:8080/api`
- Angular UI (served by Spring Boot): `http://localhost:8080/`

### 3. Self-Contained WAR with Embedded Container + UI

Build an executable WAR (still runs standalone) and bundle the UI:

```bash
cd backend
gradle clean bootWar -PpackagingType=warEmbedded -PbuildWithEmbeddedUi=true
```

Run:
```bash
java -jar build/libs/dynamic-mock-server-1.0.0-SNAPSHOT.war
```

### 4. WAR for External Tomcat (Optional)

Create a WAR that assumes a servlet container provides Tomcat:

```bash
cd backend
gradle clean bootWar -PpackagingType=warProvided          # backend APIs only
gradle clean bootWar -PpackagingType=warProvided -PbuildWithEmbeddedUi=true  # WAR + UI
```

Deploy the generated WAR from `backend/build/libs/` into your external container.

---

## Protocol Support

### REST/HTTP Mocking

Create dynamic REST endpoints with templating and scripting.

```bash
POST /api/routes
Content-Type: application/json

{
  "path": "/users/{id}",
  "method": "GET",
  "responseTemplate": "{\"id\":\"{{$randomUUID}}\",\"name\":\"{{$randomString}}\"}",
  "responseStatus": 200,
  "responseHeaders": {
    "Content-Type": "application/json"
  },
  "matchers": {
    "headers": {"Authorization": "Bearer .*"},
    "queryParams": {"page": "\\d+"}
  },
  "delayMs": 100
}
```

### GraphQL Mocking

Create GraphQL endpoints with schema and resolver configuration.

```bash
POST /api/graphql/endpoints
Content-Type: application/json

{
  "name": "User Service",
  "schema": "type Query { user(id: ID!): User users: [User] } type User { id: ID! name: String email: String }",
  "resolvers": [
    {
      "operationType": "QUERY",
      "fieldName": "user",
      "responseTemplate": "{\"id\": \"{{arguments.id}}\", \"name\": \"{{$randomString}}\", \"email\": \"{{$randomEmail}}\"}"
    },
    {
      "operationType": "QUERY", 
      "fieldName": "users",
      "responseTemplate": "[{\"id\": \"1\", \"name\": \"John\"}, {\"id\": \"2\", \"name\": \"Jane\"}]"
    }
  ],
  "active": true
}
```

Execute queries:
```bash
POST /api/graphql/endpoints/{id}/execute
Content-Type: application/json

{
  "query": "query { user(id: \"123\") { id name email } }",
  "variables": {}
}
```

### gRPC Mocking

Create gRPC services with method-level configuration.

```bash
POST /api/grpc/endpoints
Content-Type: application/json

{
  "name": "User gRPC Service",
  "serviceName": "com.example.UserService",
  "port": 9090,
  "methods": [
    {
      "methodName": "GetUser",
      "methodType": "UNARY",
      "responseTemplate": "{\"id\": \"{{request.id}}\", \"name\": \"{{$randomString}}\"}",
      "delayMs": 50
    },
    {
      "methodName": "ListUsers",
      "methodType": "SERVER_STREAMING",
      "streamResponses": [
        "{\"id\": \"1\", \"name\": \"User 1\"}",
        "{\"id\": \"2\", \"name\": \"User 2\"}",
        "{\"id\": \"3\", \"name\": \"User 3\"}"
      ]
    }
  ],
  "active": true
}
```

### ISO8583 Financial Message Mocking (jPOS)

Create ISO8583 mock servers for payment system simulation. Uses the industry-standard **jPOS** framework with Q2 hot-deployment.

**Key Features:**
- Multiple mocks share the same port (routed by MTI + field matchers)
- Optional interceptor scripts (GraalVM) for all messages
- Optional custom jPOS XML for advanced users
- Full ISO8583-1987 field support

```bash
POST /api/iso8583/endpoints
Content-Type: application/json

{
  "name": "Payment Switch Simulator",
  "port": 8583,
  "isolatedPort": false,
  
  "mocks": [
    {
      "name": "Visa Authorization",
      "mti": "0100",
      "matchers": {
        "field.2": "^4111.*"
      },
      "responseCode": "00",
      "responseFields": {
        "38": "{{$randomAlphanumeric 6}}",
        "39": "00"
      },
      "priority": 10
    },
    {
      "name": "Mastercard Authorization", 
      "mti": "0100",
      "matchers": {
        "field.2": "^5500.*"
      },
      "responseCode": "00",
      "priority": 10
    },
    {
      "name": "Insufficient Funds",
      "mti": "0100",
      "matchers": {
        "field.4": "^9999.*"
      },
      "responseCode": "51",
      "priority": 20
    },
    {
      "name": "Default Authorization",
      "mti": "0100",
      "responseCode": "00",
      "priority": 0
    },
    {
      "name": "Network Sign-On",
      "mti": "0800",
      "matchers": {
        "field.70": "001"
      },
      "responseCode": "00"
    }
  ],
  
  "interceptorEnabled": false,
  "customXmlEnabled": false,
  "active": true
}
```

**MTI Reference:**
| MTI | Description |
|-----|-------------|
| 0100 | Authorization Request |
| 0110 | Authorization Response |
| 0200 | Financial Request |
| 0210 | Financial Response |
| 0400 | Reversal Request |
| 0410 | Reversal Response |
| 0800 | Network Management Request |
| 0810 | Network Management Response |

**Response Code Reference:**
| Code | Description |
|------|-------------|
| 00 | Approved |
| 05 | Do not honor |
| 14 | Invalid card number |
| 51 | Insufficient funds |
| 54 | Expired card |
| 96 | System malfunction |

---

## Response Template Functions

- `{{$randomInt}}` - Random integer (0-100)
- `{{$randomInt min max}}` - Random integer between min and max
- `{{$randomBool}}` - Random boolean
- `{{$randomString}}` - Random string (10 chars)
- `{{$randomString length}}` - Random string of specified length
- `{{$randomAlphanumeric length}}` - Random alphanumeric string
- `{{$timestamp}}` - Current timestamp in milliseconds
- `{{$randomUUID}}` - Random UUID
- `{{$randomEmail}}` - Random email address
- `{{$randomFloat}}` - Random float (0.0-1.0)

## Scripting (GraalVM Polyglot)

Scripts can access:
- `request` - Request object (method, path, headers, queryParams, pathVariables, body)
- `response` - Response object (status, headers, body)
- `state` - Shared state (persisted in Redis)
- `vars` - Request-scoped variables

**JavaScript Example:**
```javascript
// Pre-script: Validate and set variables
if (request.headers.Authorization) {
    vars.authenticated = true;
    response.status = 200;
} else {
    response.status = 401;
    response.body = '{"error": "Unauthorized"}';
}

// Post-script: Track metrics
state.requestCount = (state.requestCount || 0) + 1;
state.lastRequest = new Date().toISOString();
```

**Python Example:**
```python
# Pre-script
if 'userId' in request['queryParams']:
    vars['userId'] = request['queryParams']['userId']
    response['status'] = 200
else:
    response['status'] = 400

# Post-script
state['requestCount'] = state.get('requestCount', 0) + 1
```

---

## API Reference

### REST Routes
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/routes | List all routes |
| POST | /api/routes | Create route |
| GET | /api/routes/{id} | Get route |
| PUT | /api/routes/{id} | Update route |
| DELETE | /api/routes/{id} | Delete route |
| POST | /api/routes/{id}/activate | Activate route |
| POST | /api/routes/{id}/deactivate | Deactivate route |

### GraphQL Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/graphql/endpoints | List all endpoints |
| POST | /api/graphql/endpoints | Create endpoint |
| POST | /api/graphql/endpoints/{id}/execute | Execute query |
| POST | /api/graphql/endpoints/{id}/activate | Activate |
| POST | /api/graphql/endpoints/{id}/deactivate | Deactivate |

### gRPC Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/grpc/endpoints | List all endpoints |
| POST | /api/grpc/endpoints | Create endpoint |
| POST | /api/grpc/endpoints/{id}/activate | Start gRPC server |
| POST | /api/grpc/endpoints/{id}/deactivate | Stop gRPC server |

### ISO8583 Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/iso8583/endpoints | List all endpoints |
| POST | /api/iso8583/endpoints | Create endpoint |
| POST | /api/iso8583/endpoints/{id}/activate | Start ISO8583 server |
| POST | /api/iso8583/endpoints/{id}/deactivate | Stop ISO8583 server |

### Scenarios (Stateful Mocking)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/scenarios | List all scenarios |
| POST | /api/scenarios | Create scenario |
| POST | /api/scenarios/{name}/transition | Trigger state transition |
| POST | /api/scenarios/{name}/reset | Reset to initial state |

---

## Testing

Run backend tests:
```bash
cd backend
gradle test jacocoTestReport
```

Run frontend tests:
```bash
cd frontend
npm test
```

Coverage reports are generated in `backend/build/reports/jacoco/test/`

---

## License

AGPL-3.0 (due to jPOS dependency for ISO8583 support)

For commercial licensing options, contact the maintainers.

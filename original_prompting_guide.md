# AI Agent Code Prompting Guide - Dynamic Mock API Server Migration

## Overview
This guide provides comprehensive instructions for AI agents/code assistants to help with the Spring Boot 4.0 migration of the Dynamic Mock API Server project.

## Project Context
**Project Name:** Dynamic Mock API Server
**Technology Stack:** Spring Boot 4.0.1, Java 21, Gradle 9.2.1
**Architecture:** Clean Architecture with Hexagonal pattern
**Key Features:**
- Dynamic mock API creation without server restart
- Polyglot scripting (JavaScript, Python via GraalVM)
- Multiple protocols (REST, gRPC, GraphQL, ISO8583)
- Response templating with Handlebars
- Stateful mocking with Redis persistence

## Current Migration Status: ✅ COMPLETED

### Migration Summary
- **From:** Spring Boot 3.3.5
- **To:** Spring Boot 4.0.1
- **Gradle:** 9.2.1 (for Java 25 support)
- **Java:** 21 (configured), 25 (system)
- **Status:** All core functionality working

## AI Agent Instructions

### 1. Problem Recognition
When users report issues, immediately check for:
- Spring Boot version compatibility
- Jackson import/package issues (`com.fasterxml.jackson.*` vs `tools.jackson.*`)
- Dependency conflicts
- Gradle/Java version mismatches
- Auto-configuration exclusions

### 2. Solution Patterns

#### A. Import/Dependency Issues
**Problem Pattern:**
```
The import com.fasterxml cannot be resolved
The import org.junit cannot be resolved
```

**Solution Pattern:**
1. Check `build.gradle` for explicit Jackson dependencies:
```groovy
implementation 'com.fasterxml.jackson.core:jackson-databind'
implementation 'com.fasterxml.jackson.core:jackson-core'
implementation 'com.fasterxml.jackson.core:jackson-annotations'
```

2. Add explicit `ObjectMapper` bean in configuration:
```java
@Bean
@Primary
public ObjectMapper objectMapper() {
    return new ObjectMapper();
}
```

#### B. Auto-Configuration Conflicts
**Problem Pattern:**
```
Error creating bean with name 'graphQlObservationInstrumentation'
NoClassDefFoundError: org/dataloader/instrumentation/DataLoaderInstrumentation
```

**Solution Pattern:**
Exclude problematic auto-configurations:
```java
@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.graphql.observation.GraphQlObservationAutoConfiguration"
})
```

#### C. Gradle/Java Compatibility
**Problem Pattern:**
```
BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_'
Unsupported class file major version 69
```

**Solution Pattern:**
1. Update Gradle wrapper to version 9.x:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2-bin.zip
```

2. Use local Gradle installation:
```bash
E:\gradle-9.2.1\bin\gradle.bat -p backend build
```

#### D. Test Configuration Issues
**Problem Pattern:**
```
package org.springframework.boot.test.autoconfigure.web.servlet does not exist
cannot find symbol @AutoConfigureMockMvc
```

**Solution Pattern:**
1. Add explicit test autoconfigure dependency:
```groovy
testImplementation 'org.springframework.boot:spring-boot-test-autoconfigure'
```

2. Remove `@AutoConfigureMockMvc` annotation
3. Change web environment to `MOCK`:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
```

### 3. Build Commands Reference

#### Primary Build Command (Recommended)
```bash
E:\gradle-9.2.1\bin\gradle.bat -p E:\dynamic-mock-project\backend clean build
```

#### Alternative Commands
```bash
# Using wrapper (may timeout)
cd backend
gradlew.bat clean build

# Specific tasks
gradlew.bat compileJava
gradlew.bat test
gradlew.bat bootRun
```

### 4. Common Error Patterns & Fixes

| Error Pattern | Root Cause | Solution |
|---------------|------------|----------|
| `ObjectMapper` bean not found | Spring Boot 4.0 auto-config issue | Add explicit `@Bean` configuration |
| `DataLoaderInstrumentation` not found | GraphQL observation auto-config | Exclude `GraphQlObservationAutoConfiguration` |
| `Unsupported class file major version` | Gradle/Java version mismatch | Upgrade Gradle to 9.x |
| `@AutoConfigureMockMvc` not found | Spring Boot 4.0 changes | Remove annotation, use `MOCK` environment |
| `com.fasterxml cannot be resolved` | Jackson dependency missing | Add explicit Jackson dependencies |

### 5. Configuration Files Reference

#### build.gradle Key Sections
```groovy
plugins {
    id 'org.springframework.boot' version '4.0.1'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

// Version variables
ext {
    graalvmVersion = '23.1.1'
    // ... other versions
}

// Explicit Jackson dependencies for Spring Boot 4.0
implementation 'com.fasterxml.jackson.core:jackson-databind'
implementation 'com.fasterxml.jackson.core:jackson-core'
implementation 'com.fasterxml.jackson.core:jackson-annotations'

// DataLoader for GraphQL
implementation 'com.graphql-java:java-dataloader:3.2.0'

// Test dependencies
testImplementation 'org.springframework.boot:spring-boot-test-autoconfigure'

// bootRun configuration
tasks.named('bootRun') {
    mainClass.set('com.dynamicmock.DynamicMockApplication')
}
```

#### application.yml Key Sections
```yaml
spring:
  application:
    name: dynamic-mock-server

  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.graphql.observation.GraphQlObservationAutoConfiguration

  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/dynamicmock}
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

server:
  port: ${SERVER_PORT:8080}

# Protocol configurations
graalvm:
  script:
    timeout-seconds: 5
    allowed-languages: js,python

grpc:
  default-port: 9090

graphql:
  path: /graphql
  graphiql:
    enabled: true
```

#### DynamicMockApplication.java
```java
@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.graphql.observation.GraphQlObservationAutoConfiguration"
})
public class DynamicMockApplication {
    public static void main(String[] args) {
        SpringApplication.run(DynamicMockApplication.class, args);
    }
}
```

### 6. Testing Guidelines

#### Integration Tests Configuration
```java
@SpringBootTest(classes = DynamicMockApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=",
    "spring.data.redis.host=",
    "spring.data.redis.port="
})
class IntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
}
```

#### Testcontainers Setup
```java
static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    registry.add("spring.data.redis.host", redisContainer::getHost);
    registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
}
```

### 7. File Structure Reference

```
backend/
├── src/main/java/com/dynamicmock/
│   ├── DynamicMockApplication.java           # Main application class
│   ├── domain/                               # Business entities
│   ├── application/service/                  # Use cases
│   ├── adapter/                              # Interface adapters
│   │   ├── in/web/                          # REST controllers
│   │   └── out/                             # External system adapters
│   └── infrastructure/config/               # Framework configuration
│       ├── WebConfig.java                   # ObjectMapper bean
│       ├── RedisConfig.java                 # Redis template
│       └── MongoConfig.java                 # MongoDB config
├── src/main/resources/
│   └── application.yml                      # Application configuration
├── build.gradle                            # Build configuration
└── gradle.properties                       # Gradle properties
```

### 8. Communication Guidelines

#### Response Format
When helping users, structure responses as:
1. **Immediate Action:** Quick fix or verification
2. **Root Cause:** Explain why the issue occurred
3. **Solution:** Step-by-step resolution
4. **Verification:** How to confirm the fix works
5. **Prevention:** How to avoid similar issues

#### Error Analysis Steps
1. **Read the error message carefully**
2. **Identify the failing component** (bean name, class, etc.)
3. **Check if it's a known Spring Boot 4.0 migration issue**
4. **Verify current configuration** matches the reference
5. **Apply the appropriate solution pattern**

### 9. Quality Assurance Checklist

Before marking an issue as resolved:
- [ ] Code compiles successfully
- [ ] Application starts without errors
- [ ] Core functionality works
- [ ] Tests pass (if applicable)
- [ ] Configuration matches reference patterns

### 10. Reference Links

- [Spring Boot 4.0 Documentation](https://docs.spring.io/spring-boot/documentation.html)
- [Spring Boot Migration Guide](https://docs.spring.io/spring-boot/upgrading.html)
- [Jackson Documentation](https://github.com/FasterXML/jackson)
- [Gradle Documentation](https://docs.gradle.org)

---

## Quick Reference Commands

```bash
# Build and run
E:\gradle-9.2.1\bin\gradle.bat -p E:\dynamic-mock-project\backend clean build
E:\gradle-9.2.1\bin\gradle.bat -p E:\dynamic-mock-project\backend bootRun

# Test compilation
E:\gradle-9.2.1\bin\gradle.bat -p E:\dynamic-mock-project\backend compileJava

# Check dependencies
E:\gradle-9.2.1\bin\gradle.bat -p E:\dynamic-mock-project\backend dependencies

# Clean and rebuild
E:\gradle-9.2.1\bin\gradle.bat -p E:\dynamic-mock-project\backend clean build --refresh-dependencies
```

**Last Updated:** Migration completed successfully
**Spring Boot Version:** 4.0.1
**Status:** All issues resolved, application fully functional

# AI Agent Troubleshooting & Runbook Guide

## Overview
This guide provides comprehensive troubleshooting instructions and configuration patterns for AI agents working on the Dynamic Mock API Server project.

## 1. Problem Recognition
When debugging issues, immediately check for:
- Spring Boot version compatibility
- Jackson import/package issues (`com.fasterxml.jackson.*` vs `tools.jackson.*`)
- Dependency conflicts
- Gradle/Java version mismatches
- Auto-configuration exclusions

## 2. Solution Patterns

### A. Import/Dependency Issues
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

2. Add explicit `ObjectMapper` bean in configuration if missing:
```java
@Bean
@Primary
public ObjectMapper objectMapper() {
    return new ObjectMapper();
}
```

### B. Auto-Configuration Conflicts
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

### C. Gradle/Java Compatibility
**Problem Pattern:**
```
BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_'
Unsupported class file major version
```

**Solution Pattern:**
Update Gradle wrapper to version 9.x:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2-bin.zip
```

### D. Test Configuration Issues
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

## 3. Common Error Patterns & Fixes

| Error Pattern | Root Cause | Solution |
|---------------|------------|----------|
| `ObjectMapper` bean not found | Auto-config issue | Add explicit `@Bean` configuration |
| `DataLoaderInstrumentation` not found | GraphQL observation auto-config | Exclude `GraphQlObservationAutoConfiguration` |
| `Unsupported class file major version` | Gradle/Java version mismatch | Upgrade Gradle to 9.x |
| `@AutoConfigureMockMvc` not found | Spring Boot changes | Remove annotation, use `MOCK` environment |
| `com.fasterxml cannot be resolved` | Jackson dependency missing | Add explicit Jackson dependencies |

## 4. Configuration Files Reference

### build.gradle Key Sections
```groovy
plugins {
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
    id 'java'
}

// Explicit Jackson dependencies
implementation 'com.fasterxml.jackson.core:jackson-databind'
implementation 'com.fasterxml.jackson.core:jackson-core'
implementation 'com.fasterxml.jackson.core:jackson-annotations'

// DataLoader for GraphQL
implementation 'com.graphql-java:java-dataloader:3.2.0'

// Test dependencies
testImplementation 'org.springframework.boot:spring-boot-test-autoconfigure'
```

### application.yml Key Sections
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.graphql.observation.GraphQlObservationAutoConfiguration
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/dynamicmock}
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

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

## 5. Testing Guidelines

### Integration Tests Configuration
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

### Testcontainers Setup
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

## 6. Build Commands Reference (Cross-Platform)

```bash
# Execute from within backend/ directory
./gradlew clean build
./gradlew bootRun
./gradlew compileJava
./gradlew test
```

## 7. Quality Assurance Checklist

Before marking an issue as resolved:
- [ ] Code compiles successfully
- [ ] Application starts without errors
- [ ] Core functionality works
- [ ] Tests pass (if applicable)
- [ ] Configuration matches reference patterns

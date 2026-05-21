# Testing Guide

This document describes how to run tests for the Dynamic Mock API Server.

## Prerequisites

- Docker Desktop installed and running
- Java 21+
- Gradle 8.6+

## Running Tests

### Run All Tests

```bash
cd backend
gradle test
```

### Run Unit Tests Only

```bash
gradle test -Dtest.single="*Test" -DfailIfNoTests=false
```

### Run Integration Tests Only

```bash
gradle test -Dtest.single="*IntegrationTest"
```

### Run Specific Test Class

```bash
gradle test -Dtest.single=RouteRegistryTest
```

### Run with Coverage Report

```bash
gradle clean test jacocoTestReport
```

Coverage report paths:
- Gradle: `build/reports/jacoco/test/html/index.html`

## Test Structure

### Unit Tests

Located in `src/test/java/com/dynamicmock/`:

- **RandomDataFunctionsTest**: Tests for random data generation functions
- **ResponseTemplateEngineTest**: Tests for Handlebars template rendering
- **RouteRegistryTest**: Tests for route registration and matching
- **ScriptEngineTest**: Tests for GraalVM script execution
- **RouteServiceTest**: Tests for route CRUD operations

### Integration Tests

Located in `src/test/java/com/dynamicmock/integration/`:

- **RouteIntegrationTest**: Full flow tests using Testcontainers (MongoDB + Redis)
  - Create, read, update, delete routes
  - Activate/deactivate routes
  - Call mock endpoints
  - Test path variables

- **ScriptExecutionIntegrationTest**: Script execution tests
  - Pre-script execution
  - Post-script execution
  - JavaScript and Python scripts
  - Query parameter handling
  - Delay execution

## Testcontainers

Integration tests use Testcontainers to spin up:
- MongoDB 7.0 container
- Redis 7-alpine container

These containers are automatically started before tests and stopped after tests.

## Coverage Requirements

The project enforces 100% code coverage. The build will fail if coverage is below 100%.

To check coverage:

```bash
gradle clean test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## Troubleshooting

### Docker Not Running

If you see errors about Docker, ensure Docker Desktop is running:

```bash
docker ps
```

### Port Conflicts

If MongoDB or Redis ports are already in use, Testcontainers will automatically use different ports.

### GraalVM Issues

If script execution tests fail, ensure GraalVM polyglot dependencies are properly downloaded:

```bash
gradle dependencies
```

## Continuous Integration

Tests are configured to run in CI/CD pipelines with:
- JaCoCo coverage enforcement (100% threshold)
- Testcontainers for integration tests
- Gradle test task for execution


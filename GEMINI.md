# Project Foundational Mandates - Dynamic Mock API Server

## Environment Configuration
- **Shell**: `/bin/zsh` (macOS default)
- **Build Tool**: Relative `./gradlew` from the `backend/` directory.
- **Java Version**: Java 21+

## Execution Mandates
1. **Always use Wrapper Scripts**: Use `./gradlew` for all Gradle commands. Do not use global `gradle` installations.
2. **Surgical Updates**: Maintain the 100% test coverage requirement. Every code change requires a corresponding test update.
3. **No Hidden Logic**: Avoid using reflection or prototype manipulation. Use explicit composition and type-safe patterns.
4. **Spring Boot 4 Compatibility**: All changes must respect the constraints established during the Spring Boot 4.0.1 migration (e.g., Jackson package structures, MockMvc configuration).

## Path Mappings
- **Root**: Project Root
- **Backend**: `./backend`
- **Frontend**: `./frontend`
- **E2E Tests**: `./e2e`

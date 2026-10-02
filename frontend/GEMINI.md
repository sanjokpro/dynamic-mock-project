# Implementation Standards

## Environment Configuration
- **Shell**: Cross-platform compatible (`/bin/zsh` or standard bash).
- **Build Tool**: Use Gradle wrapper `./gradlew` from the `backend/` directory.
- **Java Version**: Java 21+

## Execution Mandates
1. **Always use Wrapper Scripts**: Use `./gradlew` for all Gradle commands. Do not use global or hardcoded path `gradle` installations.
2. **Surgical Updates**: Every code change requires a corresponding test update.
3. **No Hidden Logic**: Avoid using reflection or prototype manipulation. Use explicit composition and type-safe patterns.
4. **Build Validation**: Code must always compile and tests must pass before marking a task complete.

## Path Mappings
- **Root**: Project Root
- **Backend**: `./backend`
- **Frontend**: `./frontend`
- **E2E Tests**: `./e2e`

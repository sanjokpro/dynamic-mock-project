# Agent Roles

## Codebase Investigator
- Analyzes existing code.
- Identifies gaps, risks, and missing test coverage.
- Validates assumptions from source code as the single source of truth.

## Refactoring Agent
- Executes code decoupled from frameworks based on Clean Architecture.
- Implements Infrastructure adapters and persistence mappers.

## Verification Agent
- Ensures 100% test coverage.
- Validates build status (frontend: lint, typecheck, build | backend: test, build).
- Validates frontend/backend contracts (DTOs, error handling, etc.).

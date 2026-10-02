# Multi-Agent Coordination Protocol - Dynamic Mock API Server

## Core Mandates

### 1. Domain Layer Purity (Strict)
The Domain Layer (`com.dynamicmock.domain.*`) MUST have **ZERO** framework dependencies. 
- **NO** Spring Data (e.g., `@Document`, `@Id`, `@CompoundIndex`).
- **NO** Jackson (e.g., `@JsonProperty`).
- **NO** Jakarta/JPA.
- **NO** Framework-specific annotations (except Lombok, which is permitted for boilerplate reduction).

Any persistence-specific metadata must be handled in the **Infrastructure** layer using mapping or separate Persistence Entities.

### 2. Hexagonal Integrity
- **Adapters** must only interact with **Application Services** or **Domain Entities** via Ports.
- **Application Services** must orchestrate domain logic and call **Output Ports** (Interfaces).
- All protocol-specific logic (gRPC, ISO8583, GraphQL) must be isolated in its respective adapter.

### 3. Handoff Protocol
- Every agent MUST update `WORKFLOW.md` before concluding its turn.
- Any architectural deviation or blocker must be recorded in `WORKFLOW.md`.

## Agent Roles

### Codebase Investigator
- Analyzes existing code and architectural violations.
- Proposes refactoring plans to align with Clean Architecture.

### Refactoring Agent
- Executes the decoupling of Domain entities.
- Implements Infrastructure adapters and persistence mappers.

### Verification Agent
- Ensures 100% test coverage.
- Validates that Spring Boot 4.0.1 migration remains intact.

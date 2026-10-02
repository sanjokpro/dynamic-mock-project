# AI Governance Audit

This document presents an audit of the current AI-facing documentation, rules, and workflows across the Dynamic Mock API Server project.

## 1. Existing AI Instructions
- **Agent Roles (`frontend/AGENTS.md`)**: Defines specific personas: Codebase Investigator, Refactoring Agent, and Verification Agent.
- **Handoff Protocol (`frontend/AGENTS.md`, `frontend/WORKFLOW.md`)**: Mandates that every agent must update `WORKFLOW.md` before concluding their turn.
- **Prompting Guide (`AI_AGENT_PROMPTING_GUIDE.md`)**: Provides structured troubleshooting (Problem Recognition, Solution Patterns), build reference commands, and communication guidelines for agent responses.
- **Execution Mandates (`frontend/GEMINI.md`)**: Imposes rules such as "Always use Wrapper Scripts", "Surgical Updates" (100% test coverage), and "No Hidden Logic".

## 2. Existing Development Process
- **State Tracking (`frontend/WORKFLOW.md`)**: Acts as the system state tracker, capturing the current phase, accomplishments, task backlog, and known anomalies/blockers.
- **Build & Execution (`AI_AGENT_PROMPTING_GUIDE.md`, `frontend/GEMINI.md`)**: Outlines the build commands required to verify the application, along with a Quality Assurance Checklist.
- **Test-Driven Changes (`frontend/GEMINI.md`)**: Requires every code change to have a corresponding test update.

## 3. Existing Coding Conventions
- **Domain Layer Purity (`frontend/AGENTS.md`)**: Zero framework dependencies in the domain layer (No Spring Data, No Jackson, No Jakarta/JPA, except Lombok).
- **Explicit Logic (`frontend/GEMINI.md`)**: Avoid reflection or prototype manipulation; use explicit composition and type-safe patterns.
- **Test Configurations (`AI_AGENT_PROMPTING_GUIDE.md`)**: Defines patterns for Testcontainers and `@SpringBootTest` environments (`webEnvironment = SpringBootTest.WebEnvironment.MOCK`).

## 4. Existing Architecture Rules
- **Hexagonal Architecture (`frontend/AGENTS.md`, `AI_AGENT_PROMPTING_GUIDE.md`)**: Strict isolation between Adapters, Application Services, and Domain Entities.
- **Protocol Isolation (`frontend/AGENTS.md`)**: All protocol-specific logic (gRPC, ISO8583, GraphQL) must be isolated in respective adapters.
- **Persistence Mapping (`frontend/AGENTS.md`, `frontend/WORKFLOW.md`)**: Persistence-specific metadata must be handled in the Infrastructure layer using mappers.

## 5. Duplicate Guidance
- **Spring Boot 4 Constraint**: Both `frontend/GEMINI.md` and `AI_AGENT_PROMPTING_GUIDE.md` emphasize Spring Boot 4.0.1 constraints and compatibility.
- **Test Coverage**: Both `frontend/GEMINI.md` and `frontend/AGENTS.md` mandate 100% test coverage.
- **State Tracking Context**: Both `frontend/WORKFLOW.md` and `AI_AGENT_PROMPTING_GUIDE.md` independently track and report the Spring Boot 4.0.1 migration status as "COMPLETED".

## 6. Conflicting Guidance
- **Operating System & Build Commands**:
  - `frontend/GEMINI.md` mandates a macOS/Linux environment (`/bin/zsh`) and relative wrapper scripts (`./gradlew`).
  - `AI_AGENT_PROMPTING_GUIDE.md` provides hardcoded Windows absolute paths as the primary build command (e.g., `E:\gradle-9.2.1\bin\gradle.bat`).
- **Build Directory Flag**:
  - `frontend/WORKFLOW.md` states an anomaly where `backend/build` is root-owned and requires the flag `-Dorg.gradle.project.buildDir=new_build`.
  - `AI_AGENT_PROMPTING_GUIDE.md` build commands omit this required flag, which would lead to permission denied errors.
- **Java Version Details**:
  - `frontend/GEMINI.md` specifies "Java 21+".
  - `AI_AGENT_PROMPTING_GUIDE.md` states "Java: 21 (configured), 25 (system)", introducing potential ambiguity about the target runtime.

## 7. Recommended Unified Governance Model

To streamline agent operations and resolve contradictions, the following unified model is recommended:

1. **Single Source of Truth for Architecture & Rules**: Consolidate `frontend/AGENTS.md` and `frontend/GEMINI.md` into a standardized `.agents/rules/` directory (or a single `AI_GOVERNANCE.md` in the root) to prevent fragmentation.
2. **Dynamic Workflow Tracking**: Confine all state, progress, and anomalies strictly to `WORKFLOW.md`. Remove static status indicators (like "Migration Status: COMPLETED") from instructional guides.
3. **Platform-Agnostic Instructions**: Standardize on cross-platform Gradle wrapper commands (`./gradlew`) in all documentation. Remove hardcoded Windows paths to ensure compatibility with the documented macOS/zsh environment.
4. **Centralized Runbooks**: Maintain `AI_AGENT_PROMPTING_GUIDE.md` purely as a troubleshooting and execution runbook, abstracting away current state and focusing strictly on error resolution and QA checklists. Ensure required build flags (e.g., custom build directories) are consistently documented.

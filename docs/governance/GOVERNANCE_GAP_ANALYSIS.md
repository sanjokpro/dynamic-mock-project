# Governance Gap Analysis

## Current State
- `frontend/AGENTS.md` defines specific agent roles and architectural guidelines like Clean Architecture and Domain Layer Purity.
- `AI_AGENT_PROMPTING_GUIDE.md` provides troubleshooting instructions, error resolutions, and some build commands (focused around a completed migration).
- `frontend/GEMINI.md` provides base execution mandates, build wrapper rules, and some architectural guidelines (no hidden logic).
- `frontend/WORKFLOW.md` acts as a project state tracker, though includes older migration status.
- `README.md` documents general project architecture, setup, and protocol support.

## Duplicates
- **Architecture**: `AGENTS.md` and `README.md` both outline Clean Architecture constraints.
- **Build Guidance**: Build commands and wrapper usage are duplicated across `GEMINI.md`, `AI_AGENT_PROMPTING_GUIDE.md`, and `README.md`.
- **State Tracking**: `WORKFLOW.md` and `AI_AGENT_PROMPTING_GUIDE.md` both contain state indicators (Migration Status: COMPLETED).

## Conflicts
- **Build Commands**: `GEMINI.md` and `README.md` use `./gradlew` and standard Linux/macOS environments, while `AI_AGENT_PROMPTING_GUIDE.md` hardcodes absolute Windows paths (`E:\gradle-9.2.1\bin\gradle.bat`).
- **Java Version**: `GEMINI.md` and `README.md` state Java 21+, while `AI_AGENT_PROMPTING_GUIDE.md` references Java 25 as system default.

## Missing Rules
- Formalized discovery phase prior to making changes.
- Pre-coding finding reports.
- Priority framework (Fix Before Feature).
- Frontend completion policy and Frontend-Backend contract validation.
- Enterprise vision alignment.
- Clear documentation ownership rules.

## Recommendations
- Create `AI_GOVERNANCE.md` as the supreme constitution for all agents.
- Clean up duplicate architectural and governance instructions from existing files.
- Refocus `AGENTS.md` strictly on agent roles.
- Refocus `GEMINI.md` strictly on implementation standards.
- Refocus `AI_AGENT_PROMPTING_GUIDE.md` purely on troubleshooting runbooks and error codes.
- Refocus `WORKFLOW.md` strictly on state tracking.
- Standardize cross-platform build commands and remove legacy migration references.

# Unified AI Governance and Engineering Constitution

This is the primary governance document for all AI agents operating on the Dynamic Mock API Server project.

## MISSION
Transform Dynamic Mock Project into a self-hosted, open-source, enterprise-grade, white-label integration simulation platform.

## SOURCE OF TRUTH
**Source code is the source of truth.**
README, comments, prompts, workflow documents, and older documentation may be outdated. Agents must verify all assumptions from code.

## MANDATORY DISCOVERY PHASE
Before implementing any change, agents must:
1. Analyze related code.
2. Analyze related tests.
3. Analyze frontend/backend contracts.
4. Verify current behavior.
5. Identify gaps.
6. Document findings.

**Implementation without discovery is prohibited.**

## FINDINGS REPORT REQUIREMENT
Before coding, agents must produce a report containing:
- Current State
- Expected State
- Gap Analysis
- Risks
- Recommendation
- Alternative Solutions

## FIX BEFORE FEATURE
Priority order for changes:
1. Broken functionality
2. Partially implemented functionality
3. Missing functionality
4. New functionality

**Never prioritize new features over broken features.**

## FRONTEND COMPLETION POLICY
Current project priority for UI development:
1. Route Management
2. Collection Management
3. Environment Management
4. Traffic Console
5. Scenario Builder
6. GraphQL Designer
7. gRPC Designer
8. ISO8583 Designer

No major feature expansion is allowed until these are functional.

## FRONTEND-BACKEND CONTRACT VALIDATION
Every UI screen must verify:
- Endpoint exists
- DTO exists
- Mapping exists
- Validation exists
- Error handling exists
- Loading state exists

## DOCUMENTATION OWNERSHIP
Every functional change must update:
- `README.md`
- Architecture documentation
- Changelog

A feature is considered incomplete if its documentation is outdated.

## BUILD VALIDATION
The repository must remain buildable at all times.
Agents must verify:
- **Frontend**: lint, typecheck, build
- **Backend**: test, build

## ENTERPRISE VISION
The future roadmap includes the following enterprise capabilities:
- White Labeling
- RBAC
- Audit Trail
- LDAP / OIDC / SAML
- Multi-Tenancy
- Git Sync
- Backup / Restore

## Sprint Closure Rule

A sprint is not complete when code is merged.

A sprint is complete only when:

1. Build succeeds.
2. Tests pass.
3. Documentation updated.
4. Screenshots updated.
5. User workflow validated manually.

## Differentiation Rule

When choosing between two tasks,
prioritize the task that increases product differentiation.

Examples:

High Priority:
- ISO8583
- Stateful Scenarios
- Enterprise Self Hosting
- White Labeling
- Protocol Simulation

Lower Priority:
- Generic REST Mocking Enhancements
- Cosmetic CRUD Improvements
- Features already common in competing tools

All five are mandatory.

## Productization Rule

If backend capability exists but is not accessible
through a discoverable UI workflow,
the feature is considered incomplete.

Backend implementation alone does not
constitute product completion.

## Roadmap Governance

Strategic authority documents:

- MASTER_PRODUCT_ACTION_PLAN.md
- MASTER_PRODUCT_ACTION_PLAN_REVIEW.md

All implementation work must align with these documents.

Any deviation requires:

- documented rationale
- business justification
- architectural review

Roadmap priorities cannot be overridden by feature curiosity.

## Analysis Debt Rule

No new analysis document may be created if:

- a roadmap already exists
- a sprint has not yet been completed

Exception:

A discovery phase explicitly required by the current sprint.

## Roadmap Freeze Rule

MASTER_PRODUCT_ACTION_PLAN.md
and
MASTER_PRODUCT_ACTION_PLAN_REVIEW.md

are the governing roadmap documents.

No new feature may be promoted ahead of roadmap priorities
without documented business justification.

Feature requests do not override strategic priorities.

## Feature Completion Rule

A feature is not complete if:

- It uses native browser prompts.
- It has no validation.
- It has no loading state.
- It has no error handling.
- It has no tests.

Functional existence is not feature completion.
Production usability is feature completion.
## Product Positioning Rule

Before implementing a feature ask:

"Does this feature strengthen our unique value proposition?"

Priority must always favor:

- Stateful simulation
- Protocol simulation
- ISO8583
- Enterprise self-hosting
- White labeling
- Data sovereignty

over generic REST mocking features.
## INCORPORATED ARCHITECTURE STANDARDS

- **Domain Layer Purity (Strict)**: The Domain Layer (`com.dynamicmock.domain.*`) MUST have ZERO framework dependencies.
  - NO Spring Data (e.g., `@Document`, `@Id`, `@CompoundIndex`).
  - NO Jackson (e.g., `@JsonProperty`).
  - NO Jakarta/JPA.
  - NO Framework-specific annotations (except Lombok, which is permitted for boilerplate reduction).
- **Hexagonal Integrity**: Adapters must only interact with Application Services or Domain Entities via Ports. Application Services must orchestrate domain logic and call Output Ports (Interfaces).
- **Protocol Isolation**: All protocol-specific logic (gRPC, ISO8583, GraphQL) must be isolated in its respective adapter.
- **Persistence Mapping Rules**: Any persistence-specific metadata must be handled in the Infrastructure layer using mapping or separate Persistence Entities.
- **Explicit Logic**: No hidden logic, reflection, or prototype manipulation. Use explicit composition and type-safe patterns.
- **Test-Driven Surgical Updates**: Maintain 100% test coverage. Every code change requires a corresponding test update.

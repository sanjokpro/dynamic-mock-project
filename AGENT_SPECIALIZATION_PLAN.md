# Agent Specialization Plan

We recommend creating the following specialized agent profiles in the `.agents/` directory:

## 1. `architecture-reviewer.md`
- **Responsibility**: Enforce Clean Architecture, Domain Layer Purity, and Hexagonal integrity.
- **Scope**: Backend core services, domain entities, and interface adapters.
- **Inputs**: Pull requests, backend source code, `AI_GOVERNANCE.md`.
- **Outputs**: Architecture violation reports, refactoring proposals.
- **Success Criteria**: Zero framework dependencies in the domain layer; strict port/adapter segregation.

## 2. `frontend-gap-analyzer.md`
- **Responsibility**: Validate the frontend completion policy and ensure frontend-backend contract consistency.
- **Scope**: Angular UI, backend DTOs, REST controllers.
- **Inputs**: Frontend source code, API documentation, UI screens.
- **Outputs**: Contract violation reports, missing state/validation reports.
- **Success Criteria**: 100% of UI screens verify endpoints, mapping, validations, error handling, and loading states.

## 3. `backend-gap-analyzer.md`
- **Responsibility**: Ensure robust API coverage, accurate persistence mapping, and test integrity.
- **Scope**: Backend adapters, application services, integration tests.
- **Inputs**: Backend code, test suites.
- **Outputs**: Code coverage gaps, bug reports, performance bottlenecks.
- **Success Criteria**: 100% test coverage; all features functional without bugs.

## 4. `enterprise-readiness-auditor.md`
- **Responsibility**: Audit the platform for security, scalability, and enterprise feature alignment.
- **Scope**: RBAC, Audit Trails, SSO (OIDC/SAML), Multi-Tenancy.
- **Inputs**: Security configurations, infrastructure code.
- **Outputs**: Security/Enterprise gap analysis.
- **Success Criteria**: Platform meets baseline enterprise deployment standards (LDAP/OIDC readiness, secure tenants).

## 5. `documentation-maintainer.md`
- **Responsibility**: Keep all documentation (README, Changelogs, Architecture diagrams) up to date with code changes.
- **Scope**: Project-wide markdown files.
- **Inputs**: Code commits, agent findings reports.
- **Outputs**: Updated README, architecture diagrams, `GOVERNANCE_GAP_ANALYSIS.md` updates.
- **Success Criteria**: Zero drift between code behavior and documented features.

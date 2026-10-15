# Sprint 4 Discovery: Enterprise Authentication & Multi-User Foundation

**Document Version**: 1.0  
**Date**: 2026-10-14  
**Author**: Antigravity Principal Product Architect & Governance Specialist  
**Status**: DISCOVERY COMPLETE — RECOMMENDATION SUBMITTED  
**Governing Context**: `AI_GOVERNANCE.md`, `ROADMAP_AUTHORITY.md`, `MASTER_PRODUCT_ACTION_PLAN.md`, `MASTER_PRODUCT_ACTION_PLAN_REVIEW.md`

---

## Executive Summary

Over Sprints 1, 2, and 3, Dynamic Mock achieved exceptional momentum, delivering three successful releases:
- **v0.1.0 (Sprint 1)**: ISO8583 Productization Foundation (Data Dictionary, Custom Packager Upload, CI Pipeline, Licensing).
- **v0.2.0 (Sprint 2)**: ISO8583 Simulation Experience (Message Simulator, Scenario State Inspector, Shared Packager Factory).
- **v0.3.0 (Sprint 3)**: Reference Banking Sandbox (1-Click Sandbox Loader, Card Payment Lifecycle, Banking Tutorial, Repositioned Platform Identity).

These three sprints established a formidable **functional simulation moat**: a developer or financial systems engineer can run the platform, load a complete ISO8583 banking sandbox, and simulate a full `Auth (0100) → Capture (0220) → Reversal (0420) → Auto-Reset` payment lifecycle in under five minutes.

**However, the platform now faces a hard reality:**
Dynamic Mock has reached the exact boundary between a **local developer prototype** and a **deployable enterprise platform**. Today, the product has zero user authentication, a hardcoded development API key (`default-dev-key`), a hardcoded user identity (`default-user`), and globally shared scenario state in Redis (`scenario:state:*` and `dynamic-mock:script-state:global`).

If a bank, fintech, or payment switch evaluates Dynamic Mock today, they will be wowed by the Banking Sandbox within 3 minutes — and then **refuse to install it on their shared servers, VPNs, or cloud environments** because anyone on the network can access the management API, delete mocks, or overwrite colleagues' scenario states.

This discovery document evaluates 8 candidate options, answers the 5 critical strategic questions, and provides an authoritative justification for **Sprint 4: Authentication Foundation & Multi-User Identity**.

---

## 1. Current Product State

### 1.1 Capabilities Inventory (v0.1.0 – v0.3.0)

| Functional Area | Current Status | Architecture / Implementation |
|---|---|---|
| **ISO8583 Engine** | **Production-Grade** | Powered by jPOS Q2, isolated port adapter, binary & ASCII bitmap parsing, custom XML packager upload with hot-reload and fallback. |
| **ISO8583 UX** | **Production-Grade** | Complete 128-field ISO 8583:1987 Data Dictionary, field search & autocomplete, built-in Message Simulator with TCP proxy, MTI templates (`0100`, `0200`, `0420`), response viewer with Field 39 interpretation, and raw hex toggles. |
| **Stateful Scenarios** | **Production-Grade** | Clean Architecture `ScenarioService`, Redis-backed state machine, execution counters, script variables, dynamic transition condition evaluation (`request.mti == '...'`), auto-reset on terminal states, live Scenario State Inspector in UI. |
| **Banking Sandbox** | **Production-Grade** | 1-click idempotent bundle loader (`banking-sandbox.json`), pre-configured endpoint and `Card Payment Flow`, step-by-step tutorial (`BANKING_SANDBOX_TUTORIAL.md`), validated end-to-end. |
| **Multi-Protocol Mocking** | **Operational** | REST (`/mock/**`), GraphQL (`/api/graphql`), gRPC (`io.grpc`), ISO8583 (TCP `8583`). |
| **Frontend Platform** | **Operational** | Next.js App Router, React, TypeScript, Tailwind CSS, TanStack React Query, Dark/Light modes. |
| **Backend Architecture** | **High Integrity** | Strict Clean / Hexagonal Architecture, framework-pure domain entities, Spring Boot 4, MongoDB persistence mapping, Redis cache. |

### 1.2 Current Gaps & Technical Debt

Despite the strong simulation engine, the following systemic limitations exist in the codebase today:
1. **No Authentication Wall**: Navigating to `http://localhost:3000` immediately opens the workspace with full administrative privileges. There is no login, registration, or session validation.
2. **Hardcoded API Key**: Backend `ApiKeyAuthFilter` checks a single static token from `${app.api.key:default-dev-key}`, which is publicly committed and baked into the frontend `.env.example` as `NEXT_PUBLIC_API_KEY`.
3. **Hardcoded User Identity**: The string `"default-user"` is hardcoded throughout the codebase:
   - `frontend/src/components/workspace/Header.tsx` (line 32)
   - `frontend/src/components/workspace/Sidebar.tsx` (line 25)
   - `backend/src/main/java/com/dynamicmock/infrastructure/config/DemoDataLoader.java` (lines 55, 56, 64)
   - `backend/src/main/java/com/dynamicmock/application/service/PostmanImportService.java` (line 42)
4. **Global Scenario State Collisions**: In `ScenarioService.java`:
   - Current state is stored as `scenario:state:<scenarioName>`.
   - Script variables are stored in a single global Redis hash: `dynamic-mock:script-state:global`.
   - Execution count is stored as `scenario:exec:<scenarioName>`.
   - *Impact*: If two developers or automated test suites execute transactions against the same scenario concurrently, their state machines and variables overwrite each other in real time.
5. **Zero Tenant or User Partitioning**: Collections, routes, and environments belong to `"default-user"`, meaning there is no workspace isolation between team members.

---

## 2. Enterprise Readiness Assessment

To determine readiness for enterprise pilots at financial institutions, we assess the product across four enterprise pillars:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   ENTERPRISE READINESS ASSESSMENT                      │
├────────────────────────┬───────────────┬───────────────────────────────┤
│ Pillar                 │ Current Score │ Status & Core Deficiencies    │
├────────────────────────┼───────────────┼───────────────────────────────┤
│ 1. Security Posture    │ 15 / 100      │ ❌ CRITICAL BLOCKER            │
│                        │               │ • No login or credentials     │
│                        │               │ • Shared default API key      │
│                        │               │ • No token expiration/revoc.  │
├────────────────────────┼───────────────┼───────────────────────────────┤
│ 2. Multi-User/Team     │ 20 / 100      │ ❌ SEVERE LIMITATION          │
│                        │               │ • Single user hardcoding      │
│                        │               │ • Global Redis state keys     │
│                        │               │ • Concurrency race conditions │
├────────────────────────┼───────────────┼───────────────────────────────┤
│ 3. Operational & Infra │ 65 / 100      │ ⚠️ MODERATE                   │
│                        │               │ • Docker Compose functional   │
│                        │               │ • MongoDB + Redis verified    │
│                        │               │ • Missing production secrets  │
├────────────────────────┼───────────────┼───────────────────────────────┤
│ 4. Compliance & Audit  │ 25 / 100      │ ⚠️ DEFERRED                   │
│                        │               │ • Data sovereignty preserved  │
│                        │               │ • Apache 2.0 / jPOS legal OK  │
│                        │               │ • No audit trail of mutations │
└────────────────────────┴───────────────┴───────────────────────────────┘
```

### 2.1 Security Posture Analysis
- **Finding**: In any regulated financial institution (bank, payment switch, fintech), security review is the first gate before deployment. The lack of an authentication wall is a **complete blocker**. Security teams will flag that unauthenticated actors on the internal corporate network can arbitrarily reconfigure payment mocks, reset state machines, or run GraalVM scripts.
- **Verdict**: Immediate intervention required.

### 2.2 Multi-User & Concurrency Analysis
- **Finding**: A bank does not run a mock server for a single isolated developer; they run it as a shared simulation environment for QA squads, integration engineers, and automated CI pipelines.
- **Finding**: Because scenario state in Redis is not namespaced, two simultaneous tests will interfere with each other. If QA Engineer A sends an Authorization (`0100`) and QA Engineer B simultaneously sends a Capture (`0220`), the state machine will process transitions against the exact same scenario instance.
- **Verdict**: User identity and state namespacing must be addressed together.

---

## 3. Comparison of Sprint 4 Candidate Options

We evaluate the eight candidate options against seven standardized criteria:
1. **User Value**: Direct benefit to the developer/tester.
2. **Enterprise Value**: Unblocking pilot deployments, enterprise sales, or compliance.
3. **Differentiation Value**: Strengthening our unique moat against generic competitors (WireMock, Mockoon, Postman).
4. **Complexity**: Implementation effort, architectural footprint, and risk of bugs.
5. **Risk**: Probability of regressions, security holes, or scope creep.
6. **Dependencies**: Prerequisite components required before execution.
7. **Recommended Priority**: Actionable priority classification.

---

### Candidate 1: Authentication Foundation (JWT & User Identity)
- **Scope**: Spring Security integration, domain-pure `User` entity, BCrypt password hashing, stateless JWT issue/validate, `/api/auth/login`, `/api/auth/register`, `/api/auth/me`, Next.js login/registration UI, Next.js auth guard/middleware, and replacing `"default-user"` with authenticated user IDs.
- **User Value**: Medium (personal workspaces, secure logins).
- **Enterprise Value**: **Critical (Maximum)**. Solves the #1 enterprise rejection reason. Allows secure self-hosted deployment on corporate networks and private clouds.
- **Differentiation Value**: Medium (foundational hygiene; prerequisite for all enterprise features).
- **Complexity**: Medium. Spring Security filter chain configuration, JWT token parsing, and Next.js session provider.
- **Risk**: Low-Medium (must ensure `/mock/**` and public endpoints remain unblocked for external simulator/traffic calls).
- **Dependencies**: None.
- **Recommended Priority**: **P0 — RECOMMENDED FOR SPRINT 4**.

---

### Candidate 2: Role-Based Access Control (RBAC)
- **Scope**: Multi-tier role permissions (`ADMIN`, `OPERATOR`, `VIEWER`), resource-level authorization checks (`@PreAuthorize`), permission matrices, UI disabled states for viewers.
- **User Value**: Medium.
- **Enterprise Value**: High for large organizations, but not needed for initial 5-to-10-person pilot teams.
- **Differentiation Value**: Low (commodity capability).
- **Complexity**: High. Requires permission schemas, database migrations, endpoint annotations, and extensive UI conditional rendering.
- **Risk**: High (building authorization before having established authentication leads to artificial mock-user abstractions and throwaway code).
- **Dependencies**: Authentication Foundation (Candidate 1).
- **Recommended Priority**: **DEFER to Sprint 6**.

---

### Candidate 3: LDAP / OIDC / SAML Preparation
- **Scope**: Corporate Single Sign-On (SSO) integration via Active Directory, Keycloak, Okta, or Azure AD.
- **User Value**: Low for local dev; High for enterprise IT administrators.
- **Enterprise Value**: Very High for company-wide rollouts, but overkill for early POC pilots.
- **Differentiation Value**: Low.
- **Complexity**: Very High (redirect callbacks, metadata parsing, SSL truststores, external IdP mock testing).
- **Risk**: High (cannot be easily validated in local open-source testing without simulated directory services).
- **Dependencies**: Authentication Foundation (Candidate 1).
- **Recommended Priority**: **DEFER to Sprint 7+**. (Ensure Sprint 4 uses standard Spring Security contracts so an `OidcUser` / `OAuth2Login` adapter can plug in seamlessly later).

---

### Candidate 4: Generic API Enhancements
- **Scope**: Nested collections, gRPC service browser, GraphQL schema explorer, bulk route cloning.
- **User Value**: Low to Medium.
- **Enterprise Value**: Very Low.
- **Differentiation Value**: **Negative / Counter-Strategic**. Violates `AI_GOVERNANCE.md` Differentiation Rule: competing tools (Postman, WireMock) have hundreds of engineers working on generic REST/GraphQL tooling. Chasing parity wastes precious sprint capacity.
- **Complexity**: Medium.
- **Risk**: Low technically, but extreme strategic opportunity cost.
- **Dependencies**: None.
- **Recommended Priority**: **REJECT / DEFER to P3**.

---

### Candidate 5: White Label Foundation
- **Scope**: Backend `BrandingController`, Organization branding settings (logo URL, product title, custom primary/accent hex colors), frontend `BrandingProvider`, dynamic CSS variable injection, dynamic favicon, environment variable overrides (`WHITE_LABEL_APP_NAME`, `WHITE_LABEL_LOGO_URL`).
- **User Value**: High for system integrators, OEM partners, and payment consultancies.
- **Enterprise Value**: High (enables internal bank developer portals to rebrand the tool as "BankX Switch Simulator").
- **Differentiation Value**: High (unique among open-source mock servers).
- **Complexity**: Medium (clean, well-understood UI CSS injection + simple config entity).
- **Risk**: Low.
- **Dependencies**: Benefits greatly from being anchored to an authenticated administrative context.
- **Recommended Priority**: **P1 — RECOMMENDED FOR SPRINT 5**.

---

### Candidate 6: Scenario State Namespacing (Session / Multi-User Isolation)
- **Scope**: Partitioning Redis scenario state keys by session ID, correlation token, PAN prefix, or user workspace:
  - `scenario:state:<namespace>:<scenarioName>`
  - `scenario:exec:<namespace>:<scenarioName>`
  - `dynamic-mock:script-state:<namespace>`
  Includes namespace resolution in `ScenarioService` and `Iso8583Server`.
- **User Value**: **Very High**. Allows multiple QA testers and automated CI jobs to test the same Banking Sandbox flow simultaneously without colliding.
- **Enterprise Value**: **Very High**. Vital for multi-tester bank environments and concurrent load/regression testing.
- **Differentiation Value**: **Extremely High**. Solidifies Dynamic Mock's position as a true multi-tenant simulation platform rather than a single-threaded mock.
- **Complexity**: Low-Medium (parameterizing existing key generation in `ScenarioService` with a fallback to `"default"`).
- **Risk**: Low (backward compatible with existing sandbox tests).
- **Dependencies**: Naturally pairs with or follows User Identity.
- **Recommended Priority**: **P0 (Deliver as a core architectural enhancement within Sprint 4)**.

---

### Candidate 7: Open Source Adoption Improvements
- **Scope**: `CONTRIBUTING.md`, Helm chart for Kubernetes deployment, developer onboarding scripts, GIF demo recordings, issue templates.
- **User Value**: Medium.
- **Enterprise Value**: Medium (Helm chart simplifies DevOps).
- **Differentiation Value**: Low.
- **Complexity**: Low to Medium.
- **Risk**: Low.
- **Dependencies**: Stable deployment model.
- **Recommended Priority**: **P2 — Continuous improvement / Sprint 6+**.

---

### Candidate 8: Other Remaining Roadmap Items
- **ISO8583 Bitmap Grid & Hex Trace Viewer**: Secondary visual enhancements; the Simulator already provides parsed field tables and raw hex toggle.
- **Audit Trail & Git Sync**: Enterprise governance features that belong in post-pilot compliance hardening.
- **Recommended Priority**: **DEFER**.

---

### Summary Comparison Table

| Candidate | User Value | Enterprise Value | Differentiation | Complexity | Risk | Recommended Priority |
|---|---|---|---|---|---|---|
| **1. Authentication Foundation** | Medium | **Critical** | Medium | Medium | Low-Med | **P0 (Sprint 4)** |
| **6. Scenario State Namespacing** | **Very High** | **Very High** | **Very High** | Low-Med | Low | **P0 (Sprint 4 Architecture)** |
| **5. White Label Foundation** | High | High | High | Medium | Low | **P1 (Sprint 5)** |
| **2. RBAC** | Medium | High | Low | High | High | **P1 (Sprint 6)** |
| **3. LDAP / OIDC Preparation** | Low | High | Low | Very High | High | **P2 (Sprint 7+)** |
| **7. Open Source Adoption / Helm** | Medium | Medium | Low | Low-Med | Low | **P2 (Sprint 6+)** |
| **4. Generic API Enhancements** | Low | Very Low | None | Medium | High (Opp. Cost) | **REJECT / DEFER** |
| **8. Audit Trail / Git Sync** | Low | High | Medium | High | Med | **P3 (Future)** |

---

## 4. Critical Questions Analysis

### Q1: What is the biggest blocker preventing enterprise pilots today?
**Answer: Zero Authentication and Insecure Access Control.**
Even though the Reference Banking Sandbox is a stellar demonstration of ISO8583 simulation, no bank or financial institution's IT security or infrastructure team will authorize deployment on a shared network without an authentication wall.
- There are no user credentials.
- All administrative routes (`/api/**`) rely on a single, publicly visible development key (`default-dev-key`).
- Anyone with network access can delete endpoints, modify mocks, view transaction payloads, or wipe out scenario states.
- Authentication is the non-negotiable **"front door"** required before any real enterprise pilot can begin.

### Q2: What would a bank, fintech, or payment switch operator expect next after seeing the Banking Sandbox?
**Answer: The transition from a "local single-user demo" to a "shared, secure team platform."**
Once an evaluator confirms that Dynamic Mock can accurately simulate an ISO8583 card lifecycle (`0100 Auth → 0220 Capture → 0420 Reversal`), their immediate operational questions are:
1. *"How do my QA team and integration partners log in securely without sharing a single password or seeing each other's uncommitted mocks?"* (User Identity).
2. *"Can Tester A and Tester B run the Card Payment Flow simultaneously without their transaction amounts and state machine counters interfering?"* (Scenario State Namespacing).
3. *"Can we white-label this with our bank's logo for our third-party fintech partners?"* (White Labeling).

### Q3: What is the single highest-value Sprint 4?
**Answer: Sprint 4 — Authentication Foundation & Multi-User Identity.**
This delivers the security foundation that unblocks enterprise pilots, eliminates hardcoded `"default-user"` references, secures the API with JWT tokens, and introduces scenario state namespacing so multiple testers can utilize the Banking Sandbox concurrently.

### Q4: Should Authentication and RBAC be combined or separated?
**Answer: SEPARATED.**
Combining Authentication and RBAC into a single sprint creates excessive scope and risks a flawed implementation.
- **Authentication** answers *"Who are you?"* (User entity, password hashing, JWT issue/verify, login page, auth guard/middleware, session context, replacing `default-user`).
- **RBAC** answers *"What are you allowed to do?"* (Roles, permission matrices, route guards, method security, UI permission rendering).
- If combined, the sprint gets bogged down in permission edge cases (can a viewer run the simulator? can an editor change packagers?).
- By separating them:
  - **Sprint 4** delivers a rock-solid **Authentication Foundation**.
  - **Sprint 5 / 6** can cleanly layer **RBAC** (Admin vs Operator vs Viewer) on top of verified identities.

### Q5: What should be deferred until later?
**Answer:**
1. **LDAP / OIDC / SAML**: Defer to Sprint 7+. Corporate SSO is a deployment integration, not a core product prerequisite.
2. **Full-Blown RBAC**: Defer to Sprint 6.
3. **Multi-Tenancy (Isolated database schemas)**: Defer until multiple enterprises share a managed instance.
4. **Generic API enhancements** (Nested collections, GraphQL query designer): Defer indefinitely; they provide zero differentiation.
5. **Audit Trail & Git Sync**: Defer until formal compliance phases.

---

## 5. Strategic Roadmap Adjustment Justification

### 5.1 Compliance with Roadmap Authority
Per `ROADMAP_AUTHORITY.md` (Rule 3 & 6):
> *"3. Any roadmap override requires written business justification."*  
> *"6. Auth, RBAC, LDAP, SAML, OIDC, and Multi-Tenancy remain deferred unless roadmap documents are formally updated."*

We formally justify adjusting the roadmap based on what was achieved and learned during Sprints 1, 2, and 3:

1. **The Simulation Moat is Already Built Ahead of Schedule**:
   - In `MASTER_PRODUCT_ACTION_PLAN.md`, the ISO8583 Message Simulator was originally scheduled for **Sprint 6**, and the Scenario State Inspector was scheduled for **Sprint 8**.
   - In `MASTER_PRODUCT_ACTION_PLAN_REVIEW.md`, the Reference Banking Sandbox was scheduled for **Sprint 7**.
   - Sprints 1, 2, and 3 pulled these flagship items forward and **shipped all of them by v0.3.0**.
   - As a result, the product's simulation engine is no longer lagging behind; it is **three sprints ahead of schedule**.

2. **White Labeling Cannot Precede Authentication**:
   - In the old sequential schedule, Sprint 4 was "White Label Backend" and Sprint 5 was "White Label Frontend".
   - However, deploying a white-labeled product that has **no login page and no user security** is useless to an enterprise customer. A bank cannot deploy a white-labeled sandbox if any anonymous user on their network can modify core payment routes.
   - White labeling is a brand customization layer; authentication is a security prerequisite.

3. **Multi-User Readiness Demands Identity First**:
   - The Banking Sandbox is now complete. The very next bottleneck is that multiple testers collide on the hardcoded `"default-user"` and global Redis scenario state. Introducing user identity in Sprint 4 resolves this immediately.

### 5.2 Proposed Master Sprint Sequence (Sprints 4 – 8)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     REVISED MASTER ROADMAP (SPRINTS 4 – 8)                      │
├──────────┬──────────────────────────────────────────┬───────────────────────────┤
│ Sprint   │ Focus Area                               │ Core Outcome              │
├──────────┼──────────────────────────────────────────┼───────────────────────────┤
│ Sprint 4 │ Authentication Foundation & Multi-User   │ Enterprise Front Door,    │
│          │ Identity (with Scenario Namespacing)     │ JWT, Login Wall, Multi-QA │
├──────────┼──────────────────────────────────────────┼───────────────────────────┤
│ Sprint 5 │ White Label Platform                     │ Customizable Branding,    │
│          │ (Backend BrandingAPI + Frontend Theming) │ CSS Injection, Env Vars   │
├──────────┼──────────────────────────────────────────┼───────────────────────────┤
│ Sprint 6 │ Role-Based Access Control (RBAC)         │ Admin, Operator, Viewer   │
│          │ & Frontend Hardening                     │ Permissions + Boundaries  │
├──────────┼──────────────────────────────────────────┼───────────────────────────┤
│ Sprint 7 │ Deployment, Helm Chart & Operations      │ Production Kubernetes &   │
│          │ (Optional OIDC/SSO Extension Hook)       │ TLS Hardening             │
├──────────┼──────────────────────────────────────────┼───────────────────────────┤
│ Sprint 8 │ Advanced ISO8583 UX & Ecosystem          │ Bitmap Grid, Hex Tracing, │
│          │                                          │ Community Dialect Packs   │
└──────────┴──────────────────────────────────────────┴───────────────────────────┘
```

---

## 6. Recommended Sprint 4 Scope & Deliverables

### Sprint 4 Name:
**"Authentication Foundation & Multi-User Identity"**

### Sprint 4 Deliverables:

#### Deliverable 1: User & Authentication Domain Architecture
- **Domain Layer Purity**: Create `com.dynamicmock.domain.entity.User` with zero framework dependencies (id, username, email, passwordHash, active, createdAt, updatedAt).
- **Out Ports**: Create `UserRepository` and `PasswordEncoderPort` interfaces.
- **Infrastructure Persistence**: Implement `UserMongoEntity`, `UserMongoRepository`, and `UserMapper` adhering to Hexagonal Architecture standards.

#### Deliverable 2: Security & Token Infrastructure
- **Stateless Authentication**: Integrate Spring Security with stateless JWT (`jjwt` or Spring Security OAuth2 Resource Server).
- **BCrypt Password Encryption**: Production-grade password hashing adapter.
- **Auth Controller**:
  - `POST /api/auth/login` — Returns JWT token, user info, and expiration.
  - `POST /api/auth/register` — Initial registration (can be restricted via env var `APP_AUTH_ALLOW_REGISTRATION`).
  - `GET /api/auth/me` — Returns current authenticated user context.
- **Route Protection**:
  - `/api/**`: Protected by JWT verification filter.
  - `/mock/**`: Preserved as open/unauthenticated for simulated mock traffic.
  - Port `8583`: Preserved for ISO8583 TCP message traffic.
- **Bootstrap Administrator**: `DemoDataLoader` seeds a default admin account via environment variables (`ADMIN_USERNAME`, `ADMIN_PASSWORD`) if no users exist.

#### Deliverable 3: User Context Integration across Services
- Refactor `WorkspaceCollectionService`, `RouteService`, `EnvironmentService`, and `PostmanImportService` to replace hardcoded `"default-user"` with the authenticated user ID from `SecurityContext`.
- Provide seamless migration/fallback so existing sandbox data created under `"default-user"` remains accessible to the seeded administrator.

#### Deliverable 4: Frontend Auth Wall & Session Management
- **Next.js Auth Pages**: Professional, themed Login and Register screens matching the dark/light design system.
- **Auth Context & Hook**: `AuthContext.tsx` and `useAuth()` hook managing token persistence (localStorage / HttpOnly cookie) and current user state.
- **Axios Interceptor Upgrade**: Update `apiClient.ts` to attach `Authorization: Bearer <token>` instead of static `X-API-KEY`.
- **Navigation Guard**: Automatic redirection to `/login` when unauthenticated; graceful handling of 401 Unauthorized responses.
- **User Profile in Header**: Replace hardcoded user badge in `Header.tsx` with active user info and Logout CTA.

#### Deliverable 5: Scenario State Namespacing (Multi-User QA Isolation)
- Update `ScenarioService` to support namespaced Redis keys:
  - Format: `scenario:state:{namespace}:{scenarioName}`
  - Format: `dynamic-mock:script-state:{namespace}`
- Namespace resolution logic:
  - If authenticated user context exists: use `userId` or active `workspaceId`.
  - If ISO8583 simulation header / session ID provided: use session ID.
  - Default fallback: `"global"` (ensuring 100% backward compatibility for single-user tutorials and quickstart flows).

---

## 7. Risks & Mitigations

| Risk ID | Risk Description | Severity | Mitigation Strategy |
|---|---|---|---|
| **R-AUTH-1** | Securing `/api/**` breaks mock execution or external test clients | **High** | Strictly decouple `/mock/**` (public mock engine) and TCP port `8583` from `/api/**` (management API). Unit and integration test both paths. |
| **R-AUTH-2** | Existing sandbox tests and demo data fail due to missing user ID | **Medium** | Ensure `DemoDataLoader` and `SandboxController` assign demo assets to the bootstrap admin user, maintaining fallback access for legacy records. |
| **R-AUTH-3** | Domain layer purity violated by Spring Security annotations | **Medium** | Maintain strict Hexagonal Architecture: zero Spring Security or Jakarta annotations on `User.java`. Keep security logic strictly within `infrastructure/security`. |
| **R-AUTH-4** | Next.js client-side hydration or routing flickers on auth check | **Low** | Implement a clean `AuthLoading` skeleton state in `WorkspaceLayout` while session token validation completes. |

---

## 8. Success Criteria

Sprint 4 will be declared complete when:

1. **Authentication Wall**:
   - Accessing `http://localhost:3000` without a token redirects to the Login screen.
   - Valid credentials authenticate the user, return a JWT, and transition into the workspace.
2. **API Protection**:
   - Calling `/api/routes` or `/api/scenarios` without `Authorization: Bearer <token>` returns `401 Unauthorized`.
   - Calling `/mock/**` continues to execute successfully without authentication.
   - ISO8583 TCP simulations on port `8583` continue to execute flawlessly.
3. **User Context Eradication**:
   - Zero occurrences of hardcoded `"default-user"` in active application paths.
   - Collections, routes, and environments created in the UI are owned by the logged-in user.
4. **Scenario Concurrency**:
   - Two distinct user sessions can run the Banking Sandbox "Card Payment Flow" independently without corrupting each other's Redis state.
5. **Quality & Governance**:
   - Backend tests pass (`./gradlew test`).
   - Frontend tests pass (`npm run test` and `npm run build`).
   - Documentation updated (`README.md`, `CHANGELOG.md`).

---

## 9. Expected Impact

Delivering Sprint 4 will produce a dramatic transformation in product maturity:

```
┌──────────────────────────────────────┐          ┌──────────────────────────────────────┐
│           BEFORE SPRINT 4            │          │            AFTER SPRINT 4            │
├──────────────────────────────────────┤          ├──────────────────────────────────────┤
│ • Single-user local prototype        │          │ • Secure multi-user team platform    │
│ • Hardcoded default-user identity    │  ──────> │ • Real user accounts & JWT sessions  │
│ • Unauthenticated management API     │          │ • Enterprise-defensible security     │
│ • Multi-tester scenario state races  │          │ • Namespaced scenario isolation      │
│ • Pilot rejected by enterprise SecOps│          │ • Pilot-ready for bank IT deployment │
└──────────────────────────────────────┘          └──────────────────────────────────────┘
```

By completing Sprint 4, Dynamic Mock will pair its **world-class financial simulation engine** with an **enterprise-defensible security foundation**, creating a rock-solid runway for White Labeling (Sprint 5) and enterprise pilot rollouts.

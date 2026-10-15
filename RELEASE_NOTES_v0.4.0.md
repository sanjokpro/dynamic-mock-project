# Dynamic Mock v0.4.0

Dynamic Mock now includes enterprise-ready authentication and multi-user identity management.

---

## Title: v0.4.0 - Authentication Foundation & Multi-User Identity

Dynamic Mock v0.4.0 elevates the platform from a single-user developer prototype to a deployable, multi-user enterprise integration simulation platform. This release introduces stateless JWT authentication powered by Spring Security 6, full user account management, authenticated workspaces, concurrent scenario state isolation via session namespacing, and seamless backward compatibility with all existing automation workflows and the Reference Banking Sandbox.

---

## Highlights

### Authentication Foundation

- **Spring Security 6 Stateless Filter Chain**: Strict stateless session creation policy (`SessionCreationPolicy.STATELESS`) protecting all management APIs under `/api/**`.
- **JWT Authentication (HS256)**: Secure HMAC-SHA256 token issuance with startup validation enforcing secrets of 32 characters or longer. Default 8-hour token lifetime configurable via environment variable.
- **Secure Password Hashing with BCrypt**: Password hashing using BCrypt at strength 12.
- **Admin Bootstrap Provisioning**: Automatic seeding of the primary administrator account on initial startup (`UserBootstrapRunner`), with warnings if default credentials are used.
- **Domain Layer Purity**: Pure Lombok domain entity (`User.java`) with zero framework, Spring Data, Jakarta, or Jackson dependencies, adhering strictly to Clean Architecture.

### Multi-User Platform

- **Authenticated User Sessions**: Next.js 16 / React 19 in-memory token state management (`AuthContext`) that completely avoids vulnerable `localStorage` or `sessionStorage` token caching.
- **Login Experience**: Themed, responsive sign-in interface at `/login` with real-time validation, password toggle, loading spinner, and uniform error reporting against enumeration attacks.
- **User-Aware Collections & Workspaces**: Dynamic user identity bindings in the workspace header, sidebar navigation, and collection filters, replacing hardcoded single-user identifiers.
- **Client Route Protection**: Declarative `<AuthGuard>` wrapper that safeguards workspace views and redirects unauthenticated requests.

### Scenario Isolation

- **`X-Session-ID` Namespacing**: Support for the `X-Session-ID` HTTP header in `DynamicRouteDispatcher`, enabling independent simulation state across concurrent test sessions.
- **Concurrent Execution Support**: Distinct Redis keys (`scenario:state:{scenarioName}:{sessionId}` and `scenario:exec:{scenarioName}:{sessionId}`) ensure that multi-tenant QA pipelines run in parallel without cross-test state collisions.
- **Redis-Backed Isolated Scenario State**: Script execution variables scoped to `dynamic-mock:script-state:session:{sessionId}` while retaining global fallbacks.

### Enterprise Readiness

- **Protected Management APIs**: All administrative CRUD endpoints (`/api/**`) require valid JWT authentication.
- **Public Simulation Endpoints**: Mock simulation routes (`/mock/**`) and WebSocket traffic streaming (`/ws-traffic/**`) remain fully public and open for mock consumers.
- **Configurable CORS**: Dynamic origin configuration via `APP_CORS_ALLOWED_ORIGINS` for multi-domain enterprise deployments.
- **Preserved Automation Workflows**: Retained headless `X-API-KEY` header authentication with constant-time comparison (`MessageDigest.isEqual`) for CI/CD pipelines and external runners.

### Backward Compatibility

- **Banking Sandbox Maintained**: Reference Banking Sandbox and ISO8583 payment flow (`INITIAL` → `AUTHORIZED` → `CAPTURED` → `REVERSED` → `INITIAL`) operate seamlessly with zero configuration changes.
- **ISO8583 Simulator Maintained**: In-browser message simulator, packager upload, and data dictionary continue uninterrupted.
- **API Key Automation Maintained**: Existing automated test runners using `X-API-KEY` execute without requiring interactive login.

---

## Validated Metrics

- **Backend Test Suite**: 376 tests executed, 0 failures, 100% assertions green.
- **Frontend Build**: Zero TypeScript errors, production build verified.
- **Clean Architecture Integrity**: Hexagonal boundaries verified; zero `"default-user"` occurrences in application source code.

# V0.4.0 Release Report

## Release Details

- **Tag Created**: `v0.4.0`
- **Tag SHA**: `efa4015319a7f5bf8c6b8c2ae95e7c2ed00dfe4f`
- **Commit SHA**: `27664bde1f01b69d08295e65e1587781dd93dd8a`
- **Push Status**: **SUCCESS** (`git push origin master` and `git push origin v0.4.0` completed cleanly)
- **Release Title**: `v0.4.0 - Authentication Foundation & Multi-User Identity`
- **Release Tag URL**: `https://github.com/sanjokpro/dynamic-mock-project/releases/tag/v0.4.0`
- **Repository Hygiene Status**: **PASS** (Zero build caches or forbidden artifacts tracked; zero files exceeding size thresholds)

---

## Release Summary

**Dynamic Mock v0.4.0**

Dynamic Mock now includes enterprise-ready authentication and multi-user identity management.

### Key Capabilities Delivered

1. **Authentication Foundation**:
   - Spring Security 6 stateless filter chain (`SessionCreationPolicy.STATELESS`).
   - HS256-signed JWT token issuance and validation with minimum 32-character secret enforcement on startup.
   - BCrypt password hashing at strength 12.
   - Initial administrator bootstrapping runner (`UserBootstrapRunner`) on first startup.
   - Pure domain entity `User.java` with zero framework annotations adhering strictly to Clean Architecture.

2. **Multi-User Platform**:
   - In-memory JWT token store (`AuthContext.tsx`) preventing XSS-based storage attacks.
   - Dedicated sign-in page at `/login` with full error handling and loading indicators.
   - Client-side `<AuthGuard>` route wrapper redirecting unauthenticated users.
   - Workspace header and sidebar bound to dynamic user identity, fully eliminating legacy `"default-user"` references.

3. **Scenario Isolation & Namespacing**:
   - `X-Session-ID` header routing in `DynamicRouteDispatcher` for concurrent simulation sessions.
   - Redis state keys scoped to `scenario:state:{scenarioName}:{sessionId}` and script state to `dynamic-mock:script-state:session:{sessionId}`.
   - Independent execution counts and state transitions for parallel test automation pipelines.

4. **Enterprise Readiness**:
   - All administrative `/api/**` routes require authenticated Bearer tokens.
   - Public simulation endpoints (`/mock/**`) and live traffic streaming (`/ws-traffic/**`) remain open and accessible.
   - Configurable cross-origin resource sharing (`APP_CORS_ALLOWED_ORIGINS`).
   - Headless automation workflows preserved via secondary `ApiKeyAuthFilter` (`X-API-KEY`) with timing-attack defense (`MessageDigest.isEqual`).

5. **Backward Compatibility**:
   - Reference Banking Sandbox and ISO8583 payment flow (`Card Payment Flow`) remain 100% operational with default fallback.
   - ISO8583 in-browser Simulator, Packager Uploader, and Data Dictionary preserved.

---

## Verification Evidence

- **Backend Unit & Integration Tests**: 376 tests executed, 0 failures, 100% passing.
- **Frontend Type-Check**: Zero TypeScript compiler errors (`npx tsc --noEmit`).
- **Frontend Unit Tests**: 11 tests across 3 suites passed (`npm run test`).
- **Frontend Production Build**: Static site generation completed with 5/5 pages (`npm run build`).
- **Hardcoded Identifier Grep**: `git grep -ni "default-user"` returned 0 matches in all application source files.

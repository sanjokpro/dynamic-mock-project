# Sprint 4 Architecture Review
## Authentication Foundation & Multi-User Identity

**Document Version**: 1.0  
**Date**: 2026-10-14  
**Status**: ARCHITECTURE APPROVED WITH ADJUSTMENTS  
**Reviewing**: `SPRINT_04_DISCOVERY.md` proposed architecture  
**Governing Documents**: `AI_GOVERNANCE.md`, `MASTER_PRODUCT_ACTION_PLAN.md`

---

## 1. Review Summary

The `SPRINT_04_DISCOVERY.md` recommendation is architecturally sound and correctly identified.

The discovery missed **two concrete implementation risks** that must be resolved before coding begins: the Spring Security auto-config lockout and the `X-API-KEY` deprecation strategy.

Three areas require scope adjustment:
1. The scenario namespacing design is too coarse (user-ID scope is premature).
2. The registration endpoint has an unresolved security decision.
3. The `DemoDataLoader`/demo profile interaction with the new identity model needs explicit design.

> [!IMPORTANT]
> This document is the approved architecture contract for Sprint 4.  
> Implementation must not deviate from it without a written amendment.

---

## 2. Verified Code Findings

Direct code inspection confirmed the following before this review was written:

| Component | File | Current Behaviour |
|---|---|---|
| **API Key Filter** | `ApiKeyAuthFilter.java` | Registered on `/api` + `/api/*` via `FilterRegistrationBean` at order 1. Uses `MessageDigest.isEqual` (constant-time comparison — correct). Reads key from `${app.api.key:default-dev-key}`. |
| **Frontend Key** | `.env.example` + `apiClient.ts` | `NEXT_PUBLIC_API_KEY=default-dev-key` inlined into client bundle at build time. Axios interceptor sends it as `X-API-KEY` header. |
| **Mock Traffic** | `DynamicRouteDispatcher.java` | Registered as a Spring MVC `HandlerInterceptor` on `/mock/**` via `WebConfig.java`. Completely independent of the API key filter. No Spring Security involvement. |
| **ISO8583 TCP** | jPOS `Iso8583Server` | TCP socket on port 8583. Spring Security cannot intercept TCP traffic here. No changes needed. |
| **CORS** | `WebConfig.java` | Hard-coded `allowedOriginPatterns("http://localhost:3000")`. Must become configurable via env var for enterprise deployment. |
| **Spring Security** | `build.gradle` | **`spring-boot-starter-security` is NOT present.** Must be added from scratch. |
| **Hardcoded User** | `Header.tsx:32`, `Sidebar.tsx:25`, `DemoDataLoader.java:55,56,64`, `PostmanImportService.java:42` | All contain the string literal `"default-user"`. |
| **Redis Script State** | `DynamicRouteDispatcher.java:289-292` | `X-Dynamic-Mock-Env` header already supported for env-scoped key `dynamic-mock:script-state:env:{envId}`. Global fallback: `dynamic-mock:script-state:global`. |
| **Redis Scenario State** | `ScenarioService.java:32-33` | Keys are `scenario:state:{scenarioName}` and `scenario:exec:{scenarioName}`. Not namespaced by user or session. |
| **Demo Profile** | `DemoDataLoader.java` | Protected by `@Profile("demo")`. Seeds collections owned by `"default-user"`. Must be updated in Sprint 4. |

---

## 3. What Should Stay

### 3.1 `ApiKeyAuthFilter` — Keep and Demote, Not Delete

JWT auth becomes the primary authentication mechanism. The API key filter is preserved as a secondary operator key for:
- CI/CD pipelines that load the banking sandbox via `curl`.
- Headless automation where a full OAuth session is impractical.

**Change**: Demote filter order from `1` to `100`. Add a short-circuit: if an `Authorization: Bearer` header is present (JWT path), skip API key validation.

```java
// ApiKeyAuthFilter.doFilterInternal — add at the top
String authHeader = request.getHeader("Authorization");
if (authHeader != null && authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response); // JWT filter already handled this
    return;
}
```

### 3.2 `/mock/**` Routes — Preserve as Fully Public

The `DynamicRouteDispatcher` is a Spring MVC `HandlerInterceptor`, not a `Filter`. Spring Security's `SecurityFilterChain` runs before the DispatcherServlet, so `/mock/**` can be excluded from security without any change to the dispatcher:

```java
// SecurityConfig.java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/mock/**").permitAll()
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/ws-traffic/**").permitAll()
    .requestMatchers("/api/**").authenticated()
    .anyRequest().permitAll()
);
```

### 3.3 Domain Entity Purity — Non-Negotiable

Per `AI_GOVERNANCE.md` INCORPORATED ARCHITECTURE STANDARDS: zero framework annotations in `com.dynamicmock.domain.*`. The `User` entity must follow the same pattern as `MockRoute.java` and `Scenario.java`.

### 3.4 `X-Dynamic-Mock-Env` Redis Pattern — Keep as Base for Namespacing

`DynamicRouteDispatcher.java` already has logic to build a per-environment Redis key when `X-Dynamic-Mock-Env` is present. This pattern is the correct foundation for session-ID-based scenario namespacing.

---

## 4. What Must Change

### 4.1 Backend — Add Spring Security

**`spring-boot-starter-security` is absent.** It must be added along with JJWT.

```groovy
// backend/build.gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

> [!WARNING]
> Adding `spring-boot-starter-security` causes Spring Boot to auto-configure a policy
> that **blocks ALL requests** by default. `SecurityConfig.java` MUST be the very
> first file created — before any other Sprint 4 code. If it is not present at
> startup, the application will return `403` on every request including `/mock/**`.

**Required security infrastructure package**:

```
infrastructure/security/
├── SecurityConfig.java           — SecurityFilterChain bean with permit/deny rules
├── JwtTokenService.java          — Issue and validate JWTs; reads secret from env var
├── JwtAuthenticationFilter.java  — OncePerRequestFilter; extracts Bearer token, sets SecurityContext
├── UserDetailsServiceImpl.java   — Loads UserMongoEntity by username for Spring Security
└── PasswordEncoderConfig.java    — BCryptPasswordEncoder @Bean
```

### 4.2 JWT Strategy — Stateless HS256, 8-Hour Expiry

| Decision | Value | Rationale |
|---|---|---|
| Algorithm | HS256 | Symmetric; no key distribution needed for single-server. RS256 is for distributed API gateways. |
| Expiry | 8 hours | Matches a workday session. Sufficient for QA lab environments. |
| Refresh tokens | Deferred to Sprint 6 | Do not over-engineer. |
| Logout endpoint | Deferred to Sprint 6 | Stateless JWTs expire naturally; revocation list is Sprint 6. |

**Minimal JWT payload**:
```json
{
  "sub": "user-mongodb-id",
  "username": "alice",
  "iat": 1234567890,
  "exp": 1234596690
}
```

No `roles` in the payload yet — that belongs in Sprint 6 RBAC.

**`application.yml` additions**:
```yaml
app:
  auth:
    jwt-secret: ${APP_AUTH_JWT_SECRET:local-dev-insecure-secret-must-be-changed-in-prod}
    jwt-expiry-hours: ${APP_AUTH_JWT_EXPIRY_HOURS:8}
  cors:
    allowed-origins: ${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}
  auth-allow-registration: ${APP_AUTH_ALLOW_REGISTRATION:false}
```

> [!CAUTION]
> `APP_AUTH_JWT_SECRET` must be at least 32 ASCII characters (256 bits) for HS256.
> `JwtTokenService` must validate the secret length at startup and emit a startup
> `WARN` log if the insecure default value is detected.

### 4.3 Registration Endpoint Security Decision

The discovery left the `POST /api/auth/register` security model ambiguous. Four options were evaluated:

| Option | Description | Verdict |
|---|---|---|
| A — Open | Anyone can register | **REJECT** — Unacceptable for corporate network deployment |
| B — First-User-Only | Open until first user exists | **REJECT** — TOCTOU race condition; confusing on reinstall |
| C — Invite Token | Admin generates invite token | **DEFER** — Correct long-term; over-engineered for Sprint 4 |
| **D — Env-Var-Gated** | `APP_AUTH_ALLOW_REGISTRATION=true` enables it | **ACCEPT** |

**Decision**: Option D. Default is `false` (registration disabled). When disabled, the endpoint returns `403` with message `"Registration is disabled. Set APP_AUTH_ALLOW_REGISTRATION=true to enable."`.

**Bootstrap Admin**: Separately, a `UserBootstrapRunner` (implements `ApplicationRunner`) creates the first admin if the users collection is empty on startup. Reads `ADMIN_USERNAME` / `ADMIN_PASSWORD` env vars. Emits a `WARN` log if the dev default `admin/admin` is used.

### 4.4 Approved User Domain Entity

```java
// com.dynamicmock.domain.entity.User — NO framework annotations
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    private String id;
    private String username;     // unique, lowercase
    private String email;        // unique
    private String passwordHash; // BCrypt hash — never plain text
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

No `roles` field. Roles are Sprint 6 RBAC scope.

**Out Port (Domain Layer)**:
```java
// com.dynamicmock.domain.port.out.UserRepository
public interface UserRepository {
    Optional<User> findById(String id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User save(User user);
    long count(); // Used by UserBootstrapRunner to check if any users exist
}
```

### 4.5 Auth API Surface — Minimal

```
POST /api/auth/login     {username, password} → {token, expiresInSeconds, userId, username}
POST /api/auth/register  {username, email, password} → {userId, username}   [env-var-gated]
GET  /api/auth/me        → {userId, username, email}                         [JWT required]
```

### 4.6 Frontend JWT Storage Decision

| Storage Option | XSS Risk | CSRF Risk | Verdict |
|---|---|---|---|
| `localStorage` | HIGH | None | Avoid |
| `sessionStorage` | HIGH | None | Avoid |
| HttpOnly Cookie | None | Medium | Preferred long-term |
| **In-memory React state** | None | None | **Accept for Sprint 4** |

**Decision**: In-memory storage in `AuthContext`. Rationale: This is a self-hosted intranet tool. XSS attack surface is substantially lower than a public SaaS. In-memory avoids CSRF engineering complexity. Re-evaluate HttpOnly cookies in Sprint 6 when the session model is more stable.

**Frontend file structure**:
```
src/
├── context/AuthContext.tsx          — token state, user info, login/logout
├── hooks/useAuth.ts                 — convenience hook
├── lib/apiClient.ts                 — updated: replaces X-API-KEY with Bearer token
├── app/
│   ├── login/page.tsx               — themed login page
│   └── layout.tsx                   — updated: wraps with AuthProvider
└── components/auth/AuthGuard.tsx    — redirects to /login if unauthenticated
```

**Navigation Guard**: Use a client-side `AuthGuard` component inside `WorkspaceLayout`. Render an `AuthLoading` skeleton while `isLoading === true` to prevent flash of unauthenticated content. Redirect to `/login` if `!token && !isLoading`.

---

## 5. What Is Over-Engineered

### 5.1 User-ID Scenario Namespacing — Premature

The discovery proposed namespacing by user ID or workspace ID. This is architecturally premature because:

1. `Scenario.java` has no `userId` or `workspaceId` field. Scenarios are global resources. Adding user-scoped state for a non-user-scoped resource creates a contradictory data model.
2. The Banking Sandbox tutorial describes a **shared** flow — a single `Card Payment Flow` scenario visible to everyone. User-ID namespacing would give each user their own private state machine, breaking the tutorial.

**Approved Namespacing Strategy for Sprint 4: Session-ID Header**

Optional `X-Session-ID` request header on `/mock/**` traffic. Backward compatible.

Redis key scheme:
```
# Shared / default (no header — existing behaviour preserved)
scenario:state:{scenarioName}
scenario:exec:{scenarioName}
dynamic-mock:script-state:global

# Per-session isolation (X-Session-ID header present)
scenario:state:{scenarioName}:{sessionId}
scenario:exec:{scenarioName}:{sessionId}
dynamic-mock:script-state:{sessionId}
```

This delivers concurrent QA test isolation without touching the User domain model.  
Per-user scenario scoping belongs in Sprint 6 alongside RBAC.

### 5.2 `PasswordEncoderPort` Interface — Remove

Password encoding is an infrastructure concern, not a domain concern. The `User` entity stores the resulting `passwordHash` string. No port interface is needed for the encoder — inject `BCryptPasswordEncoder` directly from `PasswordEncoderConfig`.

### 5.3 No Separate `AuthApplicationService` for Sprint 4

A two-endpoint auth service does not need a full port/adapter Application Service with separate in/out ports. A lightweight `AuthService` class in the application layer that calls `UserRepository` and `JwtTokenService` is sufficient and correct for Sprint 4 scope.

---

## 6. What Is Missing From the Discovery

### 6.1 CORS Is a Blocker for Enterprise Deployment

`WebConfig.java` hard-codes `allowedOriginPatterns("http://localhost:3000")`. JWT-bearing requests from a production origin (`https://simulator.bank.internal`) will be rejected by the browser before reaching the server.

**Fix**: `WebConfig.java` must read `${app.cors.allowed-origins}` and split on commas to support multi-origin configuration.

### 6.2 `DemoDataLoader` Breaks Without `"default-user"` User

After Sprint 4, `"default-user"` will not exist as a real user record. The `DemoDataLoader` currently creates collections under that string. This will result in collections with a dangling non-existent `userId`.

**Fix in Step 8**: `DemoDataLoader` must call `UserRepository.findByUsername("admin")` (or the configured `ADMIN_USERNAME`) and use the returned user ID when creating demo collections.

### 6.3 WebSocket `/ws-traffic` — Must Be Explicitly Permitted

The traffic console SockJS/STOMP connection at `/ws-traffic` will be blocked by Spring Security if not explicitly allowed. It must be in the `permitAll()` list in `SecurityConfig`.

### 6.4 No `spring-boot-starter-security` in Test Dependencies

After adding the security dependency, existing `MockMvc` tests for `RouteControllerTest`, `ScenarioController`, etc., will fail because they will encounter Spring Security interceptors. Tests must either:
- Use `@WithMockUser` annotation, or
- Add `@AutoConfigureMockMvc(addFilters = false)` where auth bypass is appropriate.

---

## 7. Security Recommendations

### 7.1 JWT Secret Validation at Startup

```java
@PostConstruct
public void validateSecret() {
    if (jwtSecret.length() < 32) {
        throw new IllegalStateException(
            "APP_AUTH_JWT_SECRET must be at least 32 characters for HS256.");
    }
    if (jwtSecret.equals("local-dev-insecure-secret-must-be-changed-in-prod")) {
        log.warn("*** INSECURE JWT SECRET IN USE. Set APP_AUTH_JWT_SECRET before production deployment. ***");
    }
}
```

### 7.2 Uniform Auth Error Messages

Both "user not found" and "wrong password" must return **identical** error responses:

```json
HTTP 401: { "error": "Invalid username or password" }
```

Never differentiate. User enumeration via error messages is a common vulnerability.

### 7.3 BCrypt Strength

Use BCrypt strength 12 (Spring Boot default for `BCryptPasswordEncoder()`). Do not lower it.

### 7.4 Session Expiry UX

The login response body should include `expiresInSeconds`. The frontend should show a "Session expiring" toast ~15 minutes before expiry to prevent mid-work lockouts.

### 7.5 Preserve `MessageDigest.isEqual` in API Key Filter

The existing timing-attack-resistant constant-time comparison in `ApiKeyAuthFilter` must not be replaced with `String.equals()` when the filter is updated.

---

## 8. Approved Implementation Order

### Phase 1: Backend (Days 1–2)

| Step | Task |
|---|---|
| 1 | Add `spring-boot-starter-security`, `jjwt-api/impl/jackson` to `build.gradle` |
| 2 | Create `User.java` domain entity and `UserRepository` port interface |
| 3 | Create `UserMongoEntity`, `UserMongoRepository`, `UserMapper`, `UserRepositoryImpl` |
| 4 | Create `PasswordEncoderConfig`, `JwtTokenService`, `JwtAuthenticationFilter`, `UserDetailsServiceImpl`, `SecurityConfig` |
| 5 | Update `application.yml` (jwt-secret, jwt-expiry, cors, allow-registration). Update `WebConfig.java` for configurable CORS |
| 6 | Create `AuthService` + `AuthController` (`/login`, `/register`, `/me`) |
| 7 | Create `UserBootstrapRunner` — seeds admin from env vars on empty users collection |
| 8 | Update `DemoDataLoader` — replace `"default-user"` with admin user ID |
| 9 | Update `ApiKeyAuthFilter` — demote to order 100; short-circuit on Bearer token |
| 10 | Write backend auth tests: `AuthControllerTest` (MockMvc), `JwtTokenServiceTest`, `UserRepositoryImplTest` (Testcontainers) |

### Phase 2: Scenario Session Namespacing (Day 3)

| Step | Task |
|---|---|
| 11 | Add `X-Session-ID` header reading to `DynamicRouteDispatcher`; pass as `sessionId` to `ScriptContext` |
| 12 | Update `RedisBackedStateMap` key construction in `DynamicRouteDispatcher` to use `sessionId` if present |
| 13 | Update `ScenarioService.getCurrentState()`, `setCurrentState()`, `processTransition()` to accept optional namespace |
| 14 | Ensure `Iso8583Server` scenario state calls pass session/correlation ID as namespace |
| 15 | Write `ScenarioService` namespacing unit tests |

### Phase 3: Frontend Auth Wall (Days 4–5)

| Step | Task |
|---|---|
| 16 | Create `AuthContext.tsx` — token state, `login()`, `logout()`, `getToken()` |
| 17 | Update `providers.tsx` — add `<AuthProvider>` outside `<WorkspaceProvider>` |
| 18 | Update `apiClient.ts` — replace `X-API-KEY` with `Authorization: Bearer`; handle 401 → logout |
| 19 | Create `/app/login/page.tsx` — themed login form matching dark/light design system |
| 20 | Create `AuthGuard.tsx` — `AuthLoading` skeleton while initialising; redirect to `/login` if unauthenticated |
| 21 | Update `WorkspaceLayout.tsx` — wrap in `<AuthGuard>` |
| 22 | Update `Header.tsx` and `Sidebar.tsx` — replace `'default-user'` with `useAuth().user?.id`; add Logout to Header |
| 23 | Write frontend tests: `AuthContext` login/logout, `AuthGuard` redirect behaviour |

---

## 9. Risks

| ID | Risk | Severity | Probability | Mitigation |
|---|---|---|---|---|
| **R1** | Spring Security auto-config blocks all endpoints on startup | Critical | Certain | `SecurityConfig.java` must be Step 4 — the very first file written. Verify `/mock/hello` returns `200` before any other Sprint 4 code. |
| **R2** | `ApiKeyAuthFilter` and `JwtAuthenticationFilter` double-process requests | High | Medium | Demote API key filter to order 100; add Bearer short-circuit. Write integration test covering both auth paths. |
| **R3** | Frontend shows workspace content flash before auth check | Medium | High | Implement `AuthLoading` skeleton; do not render workspace until `isLoading === false`. |
| **R4** | `DemoDataLoader` seeds collections under `"default-user"` — a non-existent user after Sprint 4 | High | Certain | Update `DemoDataLoader` in Step 8 to resolve actual admin user ID. |
| **R5** | JWT secret committed to git | Critical | Low but catastrophic | No production default in `application.yml`. Startup validation throws `IllegalStateException` if secret is too short. |
| **R6** | Banking Sandbox QA state collisions (un-namespaced Redis keys) | Medium | Certain | Phase 2 session namespacing must be completed before sprint is declared done. |
| **R7** | WebSocket `/ws-traffic` blocked by Spring Security | High | Certain | Add `/ws-traffic/**` to `permitAll()` list in `SecurityConfig`. |
| **R8** | Existing `MockMvc` tests fail after Spring Security is added | Medium | Certain | Add `@WithMockUser` or `addFilters = false` to existing controller tests. |

---

## 10. Architecture Decision Record

| # | Decision | Choice | Rationale |
|---|---|---|---|
| ADR-1 | JWT algorithm | HS256 | Symmetric; sufficient for single-server. RS256 adds PKI complexity with no benefit here. |
| ADR-2 | Token expiry | 8 hours | Matches workday; no refresh token complexity in Sprint 4. |
| ADR-3 | Token storage | In-memory React state | Self-hosted intranet reduces XSS risk; avoids CSRF overhead of HttpOnly cookies. Re-evaluate Sprint 6. |
| ADR-4 | Registration model | Env-var gated | Prevents open registration on corporate networks. Simple operator control with no UI needed. |
| ADR-5 | Scenario namespacing | `X-Session-ID` header | Backward-compatible. No user model changes required. Per-user scoping deferred to Sprint 6. |
| ADR-6 | API key filter | Demoted, preserved | Supports headless/CI use cases without JWT session. |
| ADR-7 | Domain purity | Zero framework annotations in `User.java` | Required by `AI_GOVERNANCE.md` INCORPORATED ARCHITECTURE STANDARDS. |
| ADR-8 | Password hashing | BCrypt strength 12 | Spring Security default; appropriate for credential storage in MongoDB. |

---

## 11. Out of Scope — Do Not Include in Sprint 4

- RBAC (roles, permissions) — Sprint 6
- Refresh tokens / server-side token revocation — Sprint 6
- LDAP / OIDC / SAML / SSO — Sprint 7+
- WebSocket STOMP authentication — Sprint 6
- Per-user scenario scoping (adding `userId` to `Scenario.java`) — Sprint 6
- Multi-tenancy — deferred
- Registration invite tokens — deferred
- Audit trail for auth events — deferred
- HttpOnly cookie session — re-evaluate Sprint 6
- White labeling — Sprint 5

---

## 12. Sprint 4 Completion Checklist

### Backend
- [ ] `./gradlew test` passes with zero failures
- [ ] `POST /api/auth/login` returns a valid JWT token
- [ ] `GET /api/routes` without token returns `401 Unauthorized`
- [ ] `GET /mock/hello` without any token returns `200 OK` (mock traffic preserved)
- [ ] Admin user bootstrapped automatically on empty users collection
- [ ] Zero occurrences of `"default-user"` string in active application code paths
- [ ] JWT secret WARN log emitted at startup when dev default is detected
- [ ] Existing `RouteControllerTest`, `ScenarioController` tests pass with `@WithMockUser`

### Frontend
- [ ] `npm run build` succeeds with zero TypeScript errors
- [ ] Navigating to `http://localhost:3000` without login redirects to `/login`
- [ ] Valid credentials open the workspace
- [ ] Invalid credentials show a uniform error (not which field was wrong)
- [ ] Logout clears state and redirects to `/login`
- [ ] `Header.tsx` shows logged-in username, not `"default-user"`

### Scenario Namespacing
- [ ] Two concurrent requests with different `X-Session-ID` values maintain independent scenario states
- [ ] Request without `X-Session-ID` uses global state (Banking Sandbox tutorial unchanged)

### Documentation
- [ ] `CHANGELOG.md` updated with Sprint 4 entries
- [ ] `README.md` updated with authentication setup and `APP_AUTH_JWT_SECRET` instructions
- [ ] `docker-compose.yml` updated with `APP_AUTH_JWT_SECRET` placeholder env var

# Changelog

All notable changes to this project will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [0.6.2] — Postman Compatibility, Collection Explorer & Execution Contract (2026-11-16)

### Added

#### Postman Collection Compatibility & Multi-Example Ingestion
- **Postman Collection v2.1 Importer Pipeline (`PostmanImportService`)** — Zero-touch collection ingestion importing requests, folders, headers, query parameters, auth metadata, and saved response examples directly into executable `MockRoute` entities.
- **Multi-Example Route Splitting** — Ingests Postman request items containing $N$ saved examples by generating $N$ distinct conditional mock routes preserving folder paths and parent request context.
- **Deterministic Route Identity (UUIDv5)** — Stable, deterministic route IDs computed from collection ID, folder hierarchy, item name, example name, and sorted effective matcher signatures, ensuring idempotent re-imports without duplicating routes or losing configurations.
- **5-Tier Deterministic Dispatch Precedence (`DynamicRouteDispatcher`)** — Guaranteed route selection order for multi-variant endpoints:
  1. Header override via `x-mock-response-name` (exact example name matching).
  2. Multi-constraint matcher evaluation (query parameter, header, and body conditions).
  3. Satisfied constraint count descending (most specific matcher wins).
  4. Route priority integer descending.
  5. Normalized matcher signature tie-breaker for deterministic selection.
- **Postman Routing Headers Support** — Supports `x-mock-response-name` to target specific saved examples directly, and `x-mock-response-code` to filter candidate responses by HTTP status code before matcher evaluation.
- **Import Notice Classification & Conflict Detection** — Evaluates imported matcher signatures to distinguish genuine routing conflicts (identical query/header matchers or multiple unconditional examples) from harmless distinct parameter variants (e.g. `?VM=AV` vs `?ACC=0000000000`), eliminating false ambiguity warnings.
- **Granular Script Severity Classification** — Postman pre-request scripts are surfaced as actionable warnings (`report.warnings`) advising manual polyglot script review, while test assertions are recorded as informational notices (`report.notices`).

#### Postman Template Compatibility & Dynamic Helpers
- **`{{$timestampSeconds}}` & `{{$postmanTimestamp}}` Helpers** — Generates 10-digit Unix epoch timestamps in seconds matching Postman dynamic variable behavior.
- **Import-Time Timestamp Translation** — Automatically translates Postman `{{$timestamp}}` occurrences in imported response templates to `{{$timestampSeconds}}`, preserving native Dynamic Mock `{{$timestamp}}` (13-digit Unix epoch milliseconds) for existing routes.
- **Safe Query Parameter Extraction (`{{$queryParams}}`)** — Custom Handlebars helper reading query parameters directly from `request.queryParams` wrapped in `Handlebars.SafeString` to prevent HTML entity escaping of Base64 padding (`=`) or symbols in query strings.

#### Collection Explorer & UX Enhancements
- **3-Level Collection Tree (`Sidebar`)** — Hierarchical sidebar tree structure: Collection Folder $\to$ Request Item $\to$ Example Variants, displaying total item counts and active variant sub-trees.
- **Visual Format & Status Badges** — Badges indicating response formats (`HTML`, `XML`, `JSON`), HTTP status codes (`200`, `404`, etc.), and matched query condition pills (e.g. `[VM=AV]`, `[ACC=0000000000]`).
- **Test Client Auto-Prefill** — Selecting a variant in the Collection Explorer automatically pre-fills test client query parameters, headers, and request bodies using the example's `originalRequest` metadata.
- **Smart GET Header Handling** — Omits body-specific headers (`Content-Type`, `Content-Length`) on GET requests while preserving explicit user overrides in the Test Client.
- **Import Modal Notice Feedback** — Renders informational notices in a dedicated blue badge section alongside warnings and error summaries with fallback compatibility for legacy backends.

#### Platform Execution Contract & Telemetry Standardization
- **Formalized Execution Modes (`ExecutionMode.java`)** — Core enum defining standard execution modes: `INLINE`, `PROXY`, `SCRIPT`, `SCENARIO`, and `WEBHOOK`.
- **Mock Action Contract (`MockActionContract.java`)** — Enforces execution contracts across adapters and services, standardizing telemetry aliases while ensuring backward compatibility with existing telemetry consumers.

### Scope & Compatibility Boundaries

#### Supported Behavior
- Full Postman Collection Schema v2.1 import with multi-example splitting, deterministic routing, and idempotent re-import.
- Header-based mock dispatching via `x-mock-response-name` and status filtering via `x-mock-response-code`.
- Handlebars template helpers for `$timestampSeconds`, `$postmanTimestamp`, and raw `$queryParams`.
- Automatic pre-fill of query parameters, headers, and bodies in Test Client from imported example requests.

#### Partially Supported Behavior
- **Postman Scripts**: Pre-request scripts and test assertion blocks are parsed and classified during collection import. Dynamic Mock reports scripts in the import summary (pre-request as warnings, assertions as notices), but does not execute the Postman JavaScript `pm.*` runtime sandbox. Pre-request logic can be migrated to Dynamic Mock polyglot post-scripts.
- **Collection Variables**: Variables defined on imported collections are extracted and seeded into an environment variable set (`[CollectionName] Variables`), but runtime mutation via Postman scripts is not supported.

#### Intentionally Deferred Postman Features
- Cloud Postman workspace synchronization, monitor runs, Newman test runner lifecycle hooks, and Postman API key authentication.
- Postman dynamic mock server URL generation and cloud-based proxy routing.

#### Backward-Compatible Aliases
- **Native `{{$timestamp}}`**: Continues to emit 13-digit millisecond epoch timestamps for all existing routes and templates; Postman collections are translated at import time to `{{$timestampSeconds}}`.
- **Telemetry Aliases**: `executionMode` and `actionType` properties retain backward compatibility across `ExecutionEvent` and DTO mappings.

#### Security Invariants
- **Hardware-Grade Iframe Sandboxing**: Interactive HTML preview strictly enforces `sandbox="allow-scripts allow-forms allow-popups"` with zero `allow-same-origin` or `allow-top-navigation`.
- **Zero Double Execution**: Previews are rendered strictly in-memory via `srcdoc` without re-executing GET/POST requests against mock endpoints.
- **Sandbox CORS Segregation**: Sandboxed `Origin: null` requests remain restricted to `/mock/**` endpoints while all `/api/**` administrative routes remain secured behind JWT authorization walls.

---

## [0.6.1] — Interactive Simulation Experience (2026-10-30)

### Added

#### Interactive HTML Preview & Route Inspection
- **Sandboxed HTML Preview (`HtmlPreviewFrame`)** — Hardware-grade isolated preview rendering engine for HTML responses (`text/html`, `application/xhtml+xml`) using in-memory `srcdoc` DOM injection.
- **Zero Double Execution Guarantee** — Renders pre-captured execution response bodies strictly in memory without re-executing GET or POST requests against mock routes or upstream backends upon preview mount.
- **Dynamic Content Sizing** — Iframe auto-resizing via `MutationObserver` and postMessage document height signaling to eliminate internal scrollbars for responsive layouts.
- **Surgical URL Normalization Shim (`htmlPreviewShim`)** — Injects `<base>` targeting the mock workspace base URL, attaches `window.__DYNAMIC_MOCK_CONTEXT__`, and intercepts relative hyperlinks and form actions (`/mock/...`) to preserve preview continuity without mutating native browser APIs.
- **Route Details Dual Modes (`RouteDrawer`)** — Added `[Source]` and `[Preview]` tabs defaulting to `Source` view. The `Source` tab displays full raw template strings with syntax highlighting and horizontal scroll (no 300-char truncation); the `Preview` tab renders a static mock preview in a script-disabled sandbox (`sandbox=""`).
- **ResponsePanel Tab Switching** — Dynamic tab bar (`Preview | Body | Headers (N)`) defaulting to `Preview` whenever HTML response headers or HTML doctype/tags are detected, with graceful fallback to `Body | Headers (N)` for JSON, XML, or plain text.
- **HTML Download Support** — Direct `.html` export action button (`response.html`) in both `ResponsePanel` preview header and `RouteDrawer` preview mode.

#### Interactive Traffic Console Explorer
- **Interactive Filtering & Full-Text Search** — Real-time search across HTTP method, URL path, status code, response time, request ID, route ID, and query parameters.
- **Sub-Second Precision Event Timestamps** — Standardized event timestamps rendered with millisecond precision in `[HH:mm:ss.SSS]` format (e.g. `[13:01:32.421]`) alongside sub-millisecond execution duration metrics (e.g. `33.41ms`).
- **Expanded Request ID & Route ID Context** — Request IDs display with 18+ characters of context in collapsed rows (`Req: 1d72f8be-f1d2...`) and full 36-character UUIDs in detail panels, complete with one-click clipboard copy. Full Route IDs are similarly exposed in metadata headers.
- **Decoded Query Parameter Table** — Structured key-value breakdown table displaying parsed query strings with copy controls.
- **Deep Payload & Header Inspection** — Formatted JSON, XML, and plain-text body views for both incoming requests and outgoing responses, paired with complete HTTP header tables.
- **Expandable Accordion Rows** — Collapsible rows revealing detail tabs: `Query Parameters`, `Headers`, `Payload`, `Redirects`, `Webhooks`, and `Raw Event`.

#### Telemetry Enrichment, Redirect Tracking & Webhook Correlation
- **Telemetry Event Enrichment (`ExecutionEvent`)** — Comprehensive event payload capturing `fullRequestId`, `fullRouteId`, `queryParams`, `requestHeaders`, `requestBody`, `responseHeaders`, `responseBody`, `requestPayloadSize`, `responsePayloadSize`, `redirectChain`, and `webhookEvents`.
- **Step-by-Step Redirect Follower** — Controlled HTTP client recording intermediate redirect hops, HTTP status transitions (e.g. `302 Found → 302 Found → 200 OK`), target URLs, hop indices, and response headers.
- **Redirect Chain Visualization (`RedirectChainBanner`)** — Multi-hop visual stepper displaying redirect journeys, final destination status, and individual hop latency.
- **Asynchronous Webhook Telemetry Correlation** — Correlates dispatched mock webhooks to originating trigger events via `parentEventId`, capturing target URL, payload size, execution latency, and delivery status.
- **Payload Truncation & Binary Guardrails** — Safe 32 KB threshold protection preventing telemetry bloat while preserving diagnostic fidelity; binary payload detection (`application/octet-stream`, `image/*`) guards console UI against binary noise.
- **Permissive Mock Sandbox CORS** — Configured backend `WebConfig.java` to support sandboxed `Origin: null` cross-origin requests targeting `/mock/**` endpoints while keeping administrative `/api/**` routes strictly protected.

---

## [0.6.0] — Sprint 6: White Label Foundation & Enterprise Customization (2026-10-27)

### Added

#### Backend Domain & Hexagonal Architecture
- `OrganizationConfig.java` — Pure POJO domain entity (`SINGLETON_ID = "instance"`) representing single-tenant instance branding with `createDefault()` factory and zero framework annotations.
- `OrganizationConfigRepository.java` & `OrganizationConfigUseCase.java` — Outbound and inbound hexagonal ports for instance branding persistence and resolution.
- `OrganizationConfigMongoEntity.java`, `OrganizationConfigMapper.java`, `MongoOrganizationConfigRepository.java`, `OrganizationConfigRepositoryImpl.java` — MongoDB persistence adapter (`organization_config` collection).
- `BrandingEnvironmentProperties.java` — Binds `white-label.*` (`WHITE_LABEL_*` environment variables) with regex/size sanitization.
- `RedisBrandingCacheService.java` — Caches effective branding JSON under `config:branding:instance` (24h TTL) with atomic synchronous eviction and fail-open fallback to MongoDB.
- `OrganizationConfigService.java` — Resolves effective branding with deterministic precedence (`WHITE_LABEL_*` env overrides → MongoDB stored config → platform defaults).
- `BrandingBootstrapRunner.java` — Evicts stale Redis branding cache on boot and seeds initial `OrganizationConfig` when the collection is empty.
- `BrandingController.java`, `BrandingConfigRequest.java`, `BrandingConfigResponse.java` — REST controller at `/api/config/branding` (`GET` public, `PUT` and `POST /reset` restricted to administrators via `@PreAuthorize`).

#### Polyglot Scripting & Template Pipeline
- `DynamicRouteDispatcher.java` — Wired rendered Handlebars template output into `ScriptContext.setResponseBody(responseBody)` prior to executing post-scripts, enabling JavaScript and Python post-scripts to inspect, parse, and mutate template-generated payloads (`response.body`).
- `DynamicRouteDispatcherTest.java` — Added unit test `preHandle_postScriptCanReadAndMutateRenderedTemplate()` verifying post-script modification of rendered template fields.

#### Frontend Runtime, Zero-FOUC SSR & Admin UI
- `colorUtils.ts` — Hex-to-RGB conversion, ITU-R BT.601 luminance calculation (`0.299R + 0.587G + 0.114B`), automatic contrast foreground selection (`#0f172a` vs `#ffffff`), URL/Hex validators, and `:root` / `.dark` CSS variable synthesis.
- `types/branding.ts`, `hooks/useBranding.ts`, `context/BrandingContext.tsx`, `context/BrandingProvider.tsx` — Live React Query + DOM style injector (`#dynamic-branding-style`, document title, and favicon synchronization).
- `app/layout.tsx` — Server-side branding pre-fetch with 1500ms fail-safe timeout and inline `<style id="ssr-branding-theme">` injection in `<head>` for 0ms FOUC.
- `components/workspace/Header.tsx` & `app/login/page.tsx` — Dynamic product name, organization subtitle, custom logo with graceful `<Globe>` icon fallback on error, branded login background, and legal footer text.
- `components/workspace/BrandingSettingsModal.tsx` — Admin-only **Branding & Appearance** dialog with native color pickers, hex inputs, logo preview, live header/button contrast preview card, and confirmation-guarded **Reset to Defaults**.
- Validation & test suites: `OrganizationConfigMapperTest`, `OrganizationConfigServiceTest`, `BrandingControllerTest`, `colorUtils.test.ts`, `BrandingProvider.test.tsx`, `BrandingSettingsModal.test.tsx`.

---

## [0.5.1] — Sprint 5: First-Time User Experience & Workspace Productivity (2026-10-17)

### Added
- `HeadersEditor.tsx` — Table and Raw/Bulk multiline headers editor with RFC 7230 validation and autocomplete.
- `BankingSandboxModal.tsx` — Optional Banking Demo Sandbox overview and 1-click loader.
- `TestClientView.tsx` & `MockDefinitionView.tsx` — Segregated RequestPanel views with "Copy as cURL", `ContextualTooltip`, `RichEmptyState`, and `KeyboardShortcutsModal`.

---

## [0.4.0] — Sprint 4: Authentication Foundation & Multi-User Identity (2026-10-15)

### Added

#### Authentication Foundation
- `POST /api/auth/login` — JWT-based authentication endpoint. Returns an HS256-signed token (8-hour lifetime by default).
- `POST /api/auth/register` — Environment-variable-gated (`APP_AUTH_ALLOW_REGISTRATION`) self-service registration. Disabled by default.
- `GET /api/auth/me` — Returns the authenticated user's profile (id, username, email).
- `UserBootstrapRunner` — Seeds an initial administrator (`admin/admin`) on first boot when the `users` MongoDB collection is empty. Emits a loud warning if default credentials are still in use.
- `User.java` domain entity — Pure Lombok-only domain entity (zero Spring Data / Jakarta / Jackson annotations) per Clean Architecture.
- `UserRepository.java` — Output port interface for user persistence operations.
- `JwtTokenService` — HS256 token issuance, validation, and claims extraction. Validates that the JWT secret is ≥ 32 characters on startup.
- `JwtAuthenticationFilter` — `OncePerRequestFilter` that parses `Authorization: Bearer <token>` and populates the `SecurityContext`.
- `SecurityConfig` — Stateless Spring Security filter chain: `/mock/**`, `/api/auth/**`, `/ws-traffic/**` are fully public; `/api/**` requires a valid JWT.
- `PasswordEncoderConfig` — BCrypt strength 12 password hashing.
- MongoDB `UserMongoEntity`, `UserMapper`, `MongoUserRepository`, and `UserRepositoryImpl` — Clean Architecture-aligned persistence adapter.
- `AuthContext.tsx` — React context providing in-memory JWT token management (never persisted to `localStorage` or `sessionStorage`).
- `useAuth.ts` — Convenience hook for consuming `AuthContext`.
- `frontend/src/app/login/page.tsx` — Themed login page matching the existing dark/light design system with error handling and loading states.
- `AuthGuard.tsx` — Client-side route guard; redirects unauthenticated users to `/login`.

#### Scenario Session Namespacing
- `X-Session-ID` request header support in `DynamicRouteDispatcher` — scopes Redis script state to `dynamic-mock:script-state:session:{sessionId}`.
- `ScenarioService` namespacing overloads (`getCurrentState`, `setCurrentState`, `getExecutionCount`, `processTransition`, etc.) — full backward-compatible shared fallback preserved for Banking Sandbox tutorial.
- `ScriptContext.sessionId` field — carries session scope through the GraalVM execution pipeline.

#### Security Test Suite
- `JwtTokenServiceTest` — token generation, validation, tampering, and weak-secret startup failure.
- `ApiKeyAuthFilterTest` — Bearer short-circuit and constant-time key validation.
- `AuthControllerTest` — MockMvc coverage of all auth endpoints.
- `UserRepositoryImplTest` — MongoDB persistence integration tests.
- `ScenarioServiceNamespacingTest` — concurrent session isolation verification.

### Changed
- `ApiKeyAuthFilter` — demoted to filter order 100; short-circuits immediately when `Authorization: Bearer` is present, delegating to Spring Security's JWT filter chain.
- `DemoDataLoader` — resolves owner ID dynamically from the admin user in MongoDB instead of using the hardcoded string `"default-user"`.
- `PostmanImportService` — parameterized user ID ownership; backward-compatible constructor retained.
- `WebConfig` — CORS `allowedOrigins` now configurable via `APP_CORS_ALLOWED_ORIGINS` environment variable.
- `Header.tsx` — displays authenticated username with an initials avatar and a Sign Out dropdown; `"default-user"` string eliminated.
- `Sidebar.tsx` — uses authenticated user's ID for collection scoping; `"default-user"` string eliminated.
- `providers.tsx` — `<AuthProvider>` added as the outermost context in the provider hierarchy.
- `apiClient.ts` — request interceptor attaches `Authorization: Bearer <token>`; response interceptor redirects to `/login` on 401.
- `docker-compose.yml` — added `APP_AUTH_JWT_SECRET`, `ADMIN_PASSWORD`, `APP_AUTH_ALLOW_REGISTRATION`, `APP_CORS_ALLOWED_ORIGINS` to `mock-server`; removed `NEXT_PUBLIC_API_KEY` from `frontend`.
- `.env.example` files updated with authentication environment variable documentation.

---

## [Unreleased] — Sprint 3 (2026-10-06)

### Added

#### Reference Banking Sandbox
- `banking-sandbox.json` — Pre-configured Sandbox bundle containing an active ISO8583 Simulator endpoint and a stateful "Card Payment Flow" scenario (INITIAL -> AUTHORIZED -> CAPTURED -> REVERSED -> REFUNDED).
- `POST /api/sandbox/load` — API endpoint in `SandboxController` that idempotent-loads the Sandbox bundle into the active environment without needing server restart.
- First-Time User Experience — Frontend `RequestPanel` empty state updated to include a prominent **"Load Reference Banking Sandbox (5-Minute Quickstart)"** button.
- `BANKING_SANDBOX_TUTORIAL.md` — Step-by-step tutorial on how to use the sandbox to observe state transitions.
- SVG placeholders for docs/assets.

### Changed
- `README.md` — Repositioned project as an Open Source Enterprise Integration Simulation Platform, removed generic mock server language, added sandbox quickstart.

---

## [Unreleased] — Sprint 2 (2026-10-04)

### Added

#### ISO8583 Message Simulator
- `Iso8583PackagerFactory` — shared packager resolution component (custom XML → bundled → ISO87A fallback); extracted from `Iso8583Server` for reuse
- `POST /api/iso8583/endpoints/{id}/simulate` — backend TCP proxy endpoint; packs request, opens TCP to mock server, unpacks response, returns decoded JSON
- `Iso8583SimulatorService` — service layer for simulator with structured error types: `ENDPOINT_INACTIVE`, `CONNECTION_REFUSED`, `TIMEOUT`, `PACK_ERROR`, `UNPACK_ERROR`
- `Iso8583SimulateRequest` / `Iso8583SimulateResponse` — new DTOs for simulator API
- Frontend Simulator Panel inside `ProtocolEditor.tsx`:
  - "Simulator" tab alongside existing "Configuration" tab
  - MTI selector (all common MTIs with descriptions)
  - Field input table with ISO8583 data dictionary labels and format hints
  - Pre-built templates for 0100, 0200, 0420
  - Send button with loading state
  - Response viewer: decoded fields table with dictionary labels, Field 39 response code with human-readable description, copy-to-clipboard
  - Raw hex toggle for request and response
  - Structured error display per error type
- `useIso8583Simulator` — React Query mutation hook for simulator API calls

#### Scenario State Inspector
- `GET /api/scenarios/{id}/state/details` — new endpoint returning `currentState`, `initialState`, `executionCount`, `stateVariables`, `active`
- `ScenarioService.getExecutionCount()` — reads execution count from Redis
- `ScenarioService.getStateVariables()` — reads global script state hash from Redis
- `ScenarioStateInspector` component embedded in `ScenarioEditor.tsx`:
  - Live current state display with initial-state indicator
  - Execution count
  - Script variables table (Redis hash entries)
  - 5-second auto-refresh via `useQuery` `refetchInterval`
  - Manual refresh button
  - Loading and empty-state handling
- `useScenarioStateDetails` — React Query polling hook (5s interval, 1s staleTime)

#### Tests
- `Iso8583SimulatorServiceTest` — unit tests covering inactive endpoint, connection refused, missing endpoint, packager factory delegation

---

## [Unreleased] — Sprint 1 (2026-10-01)


### Added

#### CI/CD
- `.github/workflows/ci.yml` — GitHub Actions pipeline (backend Gradle/Java 21 + frontend Node 20)
- `backend/gradle/wrapper/gradle-wrapper.jar` — missing from repository, added

#### Licensing
- `LICENSE` — Apache License 2.0
- `THIRD_PARTY_LICENSES.md` — Third-party attributions, including jPOS AGPL analysis

#### ISO8583 Data Dictionary
- `frontend/src/data/iso8583-fields.json` — Complete 128-field ISO 8583:1987 data dictionary with MTI descriptions and common response codes
- `ProtocolEditor.tsx` — `FieldNumberInput` with search/autocomplete (field number, name, or abbreviation)
- `ProtocolEditor.tsx` — Response fields and matchers now show labeled field names as you type
- `ProtocolEditor.tsx` — MTI dropdown built from dictionary (all 24 standard MTIs)

#### ISO8583 Packager Upload
- `Iso8583Endpoint` — added `packagerXmlContent` and `packagerName` fields (MongoDB-backed inline storage)
- `Iso8583EndpointMongoEntity` + `Iso8583EndpointMapper` — propagate new fields
- `Iso8583EndpointResponse` — exposes `packagerName` and `hasCustomPackager`
- `Iso8583Controller` — `POST /api/iso8583/endpoints/{id}/packager` and `DELETE /api/iso8583/endpoints/{id}/packager`
- `Iso8583Service` — `uploadPackager()` (validate + persist + hot-reload) and `removePackager()`
- `Iso8583Editor` — Packager Definition section with upload, filename display, Reset to Default

### Fixed
- `Iso8583Server.createPackager()` — **Critical bug**: always loaded bundled default, ignoring `endpoint.packagerConfig`. Now correctly uses uploaded `packagerXmlContent` first.

### Changed
- `README.md` — Enterprise positioning, CI/license badges, restructured features

---

## [Pre-Sprint 1]

### Added
- **Frontend**: Vitest and React Testing Library setup.
- **Frontend**: `useScenarios.test.tsx` containing basic tests for the scenarios API hook.
- **Frontend**: A view toggler in the Sidebar to switch the main content area between "Routes" and "Scenarios".
- **Frontend**: Basic error handling and a "New" scenario creation button within `ScenarioEditor.tsx`.
- **Frontend**: Added `sonner` for toast notifications and implemented `DialogContext` for custom modal dialogs.

### Fixed
- **Frontend**: Recovered `ScenarioEditor.tsx` from an orphaned/dead state by integrating it properly into `WorkspaceLayout.tsx` using an `activeView` context state.
- **Frontend**: Implemented full `EnvironmentManagerModal.tsx` allowing for Environment CRUD, variables editing, and cloning.
- **Frontend**: Enhanced Collection Management in `Sidebar.tsx` to support renaming, cloning, and deleting collections.
- **Frontend**: Added ability to move routes between collections via a contextual UI.

### Changed
- **Frontend**: Updated `Header.tsx` to include an Environment Settings button to open the Environment Manager Modal.
- **Frontend**: Replaced all native blocking dialogs (`alert()`, `confirm()`, `prompt()`) with custom UI modals and toasts.

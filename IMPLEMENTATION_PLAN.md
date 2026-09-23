# Dynamic Mock API Server — Implementation Plan

> **Purpose:** This document contains every bug, gap, and enhancement identified during the feasibility audit. Any AI agent can pick up a task, execute it, and verify it independently.
>
> **Last Updated:** 2026-07-04
>
> **How to use:** Tasks are ordered by priority. Each task includes the exact file(s), what is wrong, what to change, and how to verify. Work through Section 1 first (Critical Bugs), then Section 2 (Feature Completions), then Section 3 (Enhancements).

---

## Table of Contents

- [Section 1 — Critical Bugs (Must Fix)](#section-1--critical-bugs-must-fix)
- [Section 2 — Feature Completions (Should Fix)](#section-2--feature-completions-should-fix)
- [Section 3 — Enhancements (Nice to Have)](#section-3--enhancements-nice-to-have)
- [Section 4 — Verification Checklist](#section-4--verification-checklist)

---

## Section 1 — Critical Bugs (Must Fix)

These are bugs that prevent the application from working correctly. Fix them first.

---

### Task 1.1 — Fix ISO8583 Q2 Class Reference Mismatch

**Priority:** CRITICAL
**Category:** Backend — ISO8583
**Estimated Effort:** 5 minutes

**Problem:**
`Q2ServerManager.generateServerXml()` generates jPOS XML that references the old package path for `DynamicMockRequestListener`. After the Clean Architecture refactoring, the class moved but the XML template was never updated. When Q2 deploys an ISO8583 server, it will fail at runtime because it cannot find the listener class.

**Files to modify:**
- `backend/src/main/java/com/dynamicmock/adapter/out/protocol/iso8583/Q2ServerManager.java`

**What is wrong:**
Line ~210 in `generateServerXml()` contains this string:
```
com.dynamicmock.core.protocol.iso8583.DynamicMockRequestListener
```

**What to change:**
Replace with the correct current package path:
```
com.dynamicmock.adapter.out.protocol.iso8583.DynamicMockRequestListener
```

**Verification:**
1. Run `cd backend && ./gradlew compileJava` — must succeed
2. Run `cd backend && ./gradlew test --tests 'com.dynamicmock.service.Iso8583ServiceTest'` — must pass
3. Grep for the old package path: `grep -r "com.dynamicmock.core.protocol" backend/src/` — must return zero results

---

### Task 1.2 — Fix DynamicRouteDispatcherTest NullPointerException (4 failing tests)

**Priority:** CRITICAL
**Category:** Backend — Tests
**Estimated Effort:** 10 minutes

**Problem:**
`DynamicRouteDispatcher` was updated to include two new constructor-injected dependencies:
- `TrafficLogger trafficLogger`
- `WorkspaceEnvironmentService environmentService`

But `DynamicRouteDispatcherTest` was never updated to mock these fields. Mockito's `@InjectMocks` injects `null` for unmocked fields, causing `NullPointerException` in 4 of 6 tests.

**Files to modify:**
- `backend/src/test/java/com/dynamicmock/core/dispatcher/DynamicRouteDispatcherTest.java`

**What to change:**
Add these two mock fields to the test class, alongside the existing mocks:

```java
@Mock
private com.dynamicmock.application.service.TrafficLogger trafficLogger;

@Mock
private com.dynamicmock.application.service.WorkspaceEnvironmentService environmentService;
```

That is all that is needed. `@InjectMocks` will automatically inject them into `DynamicRouteDispatcher`.

**Verification:**
1. Run `cd backend && ./gradlew test --tests 'com.dynamicmock.core.dispatcher.DynamicRouteDispatcherTest'` — all 6 tests must pass
2. Run `cd backend && ./gradlew test --tests 'com.dynamicmock.service.*' --tests 'com.dynamicmock.core.*' --tests 'com.dynamicmock.controller.*' --tests 'com.dynamicmock.config.*'` — all unit tests must pass (integration tests excluded)

---

### Task 1.3 — Fix ISO8583 Standalone Mode `processMessage()` Stub

**Priority:** CRITICAL
**Category:** Backend — ISO8583
**Estimated Effort:** 30 minutes

**Problem:**
`Iso8583Server.java` has two modes: Q2 (full implementation via `DynamicMockRequestListener`) and Standalone (raw socket fallback). The standalone mode's `processMessage()` method is a **stub** — it ignores the endpoint's mock configurations entirely and always returns a generic response with `field 39 = "00"`.

This means ISO8583 only works when Q2 is enabled. If Q2 fails or is disabled, the standalone fallback is non-functional.

**Files to modify:**
- `backend/src/main/java/com/dynamicmock/adapter/out/protocol/iso8583/Iso8583Server.java`

**What is wrong:**
Lines 267–273, the `processMessage` method:
```java
private ISOMsg processMessage(ISOMsg request, Iso8583Endpoint endpoint) throws ISOException {
    // Placeholder for new message processing logic using endpoint.getMocks()
    String mti = request.getMTI();
    ISOMsg response = (ISOMsg) request.clone();
    response.setMTI(getResponseMti(mti));
    response.set(39, "00");
    return response;
}
```

**What to change:**
Replace this stub with logic that mirrors what `DynamicMockRequestListener.processMessage()` already does. Specifically:

1. Get the endpoint's mocks list via `endpoint.getMocks()`
2. Match incoming message by MTI (field `mti` on each `Iso8583Mock`)
3. For each mock with matching MTI, check field matchers using regex (same logic as `DynamicMockRequestListener.matchesConditions()`)
4. Sort candidates by priority (descending)
5. Pick the first matching mock (or fall back to catch-all with no matchers)
6. If a mock has `scriptEnabled` and a `script`, execute it via `ScriptEngine`
7. Apply response field templates via `ResponseTemplateEngine`
8. Set the response code from `mock.getResponseCode()` (default "00")
9. Set the response MTI from `mock.getResponseMti()` or compute it from request MTI

**Reference implementation:**
Copy the logic from `DynamicMockRequestListener.processMessage()` (lines ~185–210 of that file) and adapt it to work with `Iso8583Endpoint` and `Iso8583Mock` domain objects instead of the Q2 source.

**Additional changes needed in `Iso8583Server.java`:**
- The `acceptConnections` method needs to call `processMessage(request, endpoint)` — this is already done
- Add a `findMatchingMock()` private method (same logic as `DynamicMockRequestListener.findMatchingMock()`)
- Add a `matchesConditions()` private method (same logic as `DynamicMockRequestListener.matchesConditions()`)
- Import `ScriptContext` and `ScriptEngine` (already injected via constructor)

**Verification:**
1. Run `cd backend && ./gradlew compileJava` — must succeed
2. Run `cd backend && ./gradlew test --tests 'com.dynamicmock.service.Iso8583ServiceTest'` — must pass
3. Manual test: start the app with `q2.enabled=false`, create an ISO8583 endpoint with mocks, activate it, connect with a TCP client, send an ISO8583 message — verify the response matches the mock configuration

---

## Section 2 — Feature Completions (Should Fix)

These are features that are partially implemented. They work but have incomplete frontend wiring or missing integration.

---

### Task 2.1 — Wire Frontend `KeyValueEditor` to Save/Load Route Data

**Priority:** HIGH
**Category:** Frontend — RequestPanel
**Estimated Effort:** 45 minutes

**Problem:**
The `KeyValueEditor` component in `RequestPanel.tsx` renders static input fields for query parameters and response headers, but the data is never saved back to the route or loaded from the route. It is a visual placeholder.

**Files to modify:**
- `frontend/src/components/workspace/RequestPanel.tsx` (the `KeyValueEditor` function at the bottom)

**What to change:**

1. **Accept props:** `KeyValueEditor` should accept `entries: Record<string, string>` and `onChange: (entries: Record<string, string>) => void` as props
2. **Manage state internally:** Initialize local state from the `entries` prop, render a row per key-value pair
3. **Add Row:** The "Add Row" button should append a new empty `{ key: '', value: '' }` entry
4. **Delete Row:** The trash icon should remove the entry by index
5. **Bidirectional binding:** When any input changes, call `onChange` with the updated map
6. **Wire in RequestPanel:** Pass `activeRoute.queryParams` / `activeRoute.responseHeaders` as the `entries` prop, and update `handleSave` to include them in the saved route

**Backend note:** The `MockRoute` entity already has `Map<String, String> queryParams` and `Map<String, String> responseHeaders` fields. The `CreateRouteRequest` / `UpdateRouteRequest` DTOs already accept these. No backend changes needed.

**Verification:**
1. Run `cd frontend && npm run build` — must succeed with no TypeScript errors
2. Manual test: open the UI, select a route, add query parameters in the Params tab, save, refresh — verify they persist
3. Manual test: add response headers in the Headers tab, save, send a request — verify the headers appear in the response

---

### Task 2.2 — Wire Frontend `MatcherEditor` to Save/Load Route Data

**Priority:** HIGH
**Category:** Frontend — RequestPanel
**Estimated Effort:** 45 minutes

**Problem:**
The `MatcherEditor` component shows three static cards (Headers, Query Params, Body matchers) but the "Add Matcher" buttons are non-functional and no data is persisted.

**Files to modify:**
- `frontend/src/components/workspace/RequestPanel.tsx` (the `MatcherEditor` function)

**What to change:**

1. **Accept props:** `MatcherEditor` should accept `matchers: { headers?: Record<string, string>, queryParams?: Record<string, string>, body?: Record<string, string> }` and `onChange` callback
2. **Each matcher type card:** Render a list of key-value pairs (key = field name, value = regex pattern)
3. **Add Matcher button:** Appends a new empty matcher row to the appropriate type
4. **Delete matcher:** Removes by index
5. **Wire in RequestPanel:** Load `activeRoute.matchers` into the editor, save changes back via `handleSave`

**Backend note:** The `MockRoute` entity has a `Map<String, String> matchers` field. The `RequestMatcher` in the dispatcher already supports header and query param regex matching. No backend changes needed.

**Verification:**
1. Run `cd frontend && npm run build` — must succeed
2. Manual test: add a header matcher (e.g., `Authorization` = `Bearer .*`), save, send a request without the header — verify 404 is returned
3. Manual test: add a query param matcher (e.g., `page` = `\\d+`), save, send a request with `?page=abc` — verify 404 is returned

---

### Task 2.3 — Add Protocol-Specific Editors (GraphQL, gRPC, ISO8583)

**Priority:** MEDIUM
**Category:** Frontend — ProtocolEditor
**Estimated Effort:** 1–2 hours

**Problem:**
`ProtocolEditor.tsx` renders the raw JSON of an endpoint in a Monaco editor. This is usable but not user-friendly. Each protocol needs a dedicated form editor.

**Files to modify:**
- `frontend/src/components/workspace/protocols/ProtocolEditor.tsx`

**What to change:**

Create three sub-components inside `ProtocolEditor.tsx` (or as separate files in `protocols/`):

1. **`GraphQLEditor`** — Form with:
   - Schema textarea (multiline, syntax highlighting)
   - Resolver list: each resolver has operationType (Query/Mutation), fieldName, responseTemplate, script, delayMs
   - Add/remove resolvers
   - Save button that calls `PUT /api/graphql/endpoints/{id}`

2. **`GrpcEditor`** — Form with:
   - Service name, port, proto schema textarea
   - Methods list: each method has methodName, methodType (UNARY/SERVER_STREAMING/etc.), responseTemplate, delayMs, statusCode, errorMessage
   - Add/remove methods
   - Save button that calls `PUT /api/grpc/endpoints/{id}`

3. **`Iso8583Editor`** — Form with:
   - Port, isolated port toggle, header length type, encoding
   - Mocks list: each mock has name, mti (dropdown), matchers, responseCode, responseFields, priority, script
   - Interceptor script section
   - Custom XML toggle (advanced mode)
   - Save button that calls `PUT /api/iso8583/endpoints/{id}`

**Verification:**
1. Run `cd frontend && npm run build` — must succeed
2. Manual test: create a GraphQL endpoint via the API, open it in the UI — verify the schema and resolvers render in form fields
3. Same for gRPC and ISO8583

---

### Task 2.4 — Add Multi-Protocol Traffic to Live Console

**Priority:** MEDIUM
**Category:** Backend + Frontend — Traffic
**Estimated Effort:** 1 hour

**Problem:**
The `LiveTrafficPanel` only shows HTTP traffic logged by `DynamicRouteDispatcher`. gRPC, GraphQL, and ISO8583 traffic are not streamed to the console.

**Files to modify:**
- `backend/src/main/java/com/dynamicmock/application/service/TrafficLogger.java` — add `protocol` field to `ExecutionEvent` (already present, verify)
- `backend/src/main/java/com/dynamicmock/adapter/out/protocol/grpc/DynamicGrpcServer.java` — add `TrafficLogger` call in `handleUnary()`, `handleServerStreaming()`, etc.
- `backend/src/main/java/com/dynamicmock/application/service/GraphQLService.java` — add `TrafficLogger` call in `execute()`
- `backend/src/main/java/com/dynamicmock/adapter/out/protocol/iso8583/DynamicMockRequestListener.java` — add `TrafficLogger` call in `process()`
- `frontend/src/components/workspace/LiveTrafficPanel.tsx` — add protocol badge (color-coded) next to each event

**What to change in each file:**

1. **DynamicGrpcServer.java:**
   - Inject `TrafficLogger` via constructor
   - In `handleUnary()`, after successful response, call `trafficLogger.log(ExecutionEvent.builder().protocol("gRPC").method("UNARY").path(serviceName + "/" + methodName).status(200).durationMs(elapsed).build())`
   - Same for other handler methods

2. **GraphQLService.java:**
   - Inject `TrafficLogger` via constructor
   - In `execute()`, wrap the execution in a timer, log after completion

3. **DynamicMockRequestListener.java:**
   - Get `TrafficLogger` from `applicationContext` in `setConfiguration()`
   - In `process()`, after sending response, log the event with `protocol("ISO8583")`

4. **LiveTrafficPanel.tsx:**
   - Add a colored badge before the method: `[HTTP]` blue, `[gRPC]` green, `[ISO8583]` yellow, `[GQL]` purple

**Verification:**
1. Run `cd backend && ./gradlew compileJava` — must succeed
2. Run `cd frontend && npm run build` — must succeed
3. Manual test: send HTTP, gRPC, GraphQL, and ISO8583 traffic — verify all appear in the live console with protocol badges

---

## Section 3 — Enhancements (Nice to Have)

These are polish items that improve the user experience but are not required for core functionality.

---

### Task 3.1 — Visual Scenario State Diagram

**Priority:** LOW
**Category:** Frontend — ScenarioEditor
**Estimated Effort:** 2–3 hours

**Problem:**
`ScenarioEditor.tsx` lists scenarios as static cards showing the initial state. There is no visual representation of the state machine (states, transitions, conditions).

**Files to modify:**
- `frontend/src/components/workspace/scenarios/ScenarioEditor.tsx`

**What to change:**
1. Install a flowchart library (e.g., `reactflow` or `@xyflow/react`)
2. Render each scenario as a flowchart:
   - Nodes = states (with name, description, response status)
   - Edges = transitions (with condition label, priority)
   - Highlight the current state
3. Allow clicking a state to edit its response template
4. Allow clicking a transition to edit its condition

**Verification:**
1. Run `cd frontend && npm run build` — must succeed
2. Manual test: create a scenario with 3 states and transitions — verify the flowchart renders correctly

---

### Task 3.2 — In-Browser Script Syntax Validation

**Priority:** LOW
**Category:** Frontend — CodeEditor
**Estimated Effort:** 1 hour

**Problem:**
The Monaco editor in the Scripts tab allows typing JavaScript/Python but does not validate syntax before saving. Users can save scripts that fail at runtime.

**Files to modify:**
- `frontend/src/components/workspace/CodeEditor.tsx`

**What to change:**
1. Configure Monaco's `monaco.languages.typescript.javascriptDefaults` for JS validation
2. For Python, use a linting marker approach (or disable validation if too complex)
3. Add a "Validate" button that sends the script to a new backend endpoint `POST /api/scripts/validate` that executes it in a sandboxed GraalVM context and returns success/error
4. Show validation errors inline in the editor

**Backend addition (optional):**
- Add `POST /api/scripts/validate` endpoint in `RouteController.java` that calls `ScriptEngine.execute()` in a try/catch and returns the result

**Verification:**
1. Run `cd frontend && npm run build` — must succeed
2. Run `cd backend && ./gradlew compileJava` — must succeed (if backend endpoint added)
3. Manual test: type invalid JS in the scripts tab — verify syntax errors are highlighted

---

### Task 3.3 — Fix Full Test Suite Execution

**Priority:** MEDIUM
**Category:** Backend — Tests / Build
**Estimated Effort:** 1 hour

**Problem:**
When running `cd backend && ./gradlew test` (all 22 test classes), the Gradle test worker process crashes and all tests report "Could not execute test class". Individual test classes pass fine. This is likely caused by:
1. Testcontainers starting multiple Docker containers simultaneously (resource contention)
2. GraalVM polyglot engine initialization conflicts between test forks
3. Redis/MongoDB port conflicts between concurrent test forks

**Files to examine:**
- `backend/build.gradle` — test task configuration
- `backend/src/test/resources/testcontainers.properties`
- All integration test files in `backend/src/test/java/com/dynamicmock/integration/`

**What to change:**

Option A (Recommended): **Force single-threaded test execution**
```groovy
// In build.gradle, inside tasks.named('test') block:
maxParallelForks = 1
```

Option B: **Separate integration tests from unit tests**
```groovy
// Create a separate task for integration tests
task integrationTest(type: Test) {
    include '**/integration/**'
    maxParallelForks = 1
}
// Exclude integration tests from the default 'test' task
test {
    exclude '**/integration/**'
}
```

Option C: **Add `@Tag("integration")` to integration tests** and use JUnit 5 tag filtering

**Verification:**
1. Run `cd backend && ./gradlew test` — all unit tests must pass
2. Run `cd backend && ./gradlew integrationTest` — all integration tests must pass (Docker required)
3. No test worker process crashes

---

### Task 3.4 — Add `@Tag("integration")` to Integration Tests

**Priority:** LOW
**Category:** Backend — Tests
**Estimated Effort:** 15 minutes

**Problem:**
Integration tests (`RouteIntegrationTest`, `ScriptExecutionIntegrationTest`) require Docker for Testcontainers but are not tagged, so they run (and fail) alongside unit tests.

**Files to modify:**
- `backend/src/test/java/com/dynamicmock/integration/RouteIntegrationTest.java`
- `backend/src/test/java/com/dynamicmock/integration/ScriptExecutionIntegrationTest.java`

**What to change:**
Add `@org.junit.jupiter.api.Tag("integration")` to both test classes.

In `build.gradle`, exclude integration tests from the default test task:
```groovy
test {
    useJUnitPlatform {
        excludeTags 'integration'
    }
}
```

**Verification:**
1. Run `cd backend && ./gradlew test` — unit tests pass, integration tests are skipped
2. Run `cd backend && ./gradlew test -DincludeTag=integration` — integration tests run (Docker required)

---

## Section 4 — Verification Checklist

After completing all tasks, run this full verification:

### Backend
```bash
cd backend

# 1. Compilation
./gradlew compileJava

# 2. Unit tests (no Docker needed)
./gradlew test

# 3. Check no old package references remain
grep -r "com.dynamicmock.core" src/ --include="*.java" --include="*.xml"

# 4. Check no stub processMessage remains
grep -n "Placeholder for new message" src/main/java/com/dynamicmock/adapter/out/protocol/iso8583/Iso8583Server.java
```

### Frontend
```bash
cd frontend

# 1. TypeScript compilation
npm run build

# 2. Lint (if configured)
npm run lint
```

### Docker
```bash
# Full stack test (requires Docker)
docker compose up --build

# Test HTTP mock
curl http://localhost:8080/mock/hello?name=Test

# Test scenario flow
curl http://localhost:8080/mock/orders/demo
curl "http://localhost:8080/mock/orders/demo?action=pay"
curl "http://localhost:8080/mock/orders/demo?action=ship"

# Test live traffic (verify WebSocket shows events)
# Open http://localhost:8080/ in browser, check Live Traffic Console
```

---

## Task Summary

| # | Task | Priority | Effort | Section |
|---|------|----------|--------|---------|
| 1.1 | Fix ISO8583 Q2 class reference | CRITICAL | 5 min | Bug |
| 1.2 | Fix DynamicRouteDispatcherTest NPE | CRITICAL | 10 min | Bug |
| 1.3 | Fix ISO8583 standalone processMessage stub | CRITICAL | 30 min | Bug |
| 2.1 | Wire KeyValueEditor save/load | HIGH | 45 min | Feature |
| 2.2 | Wire MatcherEditor save/load | HIGH | 45 min | Feature |
| 2.3 | Add protocol-specific editors | MEDIUM | 1–2 hrs | Feature |
| 2.4 | Add multi-protocol traffic to console | MEDIUM | 1 hr | Feature |
| 3.1 | Visual scenario state diagram | LOW | 2–3 hrs | Enhancement |
| 3.2 | In-browser script validation | LOW | 1 hr | Enhancement |
| 3.3 | Fix full test suite execution | MEDIUM | 1 hr | Build |
| 3.4 | Add @Tag("integration") to tests | LOW | 15 min | Build |

**Total estimated effort:** ~7–9 hours of focused work.

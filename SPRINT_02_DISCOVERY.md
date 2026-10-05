# Sprint 2 Discovery — ISO8583 User Experience Completion

**Date**: 2026-10-03  
**Sprint**: 2  
**Goal**: ISO8583 User Experience Completion  
**Deliverables**: ISO8583 Message Simulator, Scenario State Inspector  
**Status**: DISCOVERY COMPLETE — Ready for implementation

**Governing Documents**:
- `MASTER_PRODUCT_ACTION_PLAN.md` (Sprints 6 & 7 → re-sequenced to Sprint 2 per user authority)
- `MASTER_PRODUCT_ACTION_PLAN_REVIEW.md`
- `AI_GOVERNANCE.md`
- `ROADMAP_AUTHORITY.md`

---

## DELIVERABLE 1: ISO8583 Message Simulator

### 1.1 Current State

The backend ISO8583 infrastructure is fully functional:

- **`Iso8583Server.java`** ([source](backend/src/main/java/com/dynamicmock/adapter/out/protocol/iso8583/Iso8583Server.java)):
  - Accepts TCP connections via raw sockets (standalone) or Q2 framework
  - Unpacks inbound messages using `ISOPackager`
  - Routes to mocks by MTI + field matchers
  - Supports custom uploaded packagers, interceptor scripts, scenario-driven response overrides
  - Header types: `2BYTE` (default), `4BYTE`, `NONE`

- **`Iso8583Controller.java`** ([source](backend/src/main/java/com/dynamicmock/adapter/in/web/Iso8583Controller.java)):
  - Full CRUD for endpoints: `GET/POST/PUT/DELETE /api/iso8583/endpoints`
  - Activate/Deactivate: `POST /api/iso8583/endpoints/{id}/activate|deactivate`
  - Packager upload: `POST/DELETE /api/iso8583/endpoints/{id}/packager`

- **Frontend `ProtocolEditor.tsx`** ([source](frontend/src/components/workspace/protocols/ProtocolEditor.tsx)):
  - 822 lines; already contains the Iso8583Editor with endpoint config, mock configuration, packager upload, and data dictionary integration
  - Mocks are configured inline with MTI, matchers, response fields, scripts, scenario binding

- **No simulator exists.** Users can only configure mocks. They cannot send test messages from the browser.

### 1.2 Existing Reusable Components

| Component | Reuse Value | Notes |
|---|---|---|
| `iso8583-fields.json` (128-field data dictionary) | **HIGH** — provides field labels, formats, and descriptions for the simulator form | Already imported in `ProtocolEditor.tsx` |
| `Iso8583Server.createPackager()` | **HIGH** — determines which packager to use for encoding the simulator's outbound message | Custom → bundled → ISO87A fallback chain |
| `Iso8583Server.handleConnection()` and TCP framing (`readLength`/`writeLength`) | **REFERENCE** — shows the exact framing protocol the simulator must conform to | Header type varies per endpoint (`2BYTE`, `4BYTE`, `NONE`) |
| `Iso8583Server.processMessage()` | **REFERENCE** — shows mock matching, scenario execution, template rendering | Helps verify simulator test results |
| `apiClient.ts` (Axios with API key interceptor) | **HIGH** — all frontend-to-backend calls must use this | Base URL: `/api` |

### 1.3 Missing Components

#### Backend
1. **Simulator Proxy Endpoint** — `POST /api/iso8583/endpoints/{id}/simulate`
   - Accepts: `{ mti: "0100", fields: { "2": "4111...", "3": "000000", ... } }`
   - Internally: creates `ISOMsg`, packs with the endpoint's packager, opens TCP socket to `localhost:{port}`, sends with correct header framing, reads response, unpacks, returns decoded fields
   - Returns: `{ responseMti: "0110", responseFields: { "39": "00", ... }, rawHex: "...", latencyMs: 42 }`
   - **Why backend proxy?** The browser cannot open raw TCP sockets. The backend must relay.

#### Frontend
2. **Simulator Panel** — new tab or section within `Iso8583Editor`
   - MTI selector (dropdown with `mtiDescriptions` from the data dictionary)
   - Field input table: field number, label (from dictionary), value, format hint
   - "Send" button → calls proxy endpoint
   - Response display: decoded fields with dictionary labels, response code explanation, raw hex toggle, latency
3. **useIso8583Simulator hook** — mutation for calling the proxy endpoint

### 1.4 Architecture Decision: Backend Proxy

**Question**: Should the browser call the backend REST API which then relays TCP, or should the browser somehow connect directly?

**Answer: Backend proxy.** This is the only viable approach because:

1. Browsers cannot open raw TCP sockets (no WebSocket upgrade possible — ISO8583 is a binary protocol on raw TCP)
2. The backend already has the packager (custom or default) loaded for the active endpoint
3. The backend can enforce the correct header framing (`2BYTE`/`4BYTE`/`NONE`)
4. Error handling is cleaner — the backend can catch `ISOException`, connection refused, timeout, etc. and return structured JSON errors

**Architecture:**
```
Browser → POST /api/iso8583/endpoints/{id}/simulate (JSON)
         → Backend creates ISOMsg, packs it
         → Backend opens TCP to localhost:{endpoint.port}
         → Backend sends with correct header framing
         → Backend reads response, unpacks
         → Backend returns decoded response as JSON
```

### 1.5 Custom Packager Impact on Simulation

The simulator MUST use the same packager the mock server uses. `Iso8583Server.createPackager(endpoint)` already handles the fallback chain:
1. `endpoint.packagerXmlContent` (custom uploaded) → `GenericPackager`
2. `/iso8583/packager.xml` (bundled) → `GenericPackager`
3. Fallback → `ISO87APackager`

The proxy endpoint should call the same `createPackager()` method (or extract it to a shared utility) to ensure the outbound message is packed identically to how the mock server will unpack it.

### 1.6 Response Decoding

The response `ISOMsg` should be unpacked field-by-field:
- MTI
- All present fields (0-128), mapped to dictionary labels
- Special attention to Field 39 (Response Code) — display the human-readable description from `commonResponseCodes` in the dictionary

### 1.7 Error Display

| Error Scenario | Display |
|---|---|
| Endpoint not active | "Endpoint is not active. Activate it before simulating." |
| Connection refused | "Connection refused on port {port}. Is the endpoint running?" |
| Timeout (>30s) | "Response timeout. The mock server may be misconfigured." |
| ISOException (pack) | "Invalid field value: {details}" |
| ISOException (unpack) | "Failed to decode response: {details}" |

### 1.8 Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Port conflict: simulator connects to a port that's not active | Medium | Check `endpoint.active` before attempting; return 400 if inactive |
| Packager mismatch: simulator packs with a different packager than the mock uses | High | Reuse `createPackager(endpoint)` — same logic, same endpoint config |
| Network loopback latency appears as mock server latency | Low | Document that `latencyMs` includes TCP round-trip on loopback |
| Q2 mode vs Standalone mode behaves differently for TCP | Low | Both accept standard ISO8583 TCP framing; simulator is protocol-agnostic |
| Large fields (field 55 EMV data) may cause UI overflow | Low | Truncate display, add "copy raw" button |

### 1.9 Proposed Architecture

```
┌─────────────────────────────┐
│  Frontend: Simulator Panel  │
│  ProtocolEditor.tsx          │
│  ┌───────────────────────┐  │
│  │ MTI Selector          │  │
│  │ Field Input Table     │  │
│  │ [Send] Button         │  │
│  │ Response Display      │  │
│  │ Error Display         │  │
│  └───────────────────────┘  │
└──────────────┬──────────────┘
               │ POST /api/iso8583/endpoints/{id}/simulate
               ▼
┌─────────────────────────────┐
│  Backend: SimulateEndpoint  │
│  Iso8583Controller.java     │
│  ┌───────────────────────┐  │
│  │ 1. Load endpoint      │  │
│  │ 2. createPackager()   │  │
│  │ 3. Build ISOMsg       │  │
│  │ 4. Pack to bytes      │  │
│  │ 5. TCP connect        │  │
│  │ 6. Send with framing  │  │
│  │ 7. Read response      │  │
│  │ 8. Unpack response    │  │
│  │ 9. Return JSON        │  │
│  └───────────────────────┘  │
└──────────────┬──────────────┘
               │ TCP (localhost:{port})
               ▼
┌─────────────────────────────┐
│  Iso8583Server (Mock)       │
│  Already running on port    │
│  Processes message normally │
└─────────────────────────────┘
```

### 1.10 Complexity

**Backend**: Medium (new endpoint + TCP client logic + shared packager extraction)  
**Frontend**: Medium (new UI panel with dynamic form + response display)  
**Overall**: Medium

---

## DELIVERABLE 2: Scenario State Inspector

### 2.1 Current State

- **`ScenarioService.java`** ([source](backend/src/main/java/com/dynamicmock/application/service/ScenarioService.java)):
  - Full state machine with Redis-backed state (`SCENARIO_STATE_PREFIX + scenarioName`)
  - Redis-backed execution counter (`SCENARIO_EXEC_PREFIX + scenarioName`)
  - `getCurrentState(name)` reads from Redis, falls back to DB/cache
  - `processTransition(name, context)` evaluates conditions and transitions
  - State is stored in Redis with 24-hour TTL

- **`ScenarioController.java`** ([source](backend/src/main/java/com/dynamicmock/adapter/in/web/ScenarioController.java)):
  - `GET /api/scenarios/{id}/state` — returns `{ scenarioName, currentState }`
  - `POST /api/scenarios/{id}/transition` — triggers a transition with context
  - `POST /api/scenarios/{id}/reset` — resets to initial state

- **`RedisBackedStateMap.java`** ([source](backend/src/main/java/com/dynamicmock/adapter/out/script/RedisBackedStateMap.java)):
  - Map abstraction over Redis hash; scripts can `state.put("key", "value")`
  - Key: `dynamic-mock:script-state:global`

- **Frontend `ScenarioEditor.tsx`** ([source](frontend/src/components/workspace/scenarios/ScenarioEditor.tsx)):
  - 335 lines; card view + ReactFlow diagram view
  - Shows current state (highlighted node), transitions, activate/deactivate/reset/delete
  - **Does NOT show**: scenario variables stored in Redis, state history, live refresh

### 2.2 Existing Reusable Components

| Component | Reuse Value | Notes |
|---|---|---|
| `ScenarioController GET /api/scenarios/{id}/state` | **HIGH** — already returns current state name | Needs enhancement: add Redis variables |
| `ScenarioController POST /api/scenarios/{id}/reset` | **HIGH** — already works | Used by ScenarioEditor already |
| `ScenarioService.getCurrentState()` | **HIGH** — reads from Redis | Could be extended to also return stored variables |
| `RedisBackedStateMap` | **REFERENCE** — shows how script variables are stored | Key pattern: `dynamic-mock:script-state:global` |
| `useScenarios` hook | **HIGH** — already provides CRUD and list | Needs extension for state inspection |
| `ScenarioFlowDiagram` (ReactFlow) | **HIGH** — already highlights current state | Can be enhanced with variable overlay |

### 2.3 Missing Components

#### Backend
1. **Enhanced State API** — `GET /api/scenarios/{id}/state/details`
   - Returns: `{ scenarioName, currentState, executionCount, stateVariables: {...}, lastTransitionAt }`
   - Reads scenario variables from Redis hash (`dynamic-mock:script-state:global` and/or scenario-specific keys)

2. **State Variable Editing** (DEFERRED — see 2.4)

#### Frontend
3. **State Inspector Panel** — embedded in `ScenarioEditor.tsx`
   - Shows current state name with highlighting (already exists in card/flow view)
   - Shows stored Redis variables for the scenario (key-value table)
   - Shows execution count
   - Auto-refresh on interval (every 2-5 seconds) or manual refresh button
   - Reset button (already exists)

### 2.4 MVP vs Deferred

| Feature | MVP? | Reasoning |
|---|---|---|
| View current state name | ✅ YES | Already partially exists; needs live refresh |
| View execution count | ✅ YES | Already returned by the API |
| View stored Redis variables | ✅ YES | This is the core value — debugging stateful scenarios |
| Reset to initial state | ✅ YES | Already exists |
| Manual refresh button | ✅ YES | Essential for debugging without auto-refresh overhead |
| Auto-refresh (polling) | ✅ YES (simple) | 5-second interval with `useQuery` refetchInterval |
| Edit state variables | ❌ DEFERRED | Adds complexity; users can reset instead |
| State transition history timeline | ❌ DEFERRED | Requires new Redis data structure (list/stream); not in MVP |
| Force-set current state to arbitrary value | ❌ DEFERRED | Edge case tool; reset covers 90% of use cases |

### 2.5 Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Redis variables key structure is global (`script-state:global`) not per-scenario | Medium | Document limitation; show global variables when inspecting any scenario. Future: namespace per scenario. |
| 24-hour TTL on scenario state in Redis may surprise users | Low | Display TTL information; document in UI |
| High-frequency polling could increase Redis load | Low | 5-second interval is conservative; add manual refresh option |
| `getCurrentState()` falls back to DB if Redis is empty — could mask stale data | Low | Document that state resets on Redis eviction |

### 2.6 Proposed Architecture

```
┌──────────────────────────────┐
│  ScenarioEditor.tsx          │
│  ┌────────────────────────┐  │
│  │ State Inspector Panel  │  │
│  │ ┌──────────────────┐   │  │
│  │ │ Current State:    │   │  │
│  │ │ [AUTHORIZED] ●    │   │  │
│  │ │ Exec Count: 3     │   │  │
│  │ │ ──────────────    │   │  │
│  │ │ Variables:        │   │  │
│  │ │ rrn: "123456"     │   │  │
│  │ │ pan: "4111...1111"│   │  │
│  │ │ amount: "10000"   │   │  │
│  │ │ ──────────────    │   │  │
│  │ │ [Refresh] [Reset] │   │  │
│  │ └──────────────────┘   │  │
│  └────────────────────────┘  │
└──────────────┬───────────────┘
               │ GET /api/scenarios/{id}/state/details
               ▼
┌──────────────────────────────┐
│  ScenarioController          │
│  GET /{id}/state/details     │
│  ┌────────────────────────┐  │
│  │ 1. Get scenario        │  │
│  │ 2. getCurrentState()   │  │
│  │ 3. Get exec count      │  │
│  │ 4. Get Redis variables │  │
│  │ 5. Return JSON         │  │
│  └────────────────────────┘  │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│  Redis                       │
│  scenario:state:{name}       │
│  scenario:exec:{name}        │
│  dynamic-mock:script-state:  │
│  global (Hash)               │
└──────────────────────────────┘
```

### 2.7 Complexity

**Backend**: Low (one new endpoint, reads existing Redis keys)  
**Frontend**: Medium (new panel in ScenarioEditor, polling, variable display)  
**Overall**: Low-Medium

---

## DELIVERABLE 3: User Workflow Design

### Ideal ISO8583 Simulation Workflow: Auth → Capture → Refund → Reversal

This workflow demonstrates the full power of the simulator combined with stateful scenarios.

#### Prerequisites
- One ISO8583 endpoint configured on port 8583 with 4 mocks:
  - `0100` Authorization (scenario: `payment-lifecycle`)
  - `0220` Capture (scenario: `payment-lifecycle`)
  - `0420` Reversal (scenario: `payment-lifecycle`)
  - `0200` Refund (no scenario — standalone)
- One scenario: `payment-lifecycle` with states: `IDLE → AUTHORIZED → CAPTURED → REVERSED`

#### Step-by-Step Workflow

**Step 1: Authorization (0100)**
1. User opens the Simulator panel in ProtocolEditor
2. Selects MTI: `0100 — Authorization Request`
3. Fills fields using dictionary labels:
   - Field 2 (PAN): `4111111111111111`
   - Field 3 (Processing Code): `000000`
   - Field 4 (Amount): `000000010000` (100.00)
   - Field 11 (STAN): `123456`
   - Field 41 (Terminal ID): `TERM0001`
   - Field 42 (Merchant ID): `MERCHANT00001`
4. Clicks **Send**
5. Response display shows:
   - MTI: `0110 — Authorization Response`
   - Field 39: `00` — Approved
   - Field 38: Auth Code
6. User opens Scenario Inspector → sees state: `AUTHORIZED`, variables: `{ rrn, pan, amount }`

**Step 2: Capture (0220)**
1. User selects MTI: `0220 — Financial Presentment`
2. Fills fields (reuses PAN, amount from step 1)
3. Clicks **Send**
4. Response: Field 39 `00`
5. Scenario Inspector → state: `CAPTURED`

**Step 3: Reversal (0420)**
1. User selects MTI: `0420 — Acquirer Reversal`
2. Fills fields
3. Clicks **Send**
4. Response: Field 39 `00`
5. Scenario Inspector → state: `REVERSED` → auto-resets to `IDLE`

**Step 4: Refund (0200 — standalone)**
1. User selects MTI: `0200 — Financial Transaction Request`
2. Processing Code: `200000` (refund)
3. Clicks **Send**
4. Response: Field 39 `00`
5. No scenario transition (standalone refund mock)

This workflow is achievable with the existing mock configuration system plus the two new deliverables. No new mock configuration features are needed.

---

## DEPENDENCY ANALYSIS

| Deliverable | Depends On |
|---|---|
| Message Simulator (Backend) | `Iso8583Server.createPackager()`, active endpoint on TCP port |
| Message Simulator (Frontend) | New backend proxy endpoint, `iso8583-fields.json` dictionary |
| Scenario State Inspector (Backend) | `ScenarioService`, Redis |
| Scenario State Inspector (Frontend) | Enhanced state API endpoint |

**No external dependencies.** Both deliverables build entirely on existing Sprint 1 infrastructure.

---

## RECOMMENDED IMPLEMENTATION ORDER

| Order | Component | Rationale |
|---|---|---|
| **1** | Backend: Extract `createPackager()` to shared utility | Both simulator and existing server need it. Currently instance method on `Iso8583Server`. |
| **2** | Backend: `POST /api/iso8583/endpoints/{id}/simulate` | Core simulator proxy — enables all frontend work. |
| **3** | Frontend: Simulator Panel in `Iso8583Editor` | The #1 demo feature. Most visible deliverable. |
| **4** | Backend: `GET /api/scenarios/{id}/state/details` | Small endpoint, enables inspector frontend work. |
| **5** | Frontend: State Inspector Panel in `ScenarioEditor` | Completes the debugging loop. |

**Estimated total**: ~3-4 implementation sessions.

---

## SUMMARY

| Aspect | Message Simulator | Scenario State Inspector |
|---|---|---|
| **Current State** | No simulator exists | State display is minimal (name only) |
| **Key New Backend** | `POST .../simulate` (TCP proxy) | `GET .../state/details` (Redis variables) |
| **Key New Frontend** | Simulator panel with form + response viewer | Inspector panel with variables + refresh |
| **Complexity** | Medium | Low-Medium |
| **Risk** | Medium (packager mismatch, TCP errors) | Low (reads existing data) |
| **Impact** | Very High — "no competitor has this" | High — makes stateful mocking debuggable |

> **This document is discovery only. No code has been modified.**

# Sprint 3 Execution Plan

## 1. Reference Banking Sandbox

To deliver a pre-configured financial testing environment that proves the ISO8583 and state machine capabilities, the following assets are required. 

### What is Required (Assets)
1. **Workspace & Environment**
   - A default workspace: `Demo Workspace`
   - A default environment: `Sandbox Env`
2. **ISO8583 Endpoint**
   - Active, running on Port `8583`.
   - Uses the bundled default `ISO87A` packager.
3. **Stateful Scenario: "Card Payment Flow"**
   - **States**: `INITIAL`, `AUTHORIZED`, `CAPTURED`, `REVERSED`, `REFUNDED`
   - **Initial State**: `INITIAL`
4. **Mock Definitions (ISO8583 Handlers)**
   - **Authorization (0100 → 0110)**: Condition: MTI=0100. Script: `state.put('lastAuthAmount', request.field(4)); scenario.transition('AUTHORIZED');`. Response: Field 39 = `00`.
   - **Capture (0220 → 0230)**: Condition: MTI=0220. Script: `scenario.transition('CAPTURED');`. Response: Field 39 = `00`.
   - **Reversal (0420 → 0430)**: Condition: MTI=0420. Script: `scenario.transition('REVERSED');`. Response: Field 39 = `00`.
   - **Refund (0200 → 0210)**: Condition: MTI=0200 & Processing Code (3) starts with `20`. Script: `scenario.transition('REFUNDED');`. Response: Field 39 = `00`.

### Delivery Mechanism Recommendation
**Recommended: B. Startup Initialization Script**

*Why?* 
- **Seed Data (Mongo dumps)**: Fragile across versions, requires Docker volume management.
- **Import Bundle**: Introduces friction. The user must click "Import" before seeing value.
- **Docker Profile**: Adds command-line complexity (`docker-compose --profile sandbox up`).
- **Startup Script (Backend)**: A Spring Boot `ApplicationRunner` or `@PostConstruct` bean that checks `scenarioRepository.count() == 0`. If empty, it programmatically creates the Workspace, Endpoint, Scenario, and Mocks. It requires zero user action, works perfectly on bare metal or Docker, and guarantees the sandbox is always syntactically correct against the current database schema.

### What requires code changes?
- Adding a `SandboxDataInitializer.java` component to the backend.
- Adding a toggle (e.g., application.yml `dynamic-mock.sandbox.enabled=true`) to control insertion.

### What should be avoided?
- Do not build a complex UI for "Sandbox Management".
- Do not build an import/export JSON parser just for this. Use backend entity repositories directly to construct the data.

---

## 2. First-Time User Experience (The 5-Minute "Wow")

**Goal**: Move the user from "What is this?" to "This is exactly what I need" in under 300 seconds.

* **Minute 1 (Install & Launch)**: User runs `docker-compose up -d`. They open `localhost:3000`. The UI loads instantly. Because of the Initialization Script, the dashboard isn't empty—it shows an active ISO8583 endpoint and the "Card Payment Flow" scenario.
* **Minute 2 (Discovery)**: Following the README Quickstart, the user clicks the ISO8583 protocol editor. They see the endpoint configuration and switch to the new **Simulator** tab.
* **Minute 3 (Action)**: User selects the `0100` (Authorization) template. The fields auto-populate. They click **Send**. The response panel instantly decodes a `0110` response with Field 39 highlighted green as `00` (Approved).
* **Minute 4 (Observation)**: User clicks the **Scenarios** tab. They open the "Card Payment Flow" editor. The **State Inspector** panel shows the current state has moved from `INITIAL` to `AUTHORIZED`, with execution count `1`, and the script variable `lastAuthAmount` visible in the Redis table.
* **Minute 5 (Completion)**: User goes back to the Simulator, selects `0420` (Reversal), and clicks Send. They flip back to the Scenario Inspector and see the state is now `REVERSED`. The "wow" moment is achieved: they have simulated a stateful financial transaction without writing a single line of code or deploying a complex test harness.

---

## 3. README Repositioning

### 1. New Project Positioning Statement
> **Dynamic Mock** — The Open Source Enterprise Integration Simulation Platform.  
> *Simulate REST, GraphQL, gRPC, and ISO8583 with stateful scenarios. Self-hosted. White-label ready. Built for banks, payment switches, and fintechs.*

### 2. New GitHub Opening Section
Replace the generic "mock API server" text. 
"Replace your test switches and legacy stubs with a single platform that speaks ISO8583 natively, supports stateful transaction flows (Auth → Capture → Reversal), and runs entirely within your infrastructure."

### 3. Screenshot Strategy
The README must include high-quality images directly below the opening paragraph:
- **Hero Image 1**: The ISO8583 Message Simulator showing a decoded `0110` response with the Data Dictionary labels.
- **Hero Image 2**: The Scenario State Inspector showing the animated state transition and script variables.

### 4. Demo Strategy
Instead of a video, the README will instruct the user to spin up the local Docker instance which now includes the Reference Banking Sandbox by default.

### 5. Quick-Start Flow
A new section: **"Try the 5-Minute Banking Sandbox"**
1. Run `docker-compose up`
2. Open the ISO8583 Simulator
3. Send an 0100 Auth
4. Check the Scenario Inspector

---

## 4. Execution Plan

### Deliverables
1. `SandboxDataInitializer.java` (Backend) — Programmatically generates the endpoint, scenario, and mocks if the DB is empty.
2. `BANKING_SANDBOX_TUTORIAL.md` (Docs) — The step-by-step tutorial.
3. `README.md` (Docs) — Complete repositioning rewrite.
4. Exported screenshots (added to a `/docs/assets/` folder).

### Dependencies
- Sprint 1 (Data Dictionary) — ✅ Done
- Sprint 2 (Simulator & Inspector) — ✅ Done

### Recommended Implementation Order
1. **Backend**: Write `SandboxDataInitializer.java`. Map the 4 ISO8583 endpoints (0100, 0220, 0420, 0200) and the Scenario. Test that it loads on an empty database.
2. **End-to-End Test**: Manually run the Minute 1-5 workflow locally to ensure the sandbox scripts execute flawlessly.
3. **Assets**: Take screenshots of the flawless workflow.
4. **Documentation**: Write the Tutorial and rewrite the README.

### Risks
- **Idempotency**: The init script must strictly check `scenarioRepository.count() == 0` so it doesn't overwrite a user's actual work on subsequent restarts.
- **Script Syntax Errors**: If the JS scripts injected by the initializer have typos, the Sandbox demo will fail, ruining the First-Time User Experience. The script strings must be heavily tested.

### Success Criteria
- The fastest path to "wow" is achieved: 0 configuration steps required from the user to send a stateful ISO8583 message.
- README clearly articulates the product's enterprise simulation category.
- No UI components or generic API enhancements are built, adhering strictly to the discovery mandate.

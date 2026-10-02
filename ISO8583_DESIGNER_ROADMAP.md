# ISO8583 Designer Roadmap

Based on the `ISO8583_GAP_ANALYSIS.md` and aligned with the `AI_GOVERNANCE.md` Differentiation Rule (which dictates prioritizing ISO8583, Stateful Scenarios, and Protocol Simulation), the following roadmap outlines the path to a production-ready ISO8583 Designer.

## Phase 1: Core Integrity & State (High Priority)
*Fixing foundational issues that block true stateful financial simulation.*

1. **Stateful Scenario Integration (Backend)**
   - **Issue**: `Iso8583Server.java` currently resets the `state` hashmap on every request.
   - **Action**: Inject the global `Scenario` or Redis-backed state context into the `ScriptContext` so ISO8583 messages can mutate and read from long-lived scenarios (e.g., tying an 0100 Auth to an 0400 Reversal).

2. **Custom Packager Upload (Full Stack)**
   - **Action**: Allow users to upload or paste a custom `packager.xml` in the UI and save it to the `Iso8583Endpoint` entity.
   - **Impact**: Enables support for proprietary ISO8583 dialects instead of forcing the default ISO87A packager.

## Phase 2: UX Dictionary & Fields (Medium Priority)
*Moving away from raw key-value mapping to a domain-aware UI.*

1. **ISO8583 Data Dictionary UI**
   - **Action**: Enhance `ResponseFieldsEditor` to display standard field names alongside numbers (e.g., `Field 2 [Primary Account Number (PAN)]`, `Field 39 [Response Code]`).
   - **Action**: Add a visual bitmap indicator showing which fields are active in a given mock template.

2. **Semantic Matchers UI**
   - **Action**: Replace the generic JSON-like matchers input with a structured rule builder (e.g., "If [Field 4 (Amount)] [is greater than] [5000]").

## Phase 3: Simulator & Debugging (Medium Priority)
*Closing the feedback loop for developers without requiring external tools like JMeter or custom TCP scripts.*

1. **Built-in ISO8583 Client (Web Terminal)**
   - **Action**: Build a UI panel to construct and send an ISO8583 message (either via Hex or Field-by-Field form) to the local TCP port via a WebSocket or backend proxy.
   - **Impact**: Allows instant testing of routing and matchers within the browser.

2. **Live Hex / Trace Viewer**
   - **Action**: Display live incoming and outgoing TCP hex dumps and parsed jPOS XML trees for the active endpoint.

## Phase 4: Lifecycle & Versioning (Lower Priority)
*Aligning ISO8583 endpoints with REST route maturity.*

1. **Versioning System**
   - **Action**: Implement `version` control for `Iso8583Endpoint` similar to REST routes, allowing users to rollback broken financial routing configurations.

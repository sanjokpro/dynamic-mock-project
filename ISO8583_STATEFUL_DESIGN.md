# ISO8583 Stateful Integration Design

## 1. Current Architecture
Currently, stateful workflows in the application are exclusively driven by HTTP REST traffic via the `DynamicRouteDispatcher`.

- **Scenario Engine (`ScenarioService`)**: Manages `Scenario` entities (Initial State, States, Transitions) and stores the runtime current state in Redis (`scenario:state:{name}`).
- **REST Integration**: The `DynamicRouteDispatcher` checks if a `MockRoute` is linked to a `scenarioName`. If so, it fetches the current `ScenarioState`, overrides the route's `preScript`, `postScript`, `responseTemplate`, and `delayMs`, executes the response, and then evaluates state transitions by calling `scenarioService.processTransition()`.
- **ISO8583 Integration (`Iso8583Server`)**: 
  - Matches incoming TCP ISO8583 messages to an `Iso8583Mock`.
  - **Flaw**: It injects a fresh, empty HashMap for script state per request (`context.put("state", new HashMap<>());`), meaning GraalVM scripts cannot persist state.
  - **Flaw**: It has no knowledge of `ScenarioService` or `scenarioName`, making multi-step financial simulations (e.g., Auth 0100 -> Capture 0200 -> Reversal 0400) impossible without manual external database hacks.

## 2. Target Architecture
ISO8583 endpoints will reuse the exact same `ScenarioService` and `RedisBackedStateMap` used by the REST dispatcher, creating a unified state engine across all protocols.

A user will be able to link an `Iso8583Mock` to a global `Scenario`. When a TCP message hits the mock:
1. The global script state is mounted via Redis.
2. The current `ScenarioState` is fetched.
3. The scenario state overrides the mock's script and fields.
4. The response is sent.
5. The `ScenarioService` evaluates the transition logic based on the ISO8583 request/response context.

## 3. Required Backend Changes

### A. Entity & DTO Updates
- Modify `Iso8583Endpoint.Iso8583Mock` to include `private String scenarioName;`.
- Update `Iso8583EndpointRequest` and `Iso8583EndpointResponse` DTOs to include `scenarioName` inside the mock definitions.

### B. `Iso8583Server` Refactoring
- **Dependency Injection**: Inject `ScenarioService`, `RedisTemplate`, and `ObjectMapper`.
- **Global State Fix**: Replace the local `HashMap` state injection with the unified Redis map:
  ```java
  String stateKey = "dynamic-mock:script-state:global";
  context.put("state", new RedisBackedStateMap(stateKey, redisTemplate));
  ```
- **Scenario State Override**:
  - After finding the matching `Iso8583Mock`, check `mock.getScenarioName()`.
  - If present, fetch `ScenarioState` from `ScenarioService`.
  - Override `mock.getDelayMs()` with `ScenarioState.getDelayMs()`.
  - Override `mock.getScript()` with `ScenarioState.getPostScript()`.
  - If `ScenarioState.getResponseTemplate()` contains valid JSON, parse it as a `Map<Integer, String>` and merge/override `mock.getResponseFields()`.
- **Transition Execution**:
  - After processing the response (but before TCP flush), build a `transitionContext` containing:
    - `request`: Maps MTI and all fields (`field.2`, `field.39`, etc.)
    - `response`: Maps the outgoing MTI and fields.
    - `state`: The Redis state.
  - Call `scenarioService.processTransition(mock.getScenarioName(), transitionContext)`.

## 4. Required Frontend Changes

### A. `Iso8583Editor` (ProtocolEditor.tsx)
- Add a `useScenarios()` hook fetch to retrieve available scenarios.
- In the Mock Configuration panel, add a dropdown: **"Link to Scenario (Optional)"**.
- Add a UI warning/helper text: *If linked to a scenario, the Scenario's `postScript` and JSON `responseTemplate` will override this mock's default fields.*

## 5. Migration Risks
- **Backwards Compatibility**: Existing `Iso8583Endpoint` MongoDB documents will simply deserialize with a `null` `scenarioName`, bypassing the new logic seamlessly.
- **Data Parsing**: `ScenarioState.responseTemplate` is a String. For HTTP, it returns raw text. For ISO8583, it must be parsed as a Map of Integers to Strings (e.g., `{"39": "00"}`). If a user provides invalid JSON in the scenario template, the ISO8583 server must gracefully fallback or drop the override to avoid crashing the TCP thread.

## 6. Performance Concerns
- **Latency Restrictions**: Financial systems communicating via ISO8583 often enforce strict timeouts (e.g., < 1000ms, sometimes < 50ms for high-throughput switches).
- **Redis Roundtrips**: Connecting the mock to `ScenarioService` and `RedisBackedStateMap` will introduce network I/O to Redis. Fetching the scenario state, updating the script state, and processing transitions could add 2-10ms of latency per TCP packet. While acceptable for a mock, this should be documented for users running high-load performance tests.
- **Thread Blocking**: The `Iso8583Server` handles TCP connections in an ExecutorService thread pool. Heavy scripts or slow Redis responses could exhaust the thread pool, requiring careful tuning of timeout configurations.

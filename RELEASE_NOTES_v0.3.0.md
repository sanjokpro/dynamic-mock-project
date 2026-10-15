# Dynamic Mock v0.3.0

Dynamic Mock now includes a complete Reference Banking Sandbox.

In less than five minutes a user can:

1. Load a complete ISO8583 sandbox.
2. Send an Authorization request.
3. Observe live scenario state transitions.
4. Execute Capture and Reversal flows.
5. Explore stateful transaction simulation without external tools.

## Highlights

### Reference Banking Sandbox

- One-click loading experience
- Preconfigured ISO8583 endpoint
- Stateful Card Payment Flow

### ISO8583 Simulation

- Message Simulator
- Data Dictionary
- Custom Packagers
- Decoded Responses

### Stateful Testing

- Scenario State Inspector
- Redis-backed variables
- Execution counters
- State transition visibility

### Enterprise Positioning

- Enterprise Integration Simulation Platform
- Updated documentation
- Banking Sandbox Tutorial
- Improved first-time user experience

## Validated Flow

INITIAL
-> AUTHORIZED
-> CAPTURED
-> REVERSED
-> INITIAL (auto-reset)

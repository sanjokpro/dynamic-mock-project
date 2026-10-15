# V0.3.0 Release Report

## Release Details

- **Tag created:** `v0.3.0`
- **Tag SHA:** `f5defe1372bcb701fe6e4e6d6b3ad58371a22033`
- **Push status:** SUCCESS (`git push origin v0.3.0` completed)
- **Release title:** `v0.3.0 - Reference Banking Sandbox`
- **Release URL:** *Pending (Requires manual GitHub Release creation via UI as `gh` CLI is unavailable in this environment)*

## Release Notes Summary

**Dynamic Mock v0.3.0**

Dynamic Mock now includes a complete Reference Banking Sandbox.

In less than five minutes a user can:
1. Load a complete ISO8583 sandbox.
2. Send an Authorization request.
3. Observe live scenario state transitions.
4. Execute Capture and Reversal flows.
5. Explore stateful transaction simulation without external tools.

**Highlights**
- **Reference Banking Sandbox:** One-click loading experience, preconfigured ISO8583 endpoint, stateful Card Payment Flow.
- **ISO8583 Simulation:** Message Simulator, Data Dictionary, Custom Packagers, Decoded Responses.
- **Stateful Testing:** Scenario State Inspector, Redis-backed variables, Execution counters, State transition visibility.
- **Enterprise Positioning:** Enterprise Integration Simulation Platform, Updated documentation, Banking Sandbox Tutorial, Improved first-time user experience.

**Validated Flow**
- `INITIAL` -> `AUTHORIZED` -> `CAPTURED` -> `REVERSED` -> `INITIAL` (auto-reset)

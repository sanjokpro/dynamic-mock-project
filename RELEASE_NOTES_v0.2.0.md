# Release Notes: v0.2.0

**Sprint:** 2 (ISO8583 User Experience Completion)
**Release Date:** 2026-10-05

## Sprint Summary
Sprint 2 successfully closes the ISO8583 feedback loop, establishing Dynamic Mock as a deeply integrated environment for financial protocol simulation. Users can now send stateful ISO8583 test messages directly from their browser to a running mock server and observe live scenario state transitions natively in the UI.

## Major Features

### 1. ISO8583 Message Simulator
A fully integrated TCP client within the browser interface:
- **Simulator Panel**: Seamlessly embedded alongside ISO8583 protocol configuration.
- **Smart Fields**: Data dictionary-powered inputs including formats, field abbreviations, and labels.
- **Templates**: Quick load definitions for common MTIs (`0100` Authorization, `0200` Financial, `0420` Reversal).
- **Deep Inspection**: Decoded response table with Field 39 (Response Code) human-readable mapping and raw hex toggles for advanced debugging.
- **Shared Packager Architecture**: Guarantees test requests are packed and parsed exactly how the target endpoint expects, including custom uploaded packagers.

### 2. Scenario State Inspector
Live visibility into the stateful backend execution engine:
- **Live Monitoring**: 5-second auto-refresh polling directly inside the Scenario Editor.
- **State Traversal Tracking**: Shows current state, execution counts, and an active pulse indicator.
- **Script Variables**: Tabular view of variables written by scripts (`state.put(key, value)`) mapped back to the global Redis hash storage.

## User Workflow Enabled
Users can now configure an ISO8583 mock endpoint, write a scenario with Javascript scripts that modify variables on request, and immediately switch to the simulator to send an `0100` authorization request. When the mock replies, the user sees the decoded `0110` response in the simulator and the incremented execution counts plus any modified Redis variables in the state inspector—all without leaving the application or switching to external testing tools.

## Known Limitations
- The Scenario State Inspector currently reads from a global hash (`dynamic-mock:script-state:global`). In advanced multi-tenant or concurrent load testing scenarios, this may show shared data. Full scenario-level namespacing is planned for the future.
- The Simulator does not yet persist historical test runs; once the browser is refreshed or the MTI is switched, the last response is cleared.

## Next Sprint Focus
**Sprint 3: Generic API Enhancements & Usability** (as dictated by the `V0_1_0_RELEASE_READY.md` action plan).
Focus will pivot towards refining REST/HTTP mocking experiences, API key security, workspace management, and bridging the generic platform usability gaps identified during early discovery phases.

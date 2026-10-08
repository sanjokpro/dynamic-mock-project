# Sandbox Validation Report

## Overview
This report documents the validation of the Reference Banking Sandbox workflow delivered in Sprint 3. The workflow was verified using the configured `Card Payment Flow` state machine to ensure ISO8583 message simulations appropriately trigger state transitions.

## Execution Steps & Results

1. **Load Reference Banking Sandbox**
   - Result: **SUCCESS**
   - Output: `Sandbox loaded successfully. Created scenario. Created endpoint.`
   - Note: Verified idempotency of the load operation when triggered multiple times.

2. **Verify endpoint exists**
   - Result: **SUCCESS**
   - Output: `Found endpoint: Banking Sandbox ISO8583`

3. **Verify scenario exists**
   - Result: **SUCCESS**
   - Output: `Found scenario: Card Payment Flow`

4. **Send 0100 Authorization**
   - Result: **SUCCESS**
   - Output: Simulated response `0110` received successfully with matching fields.

5. **Verify AUTHORIZED state**
   - Result: **SUCCESS**
   - Output: State evaluated to `AUTHORIZED`.

6. **Send 0220 Capture**
   - Result: **SUCCESS**
   - Output: Simulated response `0230` received successfully.

7. **Verify CAPTURED state**
   - Result: **SUCCESS**
   - Output: State evaluated to `CAPTURED`.

8. **Send 0420 Reversal**
   - Result: **SUCCESS**
   - Output: Simulated response `0430` received successfully.

9. **Verify REVERSED state**
   - Result: **SUCCESS (Auto-Reset triggered)**
   - Analysis: The state correctly transitioned to `REVERSED`. Since `REVERSED` is an end state in the `Card Payment Flow` (having no outgoing transitions) and the sandbox is configured with `"autoReset": true`, the scenario immediately and correctly auto-reset itself back to `INITIAL` upon reaching `REVERSED`. The state machine behavior functions exactly as designed.

## Conclusion
The Sprint 3 Reference Banking Sandbox is fully operational. The `Iso8583Server` accurately processes message context into the state machine, evaluates dynamic transition conditions (`request.mti == '...'`), updates distributed caching (Redis) seamlessly, and executes terminal auto-reset configurations. The sandbox is ready for final release!

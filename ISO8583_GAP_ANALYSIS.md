# ISO8583 Designer Gap Analysis

## Overview
This document analyzes the current state of ISO8583 support within the Dynamic Mock platform, mapping out what is functional, what is API-only, and what requires further UI or architectural development to provide a robust designer experience.

## Feature State Classification

### 1. Server Configuration & Connectivity
- **Port Management**: ✅ Complete. (Backend and Frontend both support configuring ports and isolated vs shared modes).
- **Encoding (ASCII/EBCDIC/BINARY)**: ✅ Complete.
- **Header Length (NONE, 2BYTE, 4BYTE)**: ✅ Complete.
- **jPOS Q2 Integration**: ✅ Complete. (Backend `Iso8583Server` integrates with `Q2ServerManager` and supports standalone fallbacks).

### 2. Message Routing & Matchers
- **MTI Matching**: ✅ Complete. (UI has a dropdown for standard MTIs, Backend filters by MTI).
- **Field Matching (Regex)**: ⚠ Partial. (Backend supports `field.X` regex matching. UI has a basic key-value `ResponseFieldsEditor`, but lacks UX validation to ensure users enter valid field IDs).
- **Priority Resolution**: ✅ Complete. (Mocks are sorted by priority to find the best match).

### 3. Response Generation
- **Response Fields Mapping**: ⚠ Partial. (Users can map fields, but there is no dictionary/schema to tell them what field `3` or `4` means).
- **Handlebars Templating**: ✅ Complete. (Backend uses `ResponseTemplateEngine`).
- **Response Scripting (JS/Python)**: ✅ Complete. (Supported in both UI and Backend `runResponseScript`).
- **Interceptor Scripts**: ✅ Complete. (Supported in UI and Backend).

### 4. Advanced ISO8583 Features
- **Bitmap Handling**: ❌ Missing. (Abstracted entirely by jPOS `GenericPackager`. The user cannot visually inspect or manipulate the bitmap, nor see which fields are active).
- **Packager Definitions**: ⚠ Partial. (Backend defaults to `ISO87APackager` or a custom `packager.xml`. The UI does not allow uploading or selecting custom packager XML files).

### 5. Integration & Workflows
- **Scenario Integration (Stateful Mocking)**: ❌ Broken/Missing.
  - *Code Evidence*: `Iso8583Server.java` line 445 injects `context.put("state", new HashMap<String, Object>());` per request. State is **not** persisted to Redis or the `Scenario` entity, contradicting the README.
- **Versioning Integration**: ❌ Missing. (`Iso8583Endpoint` lacks the `version`, `previousVersions`, and diffing capabilities present in REST routes).
- **Message Simulation / Testing**: ❌ Missing. (There is no UI to send a test ISO8583 payload to the configured port directly from the browser, making debugging extremely difficult).

## Summary for Real Users
While the backend engine is highly capable (thanks to jPOS), the frontend is essentially a raw JSON editor disguised as a UI. Real users dealing with ISO8583 need a data dictionary (knowing field 2 is PAN, field 4 is Amount), a way to test messages instantly without a third-party client (like a built-in terminal or hex sender), and actual stateful integration to simulate multi-step financial flows (Authorization -> Reversal).

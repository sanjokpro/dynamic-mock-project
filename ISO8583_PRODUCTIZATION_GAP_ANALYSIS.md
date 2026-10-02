# ISO8583 Productization Gap Analysis

## Overview
This document analyzes the gap between the functional but developer-centric ISO8583 engine and a productized, user-friendly designer. It prioritizes gaps based on their impact on user adoption and the ability to differentiate the product.

## Gap Analysis

### 1. Missing Data Dictionary Support
- **Current State**: The `ProtocolEditor` uses raw JSON key-value pairs (e.g., `39 = 00`, `2 = 4111...`). Users must manually remember or look up ISO8583 field specifications.
- **Gap**: No built-in dictionary describing what `Field 2` (PAN) or `Field 39` (Response Code) actually are.
- **Impact**: Extremely steep learning curve for users who are not ISO8583 domain experts.

### 2. Missing Message Simulator Support
- **Current State**: Users can configure endpoints and mocks, but cannot test them from within the UI. They must use an external client (like a JMeter plugin or custom Java TCP client).
- **Gap**: No built-in UI terminal or form to construct an ISO8583 message (either via Hex or visual form) and ping the local server port.
- **Impact**: Breaks the instant feedback loop. Users cannot immediately verify if their mock matches work.

### 3. Missing Packager Upload Support
- **Current State**: `Iso8583Endpoint` entity has a `packagerConfig` field, and `Q2ServerManager` uses it to load XML packagers. However, the `Iso8583Controller` API and the Frontend have zero support for uploading a custom `packager.xml` file.
- **Gap**: Users are stuck with the default ISO87A packager. If their switch uses a proprietary dialect (e.g., Postilion, Base24), the parser will fail on custom fields.
- **Impact**: Critical blocker for enterprise adoption, as almost every bank has a slightly modified packager.

### 4. Missing Bitmap Visualization
- **Current State**: The jPOS engine handles bitmaps under the hood. The UI has no concept of a bitmap.
- **Gap**: Users cannot visually see a grid of 1-128 indicating which fields are present in a template or request.
- **Impact**: Users lack visual confirmation of the message structure, making debugging harder.

### 5. Missing Scenario State Visibility
- **Current State**: Mocks can now be linked to a Scenario via the UI dropdown. However, there is no UI to view the *live* Redis state of that scenario during an ISO8583 transaction.
- **Gap**: If an Auth (0100) sets state, the user cannot inspect the state before sending the Reversal (0400).
- **Impact**: Hard to debug complex stateful flows.

### 6. Missing Response Field Builder UX
- **Current State**: The UI allows defining Handlebars templates (`{{$randomString}}`) in a basic text input.
- **Gap**: No autocomplete or validation for template helpers specific to financial fields (e.g., generating a valid Luhn PAN, or a valid STAN).

---

## Prioritization Matrix

Aligning with the `Differentiation Rule` from `AI_GOVERNANCE.md`:

### P0 - Critical Blockers
1. **Missing Packager Upload Support**: Without custom packager support, the product cannot parse real-world enterprise traffic.
2. **Missing Data Dictionary Support**: Without field labels, the product is just a raw JSON editor, offering no UX advantage over raw config files.

### P1 - Core Value Additions
1. **Missing Message Simulator Support**: A built-in ISO8583 client is a massive differentiator that significantly speeds up testing.
2. **Missing Scenario State Visibility**: Required to fully leverage the stateful integration implemented in the previous phase.

### P2 - UX Enhancements
1. **Missing Bitmap Visualization**: Highly requested by financial engineers, but not strictly required to function.

### P3 - Nice-to-Have
1. **Missing Response Field Builder UX**: Financial specific data-generators (Luhn, Track 2 data generators) would be a great addition later.

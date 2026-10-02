# Sprint 1 Discovery Report

**Sprint**: Sprint 1 — ISO8583 Productization Foundation  
**Date**: 2026-10-01  
**Status**: DISCOVERY COMPLETE — Ready for implementation

---

## 1. PACKAGER UPLOAD SUPPORT

### Current State
- The domain entity `Iso8583Endpoint` has a `packagerConfig` field (String) intended as a file path reference (e.g., `cfg/packager.xml`).
- `Iso8583Server.createPackager()` (line 184) **ignores `endpoint.packagerConfig` entirely**. It hardcodes loading from `getResourceAsStream("/iso8583/packager.xml")` regardless of what the endpoint specifies.
- `Q2ServerManager.generateServerXml()` (line 232) **does** use `endpoint.getPackagerConfig()` in the generated XML (`<property name="packager-config" value="...">`), so Q2 mode would theoretically use a custom path — but no mechanism exists to put a file at that path.
- The default `packager.xml` in `src/main/resources/iso8583/` contains 129 standard ISO 8583:1987 field definitions. This is a comprehensive baseline.
- `ImportController.java` demonstrates the existing pattern for multipart file uploads (`@RequestParam("file") MultipartFile`).

### Reusable Assets
- `Iso8583Endpoint.packagerConfig` field already exists in domain, DTO, Mongo entity, and mapper.
- `ImportController` provides an upload pattern (Spring multipart).
- `Q2ServerManager.copyPackagerConfig()` shows the existing file-based packager provisioning flow.

### Gaps
1. **No upload API endpoint** on `Iso8583Controller`.
2. **No storage mechanism** — uploading an XML file has no destination.
3. **`Iso8583Server.createPackager()` ignores `packagerConfig`** — standalone mode always uses the bundled default.
4. **No validation** — uploaded XML isn't validated as a valid jPOS packager before saving.
5. **No frontend UI** — `Iso8583Editor` has no file upload component.

### Risks
- **Invalid packager XMLs** could crash the jPOS server. Validation must be performed before persisting.
- **File system dependencies** break Docker portability if packagers are stored on disk.
- **Large XML files** — packager XMLs are typically 5-15KB but could be larger for extended field sets.

### Proposed Implementation

**Storage: Option A — MongoDB (Recommended)**

Store the raw XML content as a String field directly on `Iso8583Endpoint`:

```
Iso8583Endpoint.packagerXmlContent: String  // The actual XML bytes
Iso8583Endpoint.packagerConfig: String       // Keep for backward compat, but repurpose as display name
```

**Justification**: Packager XMLs are small (5-30KB). Storing in Mongo keeps the data co-located with the endpoint configuration, survives container restarts, and requires no GridFS or S3. The `ImportController` pattern already handles file uploads into byte arrays. This is the simplest approach with the fewest moving parts.

**Rejected alternatives**:
- *File system (Option B)*: Breaks Docker volume assumptions, requires shared storage in scaled deployments.
- *GridFS (Option C)*: Overkill for 10-30KB XML files. Adds GridFS dependency for no benefit.

**Upload flow**:
1. New API: `POST /api/iso8583/endpoints/{id}/packager` accepts `MultipartFile`.
2. Backend reads bytes, validates by attempting `new GenericPackager(new ByteArrayInputStream(bytes))`.
3. If valid, stores XML content on the endpoint entity.
4. `createPackager()` is modified to check `packagerXmlContent` first, then fall back to bundled default.

**Frontend**:
- Add a "Packager" section to `Iso8583Editor` with a file upload input (`<input type="file" accept=".xml">`).
- Show the current packager status (Default / Custom with filename).
- Provide a "Reset to Default" button.

### Complexity
Medium. Backend: ~2 days. Frontend: ~1 day.

### Dependencies
None.

---

## 2. ISO8583 DATA DICTIONARY

### Current State
- The `ResponseFieldsEditor` component uses raw numeric inputs (`Field #` → `value`). Users type field numbers manually with no guidance.
- The bundled `packager.xml` already contains human-readable field names for all 129 fields (e.g., `id="2" name="Primary Account Number"`).
- The MTI dropdown in `Iso8583Editor` already demonstrates UI-friendly labels (e.g., `0100 - Authorization Request`).

### Reusable Assets
- The field names in `packager.xml` can be extracted into a static JSON dictionary.
- The `ResponseFieldsEditor` component already handles key-value rendering; it just needs field name annotations.

### Gaps
1. **No field dictionary data** accessible to the frontend.
2. **No search/autocomplete** when adding response fields.
3. **No field descriptions** beyond the name (e.g., "PAN" doesn't explain format or typical values).

### Risks
- Maintaining a separate dictionary JSON that drifts from the active packager is a data consistency risk.
- Custom packagers may define non-standard fields — the dictionary must support overrides.

### Proposed Implementation

**Strategy: Option A — Hardcoded static JSON (Recommended for MVP)**

Ship a `iso8583-fields.json` as a static asset in the frontend:

```json
{
  "fields": {
    "0": { "name": "Message Type Indicator", "format": "n-4", "description": "MTI" },
    "2": { "name": "Primary Account Number", "format": "n..19", "description": "Card number (PAN)" },
    "3": { "name": "Processing Code", "format": "n-6", "description": "Transaction type identifier" },
    "4": { "name": "Transaction Amount", "format": "n-12", "description": "Amount in minor units" },
    ...
    "39": { "name": "Response Code", "format": "an-2", "description": "00=Approved, 05=Declined" }
  }
}
```

**Justification**: The ISO 8583:1987 standard has a fixed set of 128 data elements. These are well-defined and unchanging. A static JSON asset is the simplest delivery mechanism, requires no backend API, and can be iterated quickly. Future sprints can add dynamic dictionary generation from the uploaded packager XML.

**Rejected alternatives**:
- *Database-driven (Option C)*: Over-engineered for Sprint 1. The standard fields are universal. Custom fields from custom packagers can be addressed later.
- *Backend API endpoint (Option B)*: Adds a network round-trip for static data. Deferred until custom packager dictionary parsing is needed.

**UI Integration**:
1. Import the dictionary JSON in `ResponseFieldsEditor`.
2. Replace the raw `Field #` input with an autocomplete/dropdown that shows `2 — Primary Account Number (PAN)`.
3. Show the field description as a tooltip.
4. Allow free-form numeric entry for fields not in the dictionary (to support custom packagers).

### Complexity
Low-Medium. JSON creation: ~0.5 day. Frontend integration: ~1.5 days.

### Dependencies
None.

---

## 3. GITHUB ACTIONS CI PIPELINE

### Current State
- **No `.github/` directory exists**. Zero CI/CD configuration.
- Backend uses Gradle (`build.gradle`) with JUnit 5, Testcontainers, and JaCoCo.
- Frontend uses Next.js with Vitest.
- Backend build command: `./gradlew test build` (requires Java 21, MongoDB, Redis for integration tests).
- Frontend build command: `npm run lint && npm run build && npm run test`.

### Reusable Assets
- `backend/build.gradle` has clean task definitions (`test`, `build`, `jacocoTestReport`).
- `frontend/package.json` has `lint`, `build`, `test` scripts.

### Gaps
1. No GitHub Actions workflow files.
2. No branch protection rules.
3. No status badges.

### Risks
- Backend integration tests require MongoDB and Redis (Testcontainers). CI must have Docker available.
- GraalVM polyglot dependencies may cause slow first-time builds.

### Proposed Implementation

**Minimum viable pipeline** — two jobs:

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./gradlew test build
        working-directory: backend
  
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      - run: npm ci && npm run lint && npm run build && npm run test
        working-directory: frontend
```

**Notes**:
- Ubuntu runners include Docker, so Testcontainers works out of the box.
- Backend tests with tag `integration` are excluded by default in `build.gradle`, so unit tests will pass without Docker in case of issues.

### Complexity
Low. ~0.5 day.

### Dependencies
None.

---

## 4. LICENSE VALIDATION

### Current State
- **No LICENSE file exists in the repository.**
- No license headers in source files.
- No `THIRD_PARTY_LICENSES.md`.

### Reusable Assets
None.

### Gaps
1. Missing LICENSE file.
2. Missing third-party attribution.
3. jPOS AGPL implications undocumented.

### Risks
- Enterprise legal teams will reject adoption if licensing is ambiguous.
- jPOS uses **AGPL v3**. This is the most restrictive common open source license. Key implication: if the application is modified and served over a network, the source code of modifications must be made available.
- Dynamic Mock does **not** modify jPOS source — it uses jPOS as an unmodified library dependency. This is a critical distinction.
- GraalVM components use the **Universal Permissive License (UPL)** — no concern.
- Spring Boot, MongoDB driver, Redis client — all Apache 2.0 — no concern.

### Proposed Implementation

1. **LICENSE file**: Apache 2.0 (recommended). This is the standard for enterprise-friendly open source. It is compatible with using AGPL dependencies as unmodified libraries.

2. **THIRD_PARTY_LICENSES.md**:

```markdown
# Third-Party Licenses

## jPOS (org.jpos:jpos:2.1.9)
License: GNU Affero General Public License v3 (AGPL-3.0)
URL: https://jpos.org
Note: jPOS is used as an unmodified runtime dependency for ISO8583 message 
parsing and packaging. Dynamic Mock does not modify jPOS source code. 
jPOS also offers a commercial license for organizations that require 
non-AGPL terms. Contact: https://jpos.org/license

## GraalVM Polyglot (org.graalvm.polyglot:*)
License: Universal Permissive License (UPL-1.0)

## Spring Boot (org.springframework.boot:*)
License: Apache License 2.0

## Handlebars.java (com.github.jknack:handlebars)
License: Apache License 2.0

## Protocol Buffers (com.google.protobuf:*)
License: BSD 3-Clause

## gRPC (io.grpc:*)
License: Apache License 2.0
```

### Complexity
Very Low. ~2 hours.

### Dependencies
None.

---

## 5. RECOMMENDED SPRINT 1 EXECUTION ORDER

| Order | Deliverable | Rationale |
|---|---|---|
| **1** | CI Pipeline + LICENSE + THIRD_PARTY_LICENSES.md | Foundation. Establish quality gate and legal posture before any code changes. Every subsequent commit gets CI coverage. |
| **2** | ISO8583 Data Dictionary (frontend static JSON + ResponseFieldsEditor enhancement) | No backend changes needed. Immediate visible UX improvement. Low risk. Creates momentum. |
| **3** | Packager Upload (backend API + entity changes + frontend upload UI) | Requires backend + frontend changes. Higher complexity. Benefits from CI already being in place to validate changes. |
| **4** | Build verification + WORKFLOW.md update | Verify full build. Update sprint status. |

**Rationale for this order**: CI first ensures that Data Dictionary and Packager Upload changes are validated automatically. Data Dictionary is frontend-only and low risk, so it ships quickly, creating a visible win. Packager Upload is the most complex deliverable and benefits from the CI safety net.

---

## 6. ITEMS EXPLICITLY DEFERRED

The following are **not** part of Sprint 1 per roadmap authority:

- ISO8583 Message Simulator (Sprint 2)
- Scenario State Inspector (Sprint 2)
- Dynamic dictionary from uploaded packager XML (future enhancement)
- Bitmap visualization (Sprint 11)
- Authentication (Sprint 4 per revised roadmap)
- White Label (Sprints 5-6)

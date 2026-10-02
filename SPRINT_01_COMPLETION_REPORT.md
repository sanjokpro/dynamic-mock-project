# Sprint 1 Completion Report

**Sprint**: Sprint 1 — ISO8583 Productization Foundation  
**Date Completed**: 2026-10-01  
**Status**: ✅ COMPLETE  
**Governing Documents**: AI_GOVERNANCE.md, MASTER_PRODUCT_ACTION_PLAN.md, ROADMAP_AUTHORITY.md

---

## Build & Test Verification

| Check | Result |
|---|---|
| Backend compile (`./gradlew compileJava`) | ✅ BUILD SUCCESSFUL |
| Backend unit tests (`./gradlew test`) | ✅ BUILD SUCCESSFUL |
| Frontend build (`npm run build`) | ✅ Compiled successfully |
| Frontend tests (`npm run test`) | ✅ 11/11 tests passed (3 test files) |

---

## Deliverables

### 1. GitHub Actions CI Pipeline ✅

**File**: [`.github/workflows/ci.yml`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/.github/workflows/ci.yml)

- Two-job pipeline: `backend` (Java 21, Gradle) and `frontend` (Node 20, npm)
- Triggers on push and pull requests to `main`, `develop`, `feature/**`
- Gradle dependency caching with `actions/cache@v4`
- Test result artifacts uploaded on completion
- Also added missing `gradle-wrapper.jar` to repository (it was absent)

---

### 2. LICENSE + THIRD_PARTY_LICENSES.md ✅

**Files**:
- [`LICENSE`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/LICENSE) — Apache License 2.0
- [`THIRD_PARTY_LICENSES.md`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/THIRD_PARTY_LICENSES.md)

**jPOS licensing** (documented in `THIRD_PARTY_LICENSES.md`):

Dynamic Mock is distributed under the Apache License 2.0. jPOS is distributed separately under AGPL-3.0 and is used by Dynamic Mock as an unmodified runtime dependency. Dynamic Mock's ISO8583 adapter layer (`Iso8583Server.java`, `Q2ServerManager.java`) interacts with jPOS through its public API.

Organizations should independently review the licensing requirements of all third-party dependencies and consult qualified legal counsel if they have questions regarding their specific usage, distribution, hosting, or compliance obligations. For organizations that require commercial licensing terms for jPOS, additional licensing options may be available from the jPOS project.

All major backend and frontend dependencies documented (Spring Boot, GraalVM, gRPC, Protocol Buffers, Handlebars, Jackson, Lombok, Next.js, React, Tailwind, Monaco Editor, etc.).

---

### 3. ISO8583 Data Dictionary ✅

**Files**:
- [`frontend/src/data/iso8583-fields.json`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/data/iso8583-fields.json) — 128-field dictionary
- [`frontend/src/components/workspace/protocols/ProtocolEditor.tsx`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/components/workspace/protocols/ProtocolEditor.tsx) — updated

**Dictionary Contents**:
- All 128 ISO 8583:1987 standard data elements with name, abbreviation, format, and description
- 24 standard MTI descriptions
- 39 common response code descriptions (00=Approved, 51=Insufficient funds, etc.)

**UI Improvements**:
- `FieldNumberInput` component: autocomplete dropdown with live search (field number, name, or abbreviation)
- Inline label shown under the field input (e.g. `PAN — Primary Account Number`)
- Tooltip shows format and full description on hover
- Custom (non-dictionary) field numbers still work for non-standard packagers
- MTI dropdown now covers all 24 standard MTIs (was 9 hardcoded entries)
- Applied to both Response Fields and Field Matchers sections

---

### 4. Packager Upload ✅

#### Backend

**Domain / Persistence Layer**:
- [`Iso8583Endpoint.java`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/backend/src/main/java/com/dynamicmock/domain/entity/Iso8583Endpoint.java) — `packagerXmlContent`, `packagerName` fields
- [`Iso8583EndpointMongoEntity.java`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/backend/src/main/java/com/dynamicmock/infrastructure/persistence/mongodb/entity/Iso8583EndpointMongoEntity.java) — same fields
- [`Iso8583EndpointMapper.java`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/backend/src/main/java/com/dynamicmock/infrastructure/persistence/mongodb/mapper/Iso8583EndpointMapper.java) — propagates new fields
- [`Iso8583EndpointResponse.java`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/backend/src/main/java/com/dynamicmock/adapter/in/web/dto/Iso8583EndpointResponse.java) — exposes `packagerName` and `hasCustomPackager`

**Storage Design**: XML stored inline in MongoDB as a String field. Packager XMLs are 5-30 KB — small enough for inline storage without GridFS or file system dependencies. Survives Docker restarts, works in scaled deployments.

**Service Layer**:
- [`Iso8583Service.java`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/backend/src/main/java/com/dynamicmock/application/service/Iso8583Service.java) — `uploadPackager()` and `removePackager()`
  - `uploadPackager()`: validates by instantiating `GenericPackager` from uploaded bytes (fail-fast before any persistence); saves to MongoDB; hot-reloads active server
  - `removePackager()`: nulls content and name; hot-reloads active server

**API Layer**:
- [`Iso8583Controller.java`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/backend/src/main/java/com/dynamicmock/adapter/in/web/Iso8583Controller.java)
  - `POST /api/iso8583/endpoints/{id}/packager` — multipart upload, returns 400 on invalid XML
  - `DELETE /api/iso8583/endpoints/{id}/packager` — resets to default

**Bug Fix** (critical, from discovery):
- [`Iso8583Server.createPackager()`](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/backend/src/main/java/com/dynamicmock/adapter/out/protocol/iso8583/Iso8583Server.java) — was always loading bundled default. Now: custom `packagerXmlContent` → bundled default → ISO87A fallback.

#### Frontend

- Packager Definition section added to `Iso8583Editor`
- Shows: "Using bundled ISO 8583:1987 default" or custom packager filename in green
- Upload button triggers hidden `<input type="file" accept=".xml">` 
- Error display for invalid XML or network failure
- "Reset to Default" button triggers DELETE endpoint
- Upload state prevents double-submission

---

## Files Created / Modified

| File | Action |
|---|---|
| `.github/workflows/ci.yml` | ✅ Created |
| `backend/gradle/wrapper/gradle-wrapper.jar` | ✅ Added (was missing) |
| `LICENSE` | ✅ Created |
| `THIRD_PARTY_LICENSES.md` | ✅ Created |
| `CHANGELOG.md` | ✅ Updated |
| `README.md` | ✅ Updated |
| `frontend/WORKFLOW.md` | ✅ Updated |
| `frontend/src/data/iso8583-fields.json` | ✅ Created |
| `frontend/src/components/workspace/protocols/ProtocolEditor.tsx` | ✅ Updated |
| `backend/src/main/java/com/dynamicmock/domain/entity/Iso8583Endpoint.java` | ✅ Updated |
| `backend/.../mongodb/entity/Iso8583EndpointMongoEntity.java` | ✅ Updated |
| `backend/.../mongodb/mapper/Iso8583EndpointMapper.java` | ✅ Updated |
| `backend/.../web/dto/Iso8583EndpointResponse.java` | ✅ Updated |
| `backend/.../web/Iso8583Controller.java` | ✅ Updated |
| `backend/.../service/Iso8583Service.java` | ✅ Updated |
| `backend/.../protocol/iso8583/Iso8583Server.java` | ✅ Bug fixed |
| `SPRINT_01_DISCOVERY.md` | ✅ Created (discovery) |
| `SPRINT_01_COMPLETION_REPORT.md` | ✅ Created (this file) |

---

## Backward Compatibility

All changes are fully backward compatible:

- Existing `Iso8583Endpoint` documents in MongoDB with no `packagerXmlContent` field will be read as `null` → bundled default packager used (existing behavior)
- Existing `packagerConfig` field preserved
- `hasCustomPackager: false` returned for all existing endpoints
- No database migration required

---

## What Was Not Done (By Design)

Per roadmap governance — Sprint 1 scope only:

| Item | Reason Not Included |
|---|---|
| Dynamic dictionary from uploaded packager XML | Sprint 2+ enhancement |
| ISO8583 Message Simulator | Sprint 2 |
| Scenario State Inspector | Sprint 2 |
| Bitmap visualization | Sprint 11 |
| RBAC / Authentication | Sprint 4 |
| White Label implementation | Sprints 5-6 |

---

## Next Sprint

**Sprint 2**: ISO8583 Message Simulator + Scenario State Inspector

Per `MASTER_PRODUCT_ACTION_PLAN.md`.

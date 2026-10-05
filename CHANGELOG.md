# Changelog

All notable changes to this project will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased] — Sprint 2 (2026-10-04)

### Added

#### ISO8583 Message Simulator
- `Iso8583PackagerFactory` — shared packager resolution component (custom XML → bundled → ISO87A fallback); extracted from `Iso8583Server` for reuse
- `POST /api/iso8583/endpoints/{id}/simulate` — backend TCP proxy endpoint; packs request, opens TCP to mock server, unpacks response, returns decoded JSON
- `Iso8583SimulatorService` — service layer for simulator with structured error types: `ENDPOINT_INACTIVE`, `CONNECTION_REFUSED`, `TIMEOUT`, `PACK_ERROR`, `UNPACK_ERROR`
- `Iso8583SimulateRequest` / `Iso8583SimulateResponse` — new DTOs for simulator API
- Frontend Simulator Panel inside `ProtocolEditor.tsx`:
  - "Simulator" tab alongside existing "Configuration" tab
  - MTI selector (all common MTIs with descriptions)
  - Field input table with ISO8583 data dictionary labels and format hints
  - Pre-built templates for 0100, 0200, 0420
  - Send button with loading state
  - Response viewer: decoded fields table with dictionary labels, Field 39 response code with human-readable description, copy-to-clipboard
  - Raw hex toggle for request and response
  - Structured error display per error type
- `useIso8583Simulator` — React Query mutation hook for simulator API calls

#### Scenario State Inspector
- `GET /api/scenarios/{id}/state/details` — new endpoint returning `currentState`, `initialState`, `executionCount`, `stateVariables`, `active`
- `ScenarioService.getExecutionCount()` — reads execution count from Redis
- `ScenarioService.getStateVariables()` — reads global script state hash from Redis
- `ScenarioStateInspector` component embedded in `ScenarioEditor.tsx`:
  - Live current state display with initial-state indicator
  - Execution count
  - Script variables table (Redis hash entries)
  - 5-second auto-refresh via `useQuery` `refetchInterval`
  - Manual refresh button
  - Loading and empty-state handling
- `useScenarioStateDetails` — React Query polling hook (5s interval, 1s staleTime)

#### Tests
- `Iso8583SimulatorServiceTest` — unit tests covering inactive endpoint, connection refused, missing endpoint, packager factory delegation

---

## [Unreleased] — Sprint 1 (2026-10-01)


### Added

#### CI/CD
- `.github/workflows/ci.yml` — GitHub Actions pipeline (backend Gradle/Java 21 + frontend Node 20)
- `backend/gradle/wrapper/gradle-wrapper.jar` — missing from repository, added

#### Licensing
- `LICENSE` — Apache License 2.0
- `THIRD_PARTY_LICENSES.md` — Third-party attributions, including jPOS AGPL analysis

#### ISO8583 Data Dictionary
- `frontend/src/data/iso8583-fields.json` — Complete 128-field ISO 8583:1987 data dictionary with MTI descriptions and common response codes
- `ProtocolEditor.tsx` — `FieldNumberInput` with search/autocomplete (field number, name, or abbreviation)
- `ProtocolEditor.tsx` — Response fields and matchers now show labeled field names as you type
- `ProtocolEditor.tsx` — MTI dropdown built from dictionary (all 24 standard MTIs)

#### ISO8583 Packager Upload
- `Iso8583Endpoint` — added `packagerXmlContent` and `packagerName` fields (MongoDB-backed inline storage)
- `Iso8583EndpointMongoEntity` + `Iso8583EndpointMapper` — propagate new fields
- `Iso8583EndpointResponse` — exposes `packagerName` and `hasCustomPackager`
- `Iso8583Controller` — `POST /api/iso8583/endpoints/{id}/packager` and `DELETE /api/iso8583/endpoints/{id}/packager`
- `Iso8583Service` — `uploadPackager()` (validate + persist + hot-reload) and `removePackager()`
- `Iso8583Editor` — Packager Definition section with upload, filename display, Reset to Default

### Fixed
- `Iso8583Server.createPackager()` — **Critical bug**: always loaded bundled default, ignoring `endpoint.packagerConfig`. Now correctly uses uploaded `packagerXmlContent` first.

### Changed
- `README.md` — Enterprise positioning, CI/license badges, restructured features

---

## [Pre-Sprint 1]

### Added
- **Frontend**: Vitest and React Testing Library setup.
- **Frontend**: `useScenarios.test.tsx` containing basic tests for the scenarios API hook.
- **Frontend**: A view toggler in the Sidebar to switch the main content area between "Routes" and "Scenarios".
- **Frontend**: Basic error handling and a "New" scenario creation button within `ScenarioEditor.tsx`.
- **Frontend**: Added `sonner` for toast notifications and implemented `DialogContext` for custom modal dialogs.

### Fixed
- **Frontend**: Recovered `ScenarioEditor.tsx` from an orphaned/dead state by integrating it properly into `WorkspaceLayout.tsx` using an `activeView` context state.
- **Frontend**: Implemented full `EnvironmentManagerModal.tsx` allowing for Environment CRUD, variables editing, and cloning.
- **Frontend**: Enhanced Collection Management in `Sidebar.tsx` to support renaming, cloning, and deleting collections.
- **Frontend**: Added ability to move routes between collections via a contextual UI.

### Changed
- **Frontend**: Updated `Header.tsx` to include an Environment Settings button to open the Environment Manager Modal.
- **Frontend**: Replaced all native blocking dialogs (`alert()`, `confirm()`, `prompt()`) with custom UI modals and toasts.

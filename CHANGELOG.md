# Changelog

All notable changes to this project will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

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

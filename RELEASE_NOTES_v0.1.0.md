# Release Notes v0.1.0

**Date**: 2026-10-02  
**Milestone**: Sprint 1 Complete — ISO8583 Productization Foundation

This release marks a significant milestone in transitioning the platform from a generic Dynamic Mock tool to an **Enterprise Integration Simulation Platform**. It focuses heavily on hardening ISO8583 capabilities, establishing a robust governance framework, and elevating overall code quality.

---

## Highlights

### 🏛️ Governance
- **AI Governance Established**: Created strict guidelines (`AI_GOVERNANCE.md`) ensuring all future changes align with the product roadmap.
- **Roadmap Authority Established**: Introduced `ROADMAP_AUTHORITY.md` and finalized the `MASTER_PRODUCT_ACTION_PLAN.md` to guide iterative product growth securely and strategically.

### 💻 Frontend
- **Scenario Recovery**: Resurrected the `ScenarioEditor.tsx` from an orphaned state, enabling robust Stateful Scenarios.
- **Environment Management**: Introduced `EnvironmentManagerModal.tsx` for full CRUD, editing, and cloning of variables.
- **Collection Management**: Implemented renaming, cloning, and deletion in the Sidebar, along with moving routes contextually.
- **Quality Improvements**: 
  - Integrated `sonner` for non-blocking toast notifications.
  - Implemented `DialogContext` for custom, centralized modal management.
  - Test coverage via Vitest and React Testing Library setup with basic tests.

### 🏦 ISO8583
- **Stateful Scenario Integration**: Integrated ISO8583 with the unified scenario engine.
- **Data Dictionary**: Implemented a comprehensive, 128-field ISO 8583:1987 data dictionary in the UI with search and autocomplete support (`iso8583-fields.json`).
- **Packager Upload**: Introduced an API and UI to upload, store (in MongoDB), validate, and hot-reload custom `packager.xml` definitions. Reverts smoothly to standard ISO 8583:1987 when cleared.
- **Productization Improvements**: Fixed a critical bug in `createPackager()` that prevented custom packagers from being applied correctly, improving fallback behavior.

### 🚀 DevOps
- **GitHub Actions CI**: Set up `.github/workflows/ci.yml` supporting end-to-end building and testing for both the Java 21 backend and Node 20 Next.js frontend on push/PRs.
- **Licensing Documentation**: 
  - Adopted **Apache License 2.0** (`LICENSE`).
  - Cataloged all dependencies in `THIRD_PARTY_LICENSES.md`, specifically analyzing the enterprise implication of jPOS (AGPL-3.0) as an unmodified runtime dependency. Language was carefully crafted in `LICENSING_WORDING_REVIEW.md` to provide transparency without legal interpretation.

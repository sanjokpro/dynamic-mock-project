# Frontend Gap Analysis

## Overview
This document contains the functional audit of the `dynamic-mock-project` frontend. The audit evaluates the codebase (`src/app/` Next.js App Router codebase) to determine the readiness, functional gaps, and backend dependencies of every major workflow.

## Feature Area Status

| Feature Area | Implemented % | Status | Backend Dependency Status |
|---|---|---|---|
| 1. Route Management | 85% | ⚠ Partial | Fully Mapped (`/api/routes`) |
| 2. Collection Management | 70% | ⚠ Partial | Mapped, but lacks User context |
| 3. Environment Management | 40% | ⚠ Partial | Mapped (`/api/environments`), Read-Only UI |
| 4. Traffic Console | 100% | ✅ Functional | Mapped via WebSocket (`/ws-traffic`) |
| 5. Scenario Builder | 0% | ❌ Broken | Mapped (`/api/scenarios`), but Disconnected UI |
| 6. GraphQL Designer | 60% | ⚠ Partial | Mapped via Protocol Editor |
| 7. gRPC Designer | 60% | ⚠ Partial | Mapped via Protocol Editor |
| 8. ISO8583 Designer | 60% | ⚠ Partial | Mapped via Protocol Editor |
| 9. Authentication | 0% | ❌ Broken | Hardcoded API Keys (`NEXT_PUBLIC_API_KEY`) |
| 10. Branding | 20% | 🚧 Placeholder | Uses basic Tailwind, missing requested Theme |
| 11. User Management | 0% | ❌ Broken | Hardcoded to `default-user` |

## Page & Component Level Audit

### 1. Main Workspace (`app/page.tsx`, `components/workspace/WorkspaceLayout.tsx`)
- **Path**: `/`
- **Purpose**: Single Page Application rendering the dynamic mock workspace.
- **APIs used**: Orchestrates children components.
- **Status**: ✅ Functional

### 2. Request Panel (`components/workspace/RequestPanel.tsx`)
- **Purpose**: Edit HTTP routes and mock definitions.
- **APIs used**: `useRoutes` (PUT /routes).
- **CRUD Support**: Read, Update.
- **Validation**: Minimal, relies on React state. Placeholder inputs exist for body matchers (e.g. `$.field == value`).
- **Error handling**: Minimal, silently fails in UI on mutation errors.
- **Loading state**: Missing distinct loading spinners during saves.
- **Status**: ⚠ Partial.

### 3. Protocol Editor (`components/workspace/protocols/ProtocolEditor.tsx`)
- **Purpose**: Handles non-HTTP protocols (GraphQL, gRPC, ISO8583).
- **APIs used**: Custom save handlers mapping to backend protocol endpoints.
- **Status**: ⚠ Partial (Functions as a monolithic component replacing RequestPanel for specific routes).

### 4. Scenario Editor (`components/workspace/scenarios/ScenarioEditor.tsx`)
- **Purpose**: Manage stateful scenarios.
- **APIs used**: `useScenarios`.
- **Status**: ❌ Broken. It is a completely dead component. It is never imported or rendered in the `WorkspaceLayout` or any parent view.

### 5. Header / Environments (`components/workspace/Header.tsx`)
- **Purpose**: Global actions and environment switching.
- **APIs used**: `useEnvironments`.
- **CRUD Support**: Read only. No UI to Create, Update, or Delete environments.
- **Status**: ⚠ Partial.

### 6. Sidebar / Collections (`components/workspace/Sidebar.tsx`)
- **Purpose**: Browse and organize routes into collections.
- **APIs used**: `useCollections`, `useRoutes`.
- **CRUD Support**: Create, Read. Lacks UI for updating or deleting collections.
- **Status**: ⚠ Partial.

## Technical Debt & Code Smells
- **Dead Components**: `ScenarioEditor.tsx` is completely orphaned.
- **Unused API Hooks**: `useScenarios.ts` mutations are never practically executed because the UI is dead.
- **Hardcoded State**: `userId = 'default-user'` throughout `Sidebar.tsx` and related hooks.
- **Placeholder Authentication**: `apiClient.ts` handles authorization via a static fallback string (`'default-dev-key'`).
- **Placeholder Implementations**: Route drawer and matchers have UI text placeholders but lack underlying logical implementation for execution constraints.

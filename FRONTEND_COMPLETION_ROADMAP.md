# Frontend Completion Roadmap

This roadmap organizes the findings from the gap analysis into an actionable implementation plan, prioritized by core workflow necessity.

## P0: Broken Core Workflows (Immediate Action)
*Blockers for basic end-to-end usage.*

1. **Environment Management (CRUD)**
   - **Gap**: Environments can be read and selected, but there is no UI to create, update, or delete them.
   - **Task**: Build an Environment Manager modal. Hook it to existing `useEnvironments` mutations.
   - **Readiness Estimate**: 1-2 Days.

2. **Revive Scenario Builder**
   - **Gap**: `ScenarioEditor.tsx` is a dead component.
   - **Task**: Integrate Scenario Builder into the `WorkspaceLayout` (e.g., as a new tab alongside Routes, or a separate global view). Connect it to `useScenarios`.
   - **Readiness Estimate**: 2-3 Days.

3. **Authentication & User Context**
   - **Gap**: Users are hardcoded (`default-user`).
   - **Task**: Implement an Auth wall (e.g., NextAuth.js or custom login page) and pass actual authenticated user IDs to API calls (`useCollections`).
   - **Readiness Estimate**: 3-5 Days.

## P1: Partial Workflows (Enhancements)
*Core features that are partially built but missing critical polish.*

1. **Request Panel Matchers Validation**
   - **Gap**: Placeholders exist for JSONPath and XPath matchers, but no UI validation or advanced editing constraints exist.
   - **Task**: Implement robust validation for advanced request matchers.
   - **Readiness Estimate**: 1-2 Days.

2. **Collection Management Refinement**
   - **Gap**: Sidebar collections can only be created and read. Cannot be renamed, deleted, or reorganized.
   - **Task**: Implement Collection context menus (Rename/Delete) and drag-and-drop within the Sidebar.
   - **Readiness Estimate**: 2 Days.

3. **Protocol Editor Polish**
   - **Gap**: gRPC, GraphQL, and ISO8583 share a single monolithic `ProtocolEditor`.
   - **Task**: Segregate protocol editors into dedicated components for cleaner state management.
   - **Readiness Estimate**: 3 Days.

## P2: UX Improvements
*Quality of life enhancements.*

1. **Loading States and Error Handling**
   - **Gap**: Silently failing API mutations.
   - **Task**: Add toast notifications (e.g., `sonner` or `react-hot-toast`) for global success/error handling in API interceptors. Add inline loading spinners on save buttons.
   - **Readiness Estimate**: 1 Day.

2. **Theme and Branding**
   - **Gap**: Missing the distinct "Nepali rupee papernote theme" specified in earlier documentation.
   - **Task**: Update Tailwind config and CSS variables to reflect the proprietary brand colors.
   - **Readiness Estimate**: 1 Day.

## P3: New Functionality
*Future architectural shifts.*

1. **Role-Based Access Control (RBAC)**
   - **Gap**: Once authentication is complete, no UI exists for organizational permissions.
   - **Task**: Implement admin views for user management and workspace sharing.
   - **Readiness Estimate**: 5+ Days.

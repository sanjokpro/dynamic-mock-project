# Scenario Recovery Analysis

## 1. Why ScenarioEditor became orphaned
The `ScenarioEditor.tsx` component was built with a complete UI (including a React Flow diagram and state machine cards) but was never imported into the main application tree. The `WorkspaceLayout.tsx` statically renders the `RequestPanel` and `ResponsePanel` in the main content area without any conditional routing or view state to toggle between different primary tasks (e.g., editing routes vs. managing scenarios). 

## 2. Intended Integration Point
`ScenarioEditor` is designed to consume the full height and width of the main content area (`h-full flex flex-col`). The intended integration point is the `main-content-area` within `WorkspaceLayout.tsx`. When a user navigates to the "Scenarios" view, the layout should swap out the split-pane `RequestPanel` and `ResponsePanel` for the `ScenarioEditor`.

## 3. Missing Routing/State Wiring
- **WorkspaceContext**: Lacks an `activeView` state (e.g., `'routes' | 'scenarios'`).
- **Sidebar UI**: Lacks navigation buttons/tabs to switch the `activeView` between Routes (Collections) and Scenarios.
- **WorkspaceLayout**: Needs to conditionally render `ScenarioEditor` based on `activeView`, bypassing the split-pane logic when in scenario mode.
- **Scenarios API**: `useScenarios.ts` has CRUD hooks mapped, but they need to be wired into a "Create Scenario" button, as `ScenarioEditor` currently only edits existing scenarios but lacks a top-level creation button.

## 4. Missing Tests
The `frontend/` directory has absolutely no `.test.ts` or `.test.tsx` files. Unit tests for `ScenarioEditor.tsx` and the `useScenarios` hook are entirely missing. A testing framework setup (like Jest/React Testing Library) might be missing or underutilized.

## Implementation Plan (Minimum Required Changes)
1. Add `activeView: 'routes' | 'scenarios'` to `WorkspaceContext.tsx`.
2. Add a view toggle switch at the top of `Sidebar.tsx`.
3. Update `WorkspaceLayout.tsx` to conditionally render `ScenarioEditor` instead of the Request/Response panels when `activeView === 'scenarios'`.
4. Add a "Create Scenario" button inside `ScenarioEditor.tsx` to utilize `createScenarioMutation`.
5. Add basic error and loading handling via UI toast/alerts in `ScenarioEditor.tsx` and `useScenarios.ts`.
6. Add basic unit test for `useScenarios.ts` (if test infra is missing, provide a minimal Jest/RTL file).

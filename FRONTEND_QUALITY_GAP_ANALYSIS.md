# Frontend Quality & UX Gap Analysis

## 1. Native Dialog Usage (Tech Debt)
The application heavily relies on blocking native dialogs which provide a poor user experience and cannot be styled.
- **`prompt()`**:
  - `Sidebar.tsx`: Collection creation, collection renaming, adding a route.
  - `Header.tsx`: Creating a new route.
  - `ScenarioEditor.tsx`: Creating a new scenario.
- **`confirm()`**:
  - `RouteDrawer.tsx`: Deleting a stub.
  - `Sidebar.tsx`: Deleting a collection, removing a route from a collection.
  - `ScenarioEditor.tsx`: Deleting a scenario.
  - `EnvironmentManagerModal.tsx`: Deleting an environment.
- **`alert()`**:
  - Widespread use for error handling across `Sidebar.tsx`, `ScenarioEditor.tsx`, and `EnvironmentManagerModal.tsx` when API mutations fail.

## 2. Missing Notifications (Toasts)
- No toast notification system exists. Successful actions (e.g., "Saved successfully") provide no feedback to the user.
- Errors are shown via `alert()` instead of non-blocking error toasts.

## 3. Missing Loading States
- While initial data fetching uses spinners, many mutations (creating, updating, deleting) lack UI feedback.
- Buttons do not transition to a loading state during asynchronous operations.

## 4. Missing Form Validation
- Inputs gathered via `prompt()` lack validation. Users can submit empty strings or whitespace.
- No client-side uniqueness checks before submission.

## 5. Missing Empty States
- Empty states are basic text (e.g., "No collections found").
- They lack clear call-to-actions (e.g., a prominent "Create your first collection" button).

## 6. Missing Error Boundaries
- Unhandled render errors will crash the entire application since there are no React Error Boundaries implemented around major workspace panels.

## Recommendations
1. Install a toast library (e.g., `sonner` or `react-hot-toast`).
2. Replace all `prompt()` and `confirm()` calls with a centralized Dialog component or Modal context.
3. Enhance mutation buttons with loading spinners.
4. Implement basic validation for all forms (required fields, no empty strings).
5. Add a top-level Error Boundary.

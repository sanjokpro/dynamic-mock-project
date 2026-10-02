# Environment Gap Analysis

## 1. Existing Functionality
- **Data Transfer Objects (DTOs)**: Defined in `frontend/src/types/index.ts`. Includes `Environment` (id, name, variables, isDefault).
- **API Hooks**: `frontend/src/hooks/useEnvironments.ts` implements a full suite of React Query hooks (`useQuery` and `useMutation`) for mapping CRUD operations to `/environments`.
- **Workspace Integration**: `WorkspaceContext` manages `activeEnvironment` state.
- **UI Element**: `Header.tsx` includes an "Environment Selector" dropdown to switch the active environment.

## 2. Missing CRUD Operations
While the backend API hooks are implemented, the **UI is completely missing** for:
- Creating a new environment.
- Editing an existing environment (name or variables).
- Deleting an environment.
- Cloning an existing environment.

## 3. Missing Validation
- No validation ensuring environment names are non-empty and unique.
- No validation for variable keys (e.g., preventing duplicate keys, enforcing valid characters, avoiding empty keys).

## 4. Missing UX States
- No UI components to display loading spinners during environment creation/updates.
- No error handling (toast/alerts) if an environment mutation fails.
- No confirmation dialogs to prevent accidental deletion of an environment.

## 5. Missing Import/Export
- There is an `ImportModal.tsx` that appears to handle importing collections/routes, but dedicated import/export functionality specifically for individual environments (e.g., JSON export) is missing or not exposed at the environment management level.

## 6. Missing Variable Editor Capabilities
- There is no Key-Value grid or table editor to manage the `variables` record inside an environment. A proper editor is needed to add new rows, edit existing keys/values, and remove variables.

## Recommendation
Implement an `EnvironmentModal.tsx` or `EnvironmentManager.tsx` that can be opened from the Header or Sidebar. This component should provide a list of environments and a detail view with a Key-Value editor for the selected environment, supporting all CRUD operations, cloning, validations, and loading/error states.

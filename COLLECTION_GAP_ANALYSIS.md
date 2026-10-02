# Collection Management Gap Analysis

## 1. Existing Functionality
- **API Hooks**: `useCollections.ts` has full React Query hooks (`createCollection`, `updateCollection`, `deleteCollection`, `addItemToCollection`).
- **Sidebar Integration**: Collections are rendered in an accordion style in `Sidebar.tsx`.
- **Create UI**: Users can create a collection, but it relies on a rudimentary native browser `prompt()`.
- **Add Route**: Users can add a new route to a collection, also relying on a native `prompt()`.

## 2. Missing Functionality
- **Rename Collection**: No UI to trigger the `updateCollection` mutation to change a collection's name.
- **Delete Collection**: No UI (e.g., a trash icon or context menu) to delete an existing collection.
- **Move Route**: No drag-and-drop or context menu functionality to move an existing route from one collection to another (or out of a collection).
- **Clone Collection**: No UI to duplicate a collection and its items.
- **Nested Collections**: The `Collection` DTO lacks a `parentId` or hierarchical structure, so true nested collections are not supported at the API or UI level.
- **Bulk Operations**: Cannot select multiple collections or routes for deletion or movement.

## 3. Partial Functionality
- **Creation**: Collection creation works but uses a native `prompt()` instead of a styled modal.

## 4. UX Issues
- **Error Handling**: Currently uses `console.error` without notifying the user (e.g., in `handleAddRoute`).
- **Loading States**: No inline loading spinners when a collection is being created, renamed, or deleted.
- **Accessibility/Discoverability**: Context actions (rename, delete, clone) are completely missing from the collection items in the sidebar.

## 5. Backend Contract Verification
- The backend contract supports basic CRUD (`/collections`, `/collections/{id}`, `/collections/{collectionId}/items`). Moving items might require deleting from the old collection and adding to the new one, or a dedicated endpoint.

## 6. Dead Code / Tech Debt
- Using `userId = 'default-user'` hardcoded in `Sidebar.tsx`.
- Inline `prompt()` usage is technical debt for a modern React application.

## Classification
- **Functional**: ✅ Listing collections, basic creation.
- **Partial**: ⚠ Error handling, loading states.
- **Missing**: 🚧 Rename, delete, clone, move routes, nested collections.

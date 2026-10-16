# Post-v0.4.0 Critical QA Fixes Report

**Date:** 2026-10-16  
**Status:** PASS  
**Scope:** Response Body Correctness & Test Request Draft Persistence  

---

## 1. Executive Summary

During post-v0.4.0 manual quality assurance, two critical usability defects were discovered in the workspace interface:
1. **Response Body Mismatch (Bug 1):** In the Response panel, string values in JSON responses were swallowed by a faulty regex tokenizer in `highlightJson`, causing `{"reached":"head2"}` to display as `{"reached":""}`.
2. **Test Request Data Lost on Refresh (Bug 2):** On page refresh, the user's working draft (Request Headers, Query Params, Request Body Type, and Request Body Content) was wiped out, and the active route selection was reset to `null`.

Both defects have been fixed with surgical precision. All 17 frontend unit tests and the Next.js production build pass with 0 errors. Backend Gradle compilation and tests remain 100% green.

---

## 2. Root Cause Analysis

### Bug 1: Response Body Mismatch (`{"reached":""}` instead of `{"reached":"head2"}`)

- **Location:** [ResponsePanel.tsx](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/components/workspace/ResponsePanel.tsx#L244)
- **Root Cause:**
  The `highlightJson` function used the following regular expression for tokenizing JSON strings:
  ```javascript
  const regex = /("(?:\\.|[^"\\])*")\s*(:)|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|(\btrue\b|\bfalse\b)|(\bnull\b)|([{}[\]])|([,:])|([^\s{}[\]",:]+)/g;
  ```
  Group 1 `("(?:\\.|[^"\\])*")\s*(:)` had a **mandatory** colon `(:)` following the string literal.
  - When parsing a JSON key (e.g., `"reached":`), Group 1 matched because it was followed by `:`.
  - When parsing a JSON string *value* (e.g., `"head2"`), Group 1 failed because there was no colon after `"head2"`.
  - None of the subsequent groups matched the opening quote `"`. The opening quote was treated as un-matched whitespace/text.
  - The token `head2` matched fallback branch 7 (`([^\s{}[\]",:]+)`), but the tokenizer loop only processed `match[1]` through `match[7]`. `match[8]` had no handler and was dropped.
  - The closing quote `"` was appended as plaintext.
  - As a result, any JSON string value without a colon was stripped of its content, transforming `{"reached":"head2"}` into `{"reached":""}`.

### Bug 2: Test Request Data Lost on Refresh

- **Location:** [RequestPanel.tsx](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/components/workspace/RequestPanel.tsx#L42-L95), [Sidebar.tsx](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/components/workspace/Sidebar.tsx#L46-L61), [WorkspaceContext.tsx](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/context/WorkspaceContext.tsx#L28-L45)
- **Root Cause:**
  1. **Active Route Reset:** `WorkspaceContext` initialized `activeRoute` with `useState(null)`. On browser refresh, `activeRoute` defaulted to `null`, forcing the user to re-select the route from the sidebar.
  2. **Draft Wiped in Lifecycle:** When `activeRoute` was set, `RequestPanel.tsx`'s `useEffect([activeRoute])` forcefully reset `requestHeaders` to `{}`, `requestBodyType` to `'none'`, and `requestBodyContent` to `''`.
  3. **No Persistence Layer:** There was no local caching mechanism in place for test request drafts (headers, params, body).

---

## 3. Files Changed & Technical Fix

### 1. [ResponsePanel.tsx](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/components/workspace/ResponsePanel.tsx)
- Corrected regex in `highlightJson` to make the trailing colon optional:
  ```javascript
  const regex = /("(?:\\.|[^"\\])*")(?:\s*(:))?|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|(\btrue\b|\bfalse\b)|(\bnull\b)|([{}[\]])|([,:])/g;
  ```
- Evaluated `const isKey = !!match[2];`:
  - If `isKey` is true, the string token is colored with `colors.key` and followed by the `:` punctuation token.
  - If `isKey` is false, the string token is colored with `colors.string`.
- Added `formattedBody` JSON formatter to ensure compact JSON strings returned from mock endpoints are pretty-printed for visual clarity.
- Exported `highlightJson` for direct unit testing.

### 2. [WorkspaceContext.tsx](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/context/WorkspaceContext.tsx)
- Wrapped `setActiveRoute` to persist the selected route's ID in `localStorage` under `dynamicmock:active_route_id`.

### 3. [Sidebar.tsx](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/components/workspace/Sidebar.tsx)
- Added an initial restoration effect: when the route list loads and `!activeRoute`, it automatically restores the route matching `localStorage.getItem('dynamicmock:active_route_id')`.

### 4. [RequestPanel.tsx](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/components/workspace/RequestPanel.tsx)
- Implemented route-scoped draft persistence in `localStorage`:
  - Storage key: `dynamicmock:test_draft:${activeRoute.id}`
  - Restored fields: `requestHeaders`, `queryParams`, `requestBodyType`, and `requestBodyContent`.
- Added an edit listener effect that debounces and saves user edits to `localStorage`.
- In `handleSend`, automatically merged `queryParams` from the draft into `targetUrl` query string.

### 5. New Unit Tests
- [ResponsePanel.test.tsx](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/components/workspace/ResponsePanel.test.tsx): Validates that `highlightJson` preserves string values like `{"reached":"head2"}` and properly handles complex JSON structures.
- [DraftPersistence.test.ts](file:///Users/sanjokkhakurel/GITHUB-REPO/dynamic-mock-project/frontend/src/components/workspace/DraftPersistence.test.ts): Validates localStorage draft serialization, route-level draft isolation, and reload restoration.

---

## 4. Fix Verification Steps

### Automated Verification
```bash
# Frontend Unit Tests (Vitest)
npm test
# Result: 5 test files passed, 17 tests passed (0 failures)

# Frontend Production Build (Next.js 16 + Turbopack)
npm run build
# Result: Compiled successfully, TypeScript finished with 0 errors

# Backend Build & Test Suite (Gradle)
cd backend && ./gradlew test
# Result: BUILD SUCCESSFUL (all unit and integration tests passed)
```

### Manual Verification Workflow

#### Bug 1 Verification (Response Body Correctness)
1. Open `http://localhost/` and navigate to `POST /tcws/transconnectservice`.
2. Under **Headers**, verify `head1: test`.
3. Under **Body**, verify `JSON` with `{"head2": "hello"}`.
4. Click **Send**.
5. **Expected & Observed Result:**
   The Response panel displays:
   ```json
   {
     "reached": "head2"
   }
   ```
   No missing text or empty quotes.

#### Bug 2 Verification (Draft Persistence After Refresh)
1. In the **Headers** tab, add a Request Header: `head1: test-val`.
2. In the **Params** tab, add a Query Param: `env: staging`.
3. In the **Body** tab, select **JSON** and enter `{"head2": "persisted-data"}`.
4. Refresh the browser (`Cmd + R` / `F5`).
5. **Expected & Observed Result:**
   - The route remains active and selected.
   - Request Header `head1: test-val` is restored.
   - Query Param `env: staging` is restored.
   - Request Body Type `JSON` and Content `{"head2": "persisted-data"}` are restored.

---

## 5. Regression Risk Assessment

| Area | Risk Level | Mitigation |
| :--- | :--- | :--- |
| **Response Syntax Highlighting** | Very Low | Replaced regex with strict optional-colon grammar; tested against primitives, nested objects, strings with escaped quotes, and non-JSON payloads. |
| **LocalStorage Availability** | Very Low | All storage interactions are guarded with `typeof window !== 'undefined'` and wrapped in `try/catch` blocks (handles private browsing / storage quota errors gracefully). |
| **Cross-Route Draft Pollution** | Very Low | Draft keys are explicitly namespaced by `activeRoute.id`. Switching routes restores the target route's draft without leaking state between stubs. |
| **Backend Compatibility** | Zero | Changes are 100% frontend UI state and presentation layer. No API contracts or backend schemas were modified. |

---

## 6. Audit Result

**Result:** PASS  
All release criteria satisfied. Ready for production deployment.

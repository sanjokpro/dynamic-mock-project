# UI Migration Plan — Reference Design → Current Project

> **Source:** `~/Downloads/-llamacoder` (reference design, hardcoded mock data, no API calls)
> **Target:** `frontend/` (Next.js + React Query + real API integration)
>
> **Goal:** Adopt the visual design, layout, and UX patterns from the reference project while keeping all existing API integration, hooks, and backend wiring intact.
>
> **Strategy:** Each task is a small, self-contained change. Do them in order. After each task, run `npm run build` to verify no TypeScript errors.

---

## Architecture Differences (Understand Before Starting)

| Aspect | Reference (`-llamacoder`) | Current (`frontend/`) |
|--------|--------------------------|----------------------|
| Framework | Plain React (Vite-style) | Next.js (App Router) |
| State | `useState` in App.tsx, prop drilling | React Context (`WorkspaceContext`) + React Query |
| API | None — hardcoded mock data | Real API via Axios + TanStack Query hooks |
| Styling | Tailwind utility classes | Tailwind + shadcn/ui components |
| Code Editor | Custom `textarea` + syntax overlay | Monaco Editor (`@monaco-editor/react`) |
| Theming | Light/Dark toggle (manual class swapping) | Single theme (CSS variables) |
| Icons | `lucide-react` | `lucide-react` |

**Key principle:** Do NOT remove or break any existing API hooks (`useRoutes`, `useCollections`, `useTestClient`, `useTraffic`, etc.). The migration is visual only — wire the new UI to the existing data layer.

---

## Task 1 — Add Light/Dark Theme Support

**Priority:** HIGH — Many reference components use `theme` prop for styling
**Files to modify:**
- `frontend/src/context/WorkspaceContext.tsx` — add `theme` and `setTheme` to context
- `frontend/src/app/layout.tsx` — apply `dark` class to `<html>` when dark
- `frontend/src/app/globals.css` — add dark mode CSS variable overrides
- `frontend/src/components/workspace/Header.tsx` — add theme toggle button

**What to do:**

1. Add `theme: 'light' | 'dark'` and `toggleTheme: () => void` to `WorkspaceContextType`
2. Persist theme in `localStorage`
3. In `globals.css`, define dark mode overrides:
   ```css
   .dark {
     --background: 222.2 84% 4.9%;
     --foreground: 210 40% 98%;
     --card: 222.2 84% 4.9%;
     --primary: 263.4 70% 50.4%;
     /* ... other shadcn dark tokens */
   }
   ```
4. In `layout.tsx`, add `className={theme === 'dark' ? 'dark' : ''}` to `<html>`
5. In `Header.tsx`, add a sun/moon icon button that calls `toggleTheme()`

**Verification:** `cd frontend && npm run build` passes. Toggle works in browser.

---

## Task 2 — Replace `ResizablePanels` in `WorkspaceLayout`

**Priority:** HIGH — Reference has a cleaner, reusable ResizablePanels component
**Files to create:**
- `frontend/src/components/ui/ResizablePanels.tsx` — new reusable component

**Files to modify:**
- `frontend/src/components/workspace/WorkspaceLayout.tsx` — use new ResizablePanels

**What to do:**

1. Create `ResizablePanels.tsx` based on the reference implementation:
   ```tsx
   interface ResizablePanelsProps {
     topPanel: React.ReactNode;
     bottomPanel: React.ReactNode;
     defaultTopHeight?: number;
     minTopHeight?: number;
     minBottomHeight?: number;
   }
   ```
   - Uses `useState` for `topHeight`, `useRef` for container, `useCallback` for mouse handlers
   - Renders a drag handle with hover/drag states (violet accent when dragging)
   - Clamps height between min and max

2. In `WorkspaceLayout.tsx`, replace the manual vertical ratio logic with:
   ```tsx
   <ResizablePanels
     topPanel={<RequestPanel />}
     bottomPanel={<ResponsePanel />}
     defaultTopHeight={400}
     minTopHeight={200}
     minBottomHeight={150}
   />
   ```

**Verification:** `npm run build` passes. Panels resize smoothly in browser.

---

## Task 3 — Upgrade `Sidebar.tsx` with Reference Design

**Priority:** HIGH — The reference sidebar has better visual polish
**Files to modify:**
- `frontend/src/components/workspace/Sidebar.tsx`

**What to do:**

1. **Add expand/collapse for collections** (reference has chevron toggle per collection)
   - Add `expandedCollections: Set<string>` state, default all expanded
   - Add `ChevronDown`/`ChevronRight` icons per collection header
   - Add click handler to toggle

2. **Add method color coding** (reference has per-method colors in both light and dark themes)
   ```tsx
   const methodColors = {
     GET: { bg: 'bg-emerald-50', text: 'text-emerald-600' },
     POST: { bg: 'bg-blue-50', text: 'text-blue-600' },
     PUT: { bg: 'bg-amber-50', text: 'text-amber-600' },
     PATCH: { bg: 'bg-orange-50', text: 'text-orange-600' },
     DELETE: { bg: 'bg-red-50', text: 'text-red-600' },
   };
   ```
   Apply these to route items in the tree.

3. **Add route count badge** per collection (reference shows `collection.routes.length`)

4. **Improve search** — reference has a cleaner search input with icon

5. **Keep existing API wiring** — `useCollections`, `useRoutes`, `setActiveRoute` must remain

**Verification:** `npm run build` passes. Sidebar shows expandable collections with colored method tags.

---

## Task 4 — Upgrade `Toolbar.tsx` (Header)

**Priority:** HIGH — Reference Toolbar has environment selector, layout toggle, better structure
**Files to modify:**
- `frontend/src/components/workspace/Header.tsx`

**What to do:**

1. **Add environment selector dropdown** (reference has this in the toolbar)
   - Import `useEnvironments` hook
   - Render a `<select>` or custom dropdown with environment names
   - On change, call `setActiveEnvironment(env)`

2. **Add layout toggle button** (reference toggles between horizontal/vertical panel layout)
   - Add `layoutMode: 'horizontal' | 'vertical'` to WorkspaceContext
   - Render a layout icon button that toggles

3. **Add sidebar toggle button** (reference has eye/panel-left icon)
   - Add `isSidebarOpen: boolean` to WorkspaceContext
   - Render toggle button

4. **Improve styling** — match reference's cleaner header with proper spacing and borders

**Verification:** `npm run build` passes. Header shows environment selector and layout toggle.

---

## Task 5 — Upgrade `RequestPanel.tsx` with Reference Design

**Priority:** HIGH — Reference RequestBuilder has richer tab UI and styling
**Files to modify:**
- `frontend/src/components/workspace/RequestPanel.tsx`

**What to do:**

1. **Upgrade method selector** — reference uses a styled dropdown with method-specific colors:
   ```tsx
   <select className="px-3 py-1.5 text-xs font-bold text-emerald-600 bg-transparent border-r">
   ```
   Apply method color to the selected method text.

2. **Upgrade tab styling** — reference tabs have cleaner active states with bottom border
   ```tsx
   className={cn(
     "px-4 py-2.5 text-xs font-medium transition-colors border-b-2",
     activeTab === tab.id
       ? "border-violet-500 text-violet-600"
       : "border-transparent text-slate-500 hover:text-slate-700"
   )}
   ```

3. **Wire `KeyValueEditor` properly** — pass `activeRoute.queryParams` and `activeRoute.responseHeaders` as props, implement add/remove/edit with `onChange` callback

4. **Wire `MatcherEditor` properly** — pass `activeRoute.matchers` as props, implement add/remove matcher rows

5. **Keep existing API wiring** — `useRoutes`, `useTestClient`, `handleSave`, `handleSend` must remain

**Verification:** `npm run build` passes. Request panel shows colored method selector and functional editors.

---

## Task 6 — Upgrade `ResponsePanel.tsx` with Reference Design

**Priority:** HIGH — Reference has JSON highlighting, copy/download, line numbers
**Files to modify:**
- `frontend/src/components/workspace/ResponsePanel.tsx`

**What to do:**

1. **Add JSON syntax highlighting** — port the `JsonHighlighter` function from the reference:
   ```tsx
   function JsonHighlighter({ json, theme }: { json: string; theme: 'light' | 'dark' }) {
     // Regex-based highlighting for JSON keys, strings, numbers, booleans, null
     const highlighted = json
       .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
       .replace(/"([^"]+)":/g, `<span class="text-violet-600">"$1"</span>:`)
       .replace(/: "([^"]*)"/g, `: <span class="text-emerald-600">"$1"</span>`)
       // ... etc
     return <pre dangerouslySetInnerHTML={{ __html: highlighted }} />;
   }
   ```

2. **Add copy button** — reference has a copy-to-clipboard button
   ```tsx
   const [copied, setCopied] = useState(false);
   const handleCopy = () => {
     navigator.clipboard.writeText(responseBody);
     setCopied(true);
     setTimeout(() => setCopied(false), 2000);
   };
   ```

3. **Add download button** — reference has a download-as-file button

4. **Add line numbers** — reference shows line numbers next to the response body

5. **Add response size display** — reference shows `(body.length / 1024).toFixed(2) + ' KB'`

6. **Keep existing API wiring** — `useWorkspace().lastResponse` must remain the data source

**Verification:** `npm run build` passes. Response panel shows highlighted JSON with copy/download buttons.

---

## Task 7 — Add `BottomBar.tsx` (Console + History)

**Priority:** MEDIUM — Reference has a tabbed bottom bar with Console and History
**Files to create:**
- `frontend/src/components/workspace/BottomBar.tsx`

**Files to modify:**
- `frontend/src/components/workspace/WorkspaceLayout.tsx` — replace `LiveTrafficPanel` with `BottomBar`

**What to do:**

1. **Create `BottomBar.tsx`** based on the reference:
   - Props: `isOpen`, `onToggle`, `theme`
   - Tabs: "Console" (live traffic) and "History" (past requests)
   - Console tab: wraps existing `LiveTrafficPanel` content
   - History tab: shows a table of past requests (route, status, duration, timestamp)
   - Expand/collapse toggle

2. **Add `isBottomBarOpen: boolean`** to WorkspaceContext (default: `true`)

3. **Wire history** — collect `TestResponse` objects from `useTestClient` into a history array in context

4. **Replace in WorkspaceLayout**:
   ```tsx
   // Before:
   <LiveTrafficPanel />
   // After:
   <BottomBar isOpen={isBottomBarOpen} onToggle={toggleBottomBar} theme={theme} />
   ```

**Verification:** `npm run build` passes. Bottom bar shows Console and History tabs.

---

## Task 8 — Add `RouteDrawer.tsx` (Route Details Slide-Out)

**Priority:** MEDIUM — Reference has a slide-out drawer for route configuration
**Files to create:**
- `frontend/src/components/workspace/RouteDrawer.tsx`

**Files to modify:**
- `frontend/src/components/workspace/WorkspaceLayout.tsx` — add drawer
- `frontend/src/components/workspace/Sidebar.tsx` — add "details" button per route

**What to do:**

1. **Create `RouteDrawer.tsx`** based on the reference:
   - Props: `isOpen`, `onClose`, `route: MockRoute`
   - Shows: route name, description, path, method, creation date
   - Editable response template (CodeEditor)
   - Toggle switches: Active, Log Requests
   - Response delay slider (0–5000ms)
   - Save and Duplicate buttons
   - Overlay backdrop when open

2. **Add `isDrawerOpen: boolean` and `drawerRoute: MockRoute | null`** to WorkspaceContext

3. **In Sidebar**, add a small info/settings icon per route that opens the drawer

4. **In WorkspaceLayout**, render `<RouteDrawer />` as an overlay

**Verification:** `npm run build` passes. Clicking route settings icon opens the drawer.

---

## Task 9 — Upgrade `CodeEditor.tsx` Syntax Highlighting

**Priority:** LOW — Reference has custom syntax highlighting; current uses Monaco
**Decision:** Keep Monaco Editor (it's more powerful), but optionally add the reference's lightweight highlighting as a fallback

**Files to modify:**
- `frontend/src/components/workspace/CodeEditor.tsx`

**What to do (optional):**

1. Add a `theme` prop that passes light/dark to Monaco:
   ```tsx
   <Editor theme={theme === 'dark' ? 'vs-dark' : 'vs'} ... />
   ```

2. If Monaco is too heavy, replace with the reference's `textarea` + overlay approach (but this is optional — Monaco is strictly better)

**Verification:** `npm run build` passes. Editor respects theme toggle.

---

## Task 10 — Wire `ScenarioEditor.tsx` with Real Data

**Priority:** LOW — Currently shows static cards, needs interactive state machine
**Files to modify:**
- `frontend/src/components/workspace/scenarios/ScenarioEditor.tsx`

**What to do:**

1. Display each scenario's states as a list with transition arrows
2. Show current state (from Redis) via a new API call or the existing scenario data
3. Add "Reset" button per scenario (calls `POST /api/scenarios/{name}/reset`)
4. Add "Trigger Transition" dropdown per scenario (calls `POST /api/scenarios/{name}/transition`)

**Verification:** `npm run build` passes. Scenario editor shows real state data with actions.

---

## Task Summary

| # | Task | Priority | Est. Effort | Files Changed |
|---|------|----------|-------------|---------------|
| 1 | Light/Dark theme support | HIGH | 30 min | WorkspaceContext, layout, globals.css, Header |
| 2 | Reusable ResizablePanels | HIGH | 20 min | New file + WorkspaceLayout |
| 3 | Sidebar upgrade (expand/collapse, colors) | HIGH | 30 min | Sidebar.tsx |
| 4 | Toolbar upgrade (env selector, layout toggle) | HIGH | 30 min | Header.tsx + WorkspaceContext |
| 5 | RequestPanel upgrade (editors, tabs) | HIGH | 45 min | RequestPanel.tsx |
| 6 | ResponsePanel upgrade (highlighting, copy) | HIGH | 30 min | ResponsePanel.tsx |
| 7 | BottomBar (Console + History) | MEDIUM | 45 min | New file + WorkspaceLayout + Context |
| 8 | RouteDrawer (slide-out details) | MEDIUM | 45 min | New file + WorkspaceLayout + Sidebar |
| 9 | CodeEditor theme support | LOW | 10 min | CodeEditor.tsx |
| 10 | ScenarioEditor real data | LOW | 30 min | ScenarioEditor.tsx |

**Total estimated effort:** ~4.5–5 hours

---

## Migration Rules

1. **Never remove API hooks** — `useRoutes`, `useCollections`, `useTestClient`, `useTraffic`, `useEnvironments`, `useScenarios`, `useProtocolEndpoints` must all remain wired
2. **Never remove backend calls** — Every `axios.get/post/put/delete` must remain functional
3. **Keep the `WorkspaceContext` pattern** — Add new state to it, don't create competing state managers
4. **Keep React Query** — Don't replace with manual `fetch` or `useState` for data
5. **Test after each task** — Run `npm run build` after every file change
6. **Preserve existing components** — Don't delete `LiveTrafficPanel.tsx`, `ProtocolEditor.tsx`, etc. — they may be wrapped or reused

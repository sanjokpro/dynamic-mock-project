# Minor Release Discovery: v0.6.1 — HTML Response Preview & Interactive Stub Experience

- **Document Status:** Discovery & Product Validation (No Code / No Implementation Plan)
- **Current Release:** `v0.6.0` (White Label Foundation & Enterprise Customization)
- **Target Release:** `v0.6.1` (Minor UX & Simulation Experience Enhancement)
- **Candidate Title:** HTML Response Preview & Interactive Stub Experience
- **Governance Alignment:** `AI_GOVERNANCE.md`, `ROADMAP_AUTHORITY.md`, `MASTER_PRODUCT_ACTION_PLAN.md`

---

## 1. Executive Summary

During end-to-end validation of `v0.6.0` (`e2e/tests/mobile-banking-login-stub.e2e.spec.ts`), Dynamic Mock proved capable of powering a complete, stateful **Mobile Banking Login** interactive workflow combining:
- `text/html` response templates with embedded CSS and JavaScript
- Polyglot GraalVM Post-Script request inspection (`queryParams`, JSON body, form-urlencoded body)
- Dynamic template token replacement (`__RETURN_URL__` and Handlebars variables)
- Asynchronous `fetch()` callbacks from the rendered page back into `/mock/**`
- Conditional webhook triggering (`webhook = { url, method }`) and `302` redirect chains

While Dynamic Mock's backend simulation engine executes these workflows flawlessly, **the workspace UI treats HTML responses as plain text/source code**. Both the **Response Panel** (`ResponsePanel.tsx`) and the **Route Details Drawer** (`RouteDrawer.tsx`) render raw HTML strings inside `<pre>` / `JsonHighlighter` blocks.

**Discovery Verdict:** **STRONG GO for `v0.6.1`**. Adding a secure, sandboxed **HTML Preview** mode in the Response Panel and Route Details Drawer is a low-risk, high-impact minor release (`v0.6.1`) that closes the loop on interactive web/redirect simulations without altering backend domain logic or delaying Sprint 7.

---

## 2. User Problem Analysis

### 2.1 The Current Friction Loop
When engineers build or test HTML-returning stubs in Dynamic Mock today, their workflow is broken across multiple contexts:

```text
Current Workflow (Broken Context):
Configure HTML Stub + Post-Script in UI
  → Click "Save"
  → Click "Execute" (sees raw HTML source code in Response Panel)
  → Open a separate browser tab
  → Manually construct URL + query parameters (?returnUrl=...)
  → Inspect UI & click buttons
  → Switch back to Dynamic Mock to check Traffic / Logs
```

```text
Target v0.6.1 Workflow (Unified Workspace):
Configure HTML Stub + Post-Script in UI
  → Click "Execute"
  → Response Panel automatically surfaces rendered HTML in "Preview" tab
  → Interact with forms/buttons directly inside the Response Panel (or preview template in Route Details)
  → Verify UI, fetch() callbacks, and webhook/redirect side effects immediately
```

### 2.2 High-Value Enterprise Use Cases Unlocked
Modern API integrations frequently cross the boundary between pure JSON APIs and browser-mediated user journeys ("human-in-the-loop" API flows). Supporting visual HTML preview directly targets:

| Use Case | Protocol / Flow Pattern | Why In-App Preview Matters |
| :--- | :--- | :--- |
| **Mobile Banking Login** | WebView / Redirect Auth (`?returnUrl=...`) | Verifies login form layout, MPIN inputs, JS `fetch()` submission, and return URL webhook triggers in one view. |
| **OAuth2 / OIDC Login & Consent** | `/authorize` HTML login & scope consent screen | Lets identity & fintech teams preview branded login/consent screens and test "Approve" / "Deny" button redirects. |
| **3-D Secure (3DS) Challenge Screens** | Payment Gateway ACS challenge HTML + OTP form | Essential for payment processors simulating cardholder OTP verification screens before payment capture. |
| **SSO / SAML IdP Portals** | HTML form auto-submit (`POST` binding) | Allows immediate inspection of SAML POST binding forms and intermediate redirect pages. |
| **Payment Gateway Redirect Pages** | Hosted checkout / redirect status pages | Enables QA engineers to visually verify success/failure checkout landing pages. |
| **Partner Onboarding & Banking Consent** | Open Banking PSD2 / FDX account selection UI | Shows multi-step consent HTML templates rendered with dynamic Handlebars data. |

---

## 3. Required 10-Point Evaluation

### 3.1 Product Value (Score: 9.5 / 10)
- Elevates Dynamic Mock from a "JSON/Protocol Mock Server" to a **Full-Journey Integration Simulator** capable of demonstrating both backend APIs (REST, GraphQL, gRPC, ISO8583) and the interactive browser/WebView handoffs that sit in front of them.
- Dramatically improves live product demonstrations: showing a working Mobile Banking Login or 3DS Challenge card right inside the Response Panel is immediately memorable for technical evaluators.

### 3.2 UX Value (Score: 9.5 / 10)
- Eliminates context-switching between the Dynamic Mock workspace and external browser tabs.
- Works seamlessly with existing Test Client parameters (`Query Parameters`, `Request Headers`, `Request Body`), allowing users to test how different query parameters (e.g., missing `returnUrl` vs. valid `returnUrl`) alter the rendered HTML UI in real time.

### 3.3 Technical Complexity (Score: Low-Medium — Ideal for `v0.6.1`)
- **Zero Backend Changes Required:** `POST /api/test-client/execute` already returns `status`, `statusText`, `headers` (including `Content-Type`), and `body` (after Handlebars + GraalVM Post-Script execution).
- **Frontend-Focused Scope:** Changes are localized to `ResponsePanel.tsx`, `RouteDrawer.tsx`, and a reusable `<HtmlPreviewFrame />` component.

### 3.4 Security Risks (Manageable with Strict Sandboxing)
- Rendering user-authored HTML/JS inside an admin workspace introduces **Self-XSS / Sandbox Escape** risks if implemented naively (e.g., `dangerouslySetInnerHTML` or an unsandboxed same-origin `<iframe>`).
- **Mitigation:** Never use `dangerouslySetInnerHTML`. Always render inside a sandboxed `<iframe sandbox="allow-scripts allow-forms">` **without** `allow-same-origin` (giving the iframe an isolated, opaque `null` origin that cannot access `window.parent`, `localStorage`, or parent DOM). See Section 6 for full security analysis.

### 3.5 Performance Impact (Negligible)
- The `<iframe>` is only mounted when the user views the `Preview` tab on a route/response with HTML content.
- Unmounting the tab immediately destroys the iframe browsing context and frees all memory/timers inside it.
- No impact on JSON, XML, GraphQL, gRPC, or ISO8583 workflows.

### 3.6 Browser Sandbox Strategy
- Use an `<iframe srcdoc={preparedHtml} sandbox="allow-scripts allow-forms allow-popups">` (explicitly omitting `allow-same-origin` and `allow-top-navigation`).
- Inject a lightweight runtime context header into `srcdoc` so that relative URLs, `window.location.pathname` references (as used in `mobile-banking-login-stub.e2e.spec.ts`), and `fetch()` calls target the actual mock endpoint (`/mock/...`) cleanly.

### 3.7 Route Details Preview Design (`RouteDrawer.tsx`)
- Currently, `RouteDrawer.tsx` (lines 234–244) shows a truncated 300-character `<pre>` block labeled `Response Preview`.
- **Proposed Design:**
  - When `activeRoute` has `Content-Type: text/html` in `responseHeaders` OR the `responseTemplate` starts with `<!DOCTYPE html>` / `<html`, display a compact **Preview | Source** segmented toggle in the Route Details drawer.
  - **Preview mode:** Renders the `responseTemplate` inside a compact, scaled or scrollable sandboxed preview card (with an "Expand" / "Open in Test Client" action) so users browsing routes in the drawer can visually identify HTML stubs at a glance.
  - **Source mode:** Displays the raw template source code (removing the arbitrary 300-character hard truncation in favor of a scrollable code block with `max-h-48`).
  - Note that because Route Details inspects the *static definition* prior to test execution, any Post-Script dynamic replacements or Handlebars expressions will either render with placeholder/sample tokens or display a subtle badge: *"Static template preview — click Execute in Test Client to run Post-Scripts"*.

### 3.8 Response Panel Preview Design (`ResponsePanel.tsx`)
- Currently, `ResponsePanel.tsx` has two tabs: `Body` and `Headers (N)`.
- **Proposed Design:**
  - Detect HTML responses when `Content-Type` header contains `text/html` (case-insensitive) or when `Body` is an HTML document (`<!DOCTYPE html>` / `<html...`).
  - When HTML is detected, surface three tabs:
    - **`Preview`** *(auto-selected by default when `Content-Type` includes `text/html`, or selectable alongside `Body`)*
    - **`Body`** *(retains raw source inspection + search bar)*
    - **`Headers (N)`**
  - Provide a compact **Browser Preview Toolbar** above the iframe showing:
    - Simulated URL bar (`GET /mock/mobile-banking-login?returnUrl=...`)
    - **Reload Preview** button (re-mounts the iframe to reset DOM/JS state)
    - **Open in New Tab** button (opens the resolved `/mock/...` URL in a real browser tab for full redirect testing)

### 3.9 Compatibility With Existing JSON Workflows (100% Backward Compatible)
- For `application/json`, `application/xml`, `text/plain`, and all non-HTML responses:
  - `Body` remains the default active tab with `JsonHighlighter`, search match counter, Copy, Download (with smart file extension `.html` vs `.json`), and Copy as cURL.
  - Existing E2E tests and REST workflows experience zero disruption.
  - Bonus fix: `handleDownload` in `ResponsePanel.tsx` currently hardcodes `response.json` and `application/json`; for HTML responses, it can download as `response.html` (`text/html`).

### 3.10 Competitive Differentiation
| Capability | Postman | WireMock Cloud / Standalone | Mockoon | **Dynamic Mock (with v0.6.1)** |
| :--- | :--- | :--- | :--- | :--- |
| Render HTML Response Preview | Basic static preview (blocks most JS/fetch workflows) | External browser required | Basic static preview | **Interactive Sandboxed Preview (CSS + JS + Forms + `fetch()` + Post-Script)** |
| Stateful Polyglot Post-Script + HTML + Webhook in one Stub | ❌ No | ⚠️ Complex Java extensions | ❌ No | **✅ Native (GraalVM JS + Webhooks + HTML Preview)** |
| Route Details Visual Thumbnail / Preview | ❌ No | ❌ No | ❌ No | **✅ Preview / Source toggle in Route Drawer** |

---

## 4. UX Proposal

### 4.1 Response Panel (`ResponsePanel.tsx`)

```text
+-----------------------------------------------------------------------------------+
| [● 200 OK]   ⏱ 14ms   🗄 3.2 KB                        [cURL] [Copy] [Download]   |
+-----------------------------------------------------------------------------------+
| [ Preview ✨ ]  [ Body ]  [ Headers (2) ]        🌐 /mock/mobile-banking-login [↻] [↗] |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|   +---------------------------------------------------------------------------+   |
|   |  [Mobile Banking]                                                         |   |
|   |  Secure Sign In                                                           |   |
|   |  Authenticate to continue to your mobile banking session                  |   |
|   |                                                                           |   |
|   |  Mobile Number / User ID                                                  |   |
|   |  [ 9800000000                                                          ]  |   |
|   |                                                                           |   |
|   |  MPIN / Password                                                          |   |
|   |  [ ••••                                                                ]  |   |
|   |                                                                           |   |
|   |  [                              Login                                  ]  |   |
|   |                                                                           |   |
|   |                   Login completed - return URL triggered                  |   |
|   +---------------------------------------------------------------------------+   |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

**Key UX Behaviors in Response Panel:**
1. **Smart Content-Type Detection:**
   - Checks `lastResponse.headers` (case-insensitively) for `content-type` containing `text/html` or `application/xhtml+xml`.
   - Also detects if a Post-Script dynamically mutated `response.body` into plain text (e.g., `'no return url were provided'` when `returnUrl` is omitted, even though `Content-Type` is `text/html; charset=utf-8`): both HTML cards and plain-text error messages render accurately in the browser preview just as a real browser displays them!
2. **Auto-Tab Selection with User Override:**
   - When a new test execution returns `Content-Type: text/html`, `ResponsePanel` automatically selects the `Preview` tab so the user sees the rendered page immediately after clicking **Execute**.
   - Users can click `Body` at any time to inspect the raw HTML source code or search within it.
   - When a non-HTML response arrives (`application/json`), `ResponsePanel` automatically selects `Body`.
3. **Interactive Mini-Browser Bar:**
   - Displays the target mock path + query string (e.g., `/mock/mobile-banking-login?returnUrl=...`).
   - **Reset / Reload (`↻`)**: Re-initializes the iframe `srcdoc` from `lastResponse.body` so the user can re-test form interactions without re-running the network call if desired.
   - **Open in Browser (`↗`)**: Opens the live `/mock/...` URL in a new browser tab (`target="_blank" rel="noopener noreferrer"`).

### 4.2 Route Details Drawer (`RouteDrawer.tsx`)

```text
+--------------------------------------------------+
| RESPONSE PREVIEW              [Preview] [Source] |
+--------------------------------------------------+
| +----------------------------------------------+ |
| |  [Mobile Banking]                            | |
| |  Secure Sign In                              | |
| |  [ 9800000000 ]                              | |
| |  [ ••••       ]                              | |
| |  [   Login    ]                              | |
| +----------------------------------------------+ |
| ℹ️ Template preview. Run in Test Client to       |
|    execute Post-Scripts & dynamic parameters.    |
+--------------------------------------------------+
```

**Key UX Behaviors in Route Details Drawer:**
1. **Preview | Source Toggle:** Replaces the static 300-character truncated `<pre>` snippet when the route is an HTML stub (detected via `responseHeaders['Content-Type']` containing `text/html` or template starting with `<`).
2. **Expectation Clarity:** Because a stub's final HTML may depend on runtime query parameters (like `?returnUrl=...`) or GraalVM Post-Script mutations, a concise helper caption clarifies that `Route Details` previews the raw `Response Template`, whereas `Test Client -> Execute` previews the live script-processed response.

---

## 5. Technical Options Analysis

We evaluated three technical approaches for rendering HTML inside Dynamic Mock:

### Option 1: Direct DOM Injection (`dangerouslySetInnerHTML` or Shadow DOM)
- **How it works:** Injects `lastResponse.body` into a `<div>` or Shadow Root inside the React tree.
- **Pros:** Fast, no iframe border.
- **Cons:**
  - **Critical Security Flaw:** Any `<script>` tag or inline event handler (`onerror`, `onload`) executes in the **exact same origin and `window` context** as the Dynamic Mock Admin UI, exposing the in-memory JWT token (`AuthContext`), `localStorage`, and internal `/api/**` endpoints.
  - `<script>` tags injected via `innerHTML` do not execute standardly without manual DOM script reconstruction, and global CSS (`* { margin: 0 }`, `body { ... }`) leaks or conflicts.
- **Verdict:** **REJECTED** (Unacceptable security and CSS isolation risks).

### Option 2: Live URL Iframe (`<iframe src="/mock/..." />`)
- **How it works:** Points the iframe `src` directly to the live `/mock/path?query` endpoint.
- **Pros:** Native browser URL resolution for `window.location.pathname` and `302` redirects.
- **Cons:**
  - **Only works for `GET` requests:** Cannot preview `POST`, `PUT`, `DELETE` HTML responses or requests that require custom outgoing `Request Headers` (e.g., `Authorization`, `X-Dynamic-Mock-Env`, custom matcher headers) configured in the Test Client.
  - **Double Execution Problem:** Clicking "Execute" in the Test Client already calls `POST /api/test-client/execute` (which increments stateful scenario counters, triggers webhooks, and logs traffic). Pointing `<iframe src="/mock/...">` would fire a **second** unintended HTTP request against the mock route!
- **Verdict:** **REJECTED as primary renderer** (Useful only as an optional "Open in New Tab" external action).

### Option 3: Sandboxed `srcdoc` Iframe with Smart Context Shim (Recommended)
- **How it works:**
  1. Renders the exact `lastResponse.body` already returned by `POST /api/test-client/execute` inside `<iframe srcdoc={ augmentedHtml } sandbox="allow-scripts allow-forms allow-popups" />`.
  2. Does **not** include `allow-same-origin`, enforcing a strict browser-level **opaque `null` origin** boundary.
  3. Pre-processes `srcdoc` by injecting a tiny, transparent `<head>` bootstrap before the stub's scripts run:
     - Sets `<base href="{origin}/mock/" />` so relative links, images, and stylesheets resolve properly.
     - Provides a lightweight compatibility shim for `fetch()` and `XMLHttpRequest` inside `srcdoc` so that common patterns used in interactive stubs—specifically `window.location.pathname + '?action=login...'` (which in `about:srcdoc` would evaluate to `"srcdoc?action=login..."`) or relative paths—automatically resolve against the executed mock route URL (e.g., `http://localhost:8080/mock/mobile-banking-login?action=login...`).
     - Handles `<form>` submissions with relative actions similarly.
- **Pros:**
  - **Zero Double-Execution:** Renders the exact response from the single Test Client execution.
  - **Works for All HTTP Methods & Custom Headers:** Previews HTML returned by `POST /login`, header-matched routes, stateful scenarios, Handlebars templates, and GraalVM Post-Scripts.
  - **Works Out-of-the-Box with `mobile-banking-login-stub.e2e.spec.ts`:** When the user clicks the `#login-btn` inside the preview iframe, `window.location.pathname + '?action=login&returnUrl=...'` is transparently routed to `/mock/mobile-banking-login?action=login&returnUrl=...`, firing the backend webhook and updating `#login-status` to `"Login completed - return URL triggered"` right inside the Response Panel!
- **Verdict:** **RECOMMENDED**.

---

## 6. Recommended Architecture & Security Review

### 6.1 Sandbox Isolation Matrix

```text
+-----------------------------------------------------------------------+
| Parent Window: Dynamic Mock Workspace UI (Origin: http://localhost)   |
| - Holds in-memory JWT (AuthContext)                                   |
| - Holds draft state in localStorage                                   |
|                                                                       |
|   +---------------------------------------------------------------+   |
|   | <iframe sandbox="allow-scripts allow-forms allow-popups"      |   |
|   |         referrerpolicy="no-referrer"                          |   |
|   |         srcdoc="..." />                                       |   |
|   |                                                               |   |
|   | - Effective Origin: null (Opaque Origin)                      |   |
|   | - window.parent.document: BLOCKED (SecurityError)             |   |
|   | - localStorage / sessionStorage: BLOCKED (SecurityError)      |   |
|   | - document.cookie: BLOCKED (SecurityError)                    |   |
|   | - Top-level navigation: BLOCKED (no allow-top-navigation)     |   |
|   | - CSS & Layout: 100% isolated inside iframe viewport          |   |
|   | - fetch() to /mock/**: ALLOWED via CORS                       |   |
|   +---------------------------------------------------------------+   |
+-----------------------------------------------------------------------+
```

### 6.2 Security Controls Checklist
1. **No `allow-same-origin` Attribute:**
   - The combination of `allow-scripts` + `allow-same-origin` is strictly forbidden because it would allow iframe scripts to access `window.parent` and strip its own `sandbox` attribute.
   - By omitting `allow-same-origin`, the browser assigns the iframe a unique opaque origin (`null`).
2. **No `allow-top-navigation` Attribute:**
   - Prevents a stub's JavaScript (`window.top.location = ...`) from hijacking or redirecting the Dynamic Mock workspace window.
3. **CORS Support on `/mock/**` for Opaque Origins:**
   - Because an iframe without `allow-same-origin` sends `Origin: null` on `fetch()` requests to `/mock/**`, verify that `/mock/**` responses include `Access-Control-Allow-Origin: *` (or echo permissive CORS headers on mock endpoints) so interactive `fetch()` calls inside the preview succeed without CORS blocks.
   - Crucially, `/api/**` (admin endpoints) remain protected by JWT authentication and strict `app.cors.allowed-origins`.
4. **No Automatic Execution in Passive Lists:**
   - In `RouteDrawer.tsx` (Route Details), the static template preview can either use `sandbox=""` (pure HTML+CSS, zero JS execution) or `sandbox="allow-scripts allow-forms"` only when explicitly toggled, ensuring passive inspection never triggers unintended network calls.

---

## 7. Risks & Mitigations

| # | Risk | Likelihood | Impact | Mitigation |
| :- | :--- | :---: | :---: | :--- |
| 1 | **`window.location.pathname` evaluates to `"srcdoc"` inside `<iframe srcdoc>`** (breaking scripts like the Mobile Banking Login stub that build URLs from `window.location.pathname`) | High | High | Inject a tiny script shim at the top of `srcdoc` that intercepts `fetch` / `XMLHttpRequest` and rewrites leading `srcdoc` or `about:srcdoc` pathnames to the actual executed mock path (e.g., `/mock/mobile-banking-login`). |
| 2 | **CORS rejection when `srcdoc` (Origin: `null`) calls `fetch('/mock/...')`** | Medium | High | Ensure `DynamicRouteDispatcher` / mock filter includes permissive CORS headers (`Access-Control-Allow-Origin: *`) for `/mock/**` simulation traffic so `Origin: null` preview frames can call mock routes freely. |
| 3 | **Iframe white background flash or dark-mode clash** | Low | Low | Give the preview container a clean neutral browser frame with a subtle header bar so both light-themed and dark-themed HTML pages look intentional. |
| 4 | **Redirect flows (`302 Found`) inside `fetch()` or form submit** | Medium | Medium | Standard `fetch()` follows `302` redirects automatically; for full-page navigation redirects, provide the 1-click **"Open in New Tab"** button in the preview toolbar. |

---

## 8. Acceptance Criteria for `v0.6.1`

1. **Content-Type Detection & Response Panel Tabs:**
   - When an executed route returns `Content-Type: text/html` (including parameters like `text/html; charset=utf-8`), the **Response Panel** displays `Preview`, `Body`, and `Headers` tabs and defaults to `Preview`.
   - For non-HTML responses (`application/json`, etc.), existing behavior (`Body` and `Headers`) is unchanged.
2. **Interactive Browser Behavior in Preview:**
   - CSS styles (`<style>` and inline styles) render accurately inside the preview viewport.
   - JavaScript (`<script>`), HTML `<form>` elements, `<input>` fields, and `<button>` clicks execute properly.
   - `fetch()` calls triggered by buttons inside the preview (such as the Mobile Banking Login form submitting `?action=login&returnUrl=...`) resolve against the mock endpoint, execute Post-Scripts/webhooks, and update the DOM inside the preview.
   - Dynamic Handlebars expressions and GraalVM Post-Script modifications (such as replacing `__RETURN_URL__` or returning `'no return url were provided'`) are accurately reflected in the preview.
3. **Route Details Drawer Preview:**
   - Opening **Route Details** (`RouteDrawer`) on an HTML route provides a `Preview` / `Source` toggle instead of only a truncated 300-character raw string.
4. **Security & Isolation:**
   - The preview `<iframe>` uses strict sandboxing (`sandbox="allow-scripts allow-forms allow-popups"`, **never** `allow-same-origin`) so previewed scripts cannot access `window.parent`, `localStorage`, or admin `/api/**` resources.
5. **E2E Verification:**
   - The Mobile Banking Login workflow (`mobile-banking-login-stub.e2e.spec.ts`) can be visually previewed and interacted with directly inside the Response Panel `Preview` tab.

---

## 9. Release Scope (`v0.6.1`)

### In Scope for `v0.6.1`
- `<HtmlPreviewFrame />` reusable sandboxed iframe component with `srcdoc` context shim (`<base>` + `window.location.pathname` / relative `fetch` resolution) and mini-toolbar (`Reload Preview`, `Open in New Tab`).
- `ResponsePanel.tsx` integration: `Preview | Body | Headers` tabs for HTML responses, auto-switching to `Preview` on `text/html`, and `.html` file extension support on Download.
- `RouteDrawer.tsx` integration: `Preview | Source` toggle in Route Details for HTML stubs.
- Permissive CORS verification for `/mock/**` endpoints when called from `Origin: null` sandboxed preview iframes.
- Unit and Playwright E2E test coverage verifying in-app HTML preview and interactive login button execution.

### Explicitly Out of Scope for `v0.6.1`
- Full multi-page browser history back/forward navigation stack inside the iframe (users can use "Open in New Tab" for complex multi-domain redirect chains).
- WYSIWYG visual drag-and-drop HTML page builder.
- Changes to backend domain entities (`MockRoute`) or database schemas.

---

## 10. Recommendation

**Approve `v0.6.1: HTML Response Preview & Interactive Stub Experience` for immediate implementation prior to Sprint 7.**

### Why This Belongs in `v0.6.1`
1. **High Leverage, Small Footprint:** It requires zero database migrations or domain model changes, making it an ideal point release (`v0.6.1`).
2. **Completes the Interactive Simulation Story:** We already built the hardest parts in `v0.1.0–v0.6.0` (Handlebars templating, GraalVM Polyglot Post-Scripts, asynchronous webhook dispatching, and `text/html` serving). Adding the visual preview unlocks 100% of that backend power directly in the workspace UI.
3. **Immediate Validation Value:** Developers and QA engineers building OAuth, 3DS, SSO, and Mobile Banking Login stubs can go from `Save Stub → Execute → Interact in Preview` in under 2 seconds without leaving Dynamic Mock.


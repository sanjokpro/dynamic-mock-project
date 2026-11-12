# Feature Traceability Report: Interactive HTML Simulation & Mock Browser Journeys

**Generated Date**: 2026-10-30
**Investigated Target**: Pre-v0.6.x Product Documentation and Roadmap Authority
**Evidence Method**: Full-repository `git grep` at tags `v0.5.1` (2026-10-26) and `v0.6.0` (2026-10-28, commit `ebd6ac0`), plus HEAD-state excerpt verification. No inference; file/commit evidence only.
**Note on corpus layout**: No `sprint/`, `architecture/`, or `roadmap/**` directories exist in this repository. The colloquially referenced "sprint/**" corpus is `SPRINT_0[1-6]_*.md` files at the repo root; "architecture/**" maps to `WHITE_LABEL_ARCHITECTURE.md` and similar root docs; "roadmap/**" maps to `FRONTEND_COMPLETION_ROADMAP.md` and `ISO8583_DESIGNER_ROADMAP.md`. All were searched. `docs/**` contains only `docs/governance/GOVERNANCE_GAP_ANALYSIS.md` and it was searched.

---

## 1. Executive Summary: Was This Feature Documented Before v0.6.x?

### **NO.**

None of the following capabilities were documented or planned as mock-server/simulation features prior to `v0.6.x`:

- Custom HTML login pages
- Mobile banking login simulation
- OAuth / SSO login screens (as mock simulation endpoints)
- Consent screens
- Return URL callbacks
- Redirect-based flows (multi-hop 302 redirect simulation)
- Browser-rendered mock pages
- Interactive user journeys (browser-mediated / human-in-the-loop)

**Decisive tag-level evidence** — occurrence count of each term across **all `.md` files** in the repository at the `v0.5.1` and `v0.6.0` tags:

| Search term | Files at `v0.5.1` | Files at `v0.6.0` |
|---|:---:|:---:|
| `consent` / `Consent` | **0** | **0** |
| `returnUrl` / `return_url` / `RETURN_URL` | **0** | **0** |
| `human-in-the-loop` | **0** | **0** |
| `HTML preview` / `html preview` / `HtmlPreview` | **0** | **0** |
| `browser-rendered` | **0** | **0** |
| `Mobile Banking` / `mobile banking` | **0** | **0** |
| `3DS` / `3-D Secure` | **0** | **0** |
| `OAuth` | 4 | 4 |
| `SSO` | 4 | 4 |
| `login` / `Login` | 14 / 12 | 14 / 12 |
| `redirect` / `Redirect` | 10 / 3 | 10 / 3 |

The non-zero terms (OAuth, SSO, login, redirect) were inspected in full context at the `v0.6.0` tag: **every occurrence** refers to (a) the platform's own authentication wall (Sprint 4 / v0.4.0 admin `/login` page, JWT session, route guards), (b) white-label branding of that same platform login screen (Sprint 6 / v0.6.0), or (c)ally deferred inbound enterprise SSO adapters. None describes *simulating* login, consent, callback, or redirect experiences for downstream consumers.

The capability to simulate and preview browser-rendered HTML workflows, mobile banking login journeys, return URL callbacks, and redirect chains **first emerged during post-v0.6.0 end-to-end validation** and was formalized in `HTML_PREVIEW_DISCOVERY.md`, which entered the repository history in the very commit the `v0.6.1` tag marks (`bca3faa` / tag `9bf1a0e`, 2026-10-30).

---

## 2. Evidence Timeline (from `git log` and tag objects)

| Date | Commit / Tag | Event |
|---|---|---|
| 2026-10-26 | tag `v0.5.0` / `v0.5.1` | Sprint 5 UX shipped. Zero occurrences of all nine feature concepts in all `.md`. |
| 2026-10-27 | `c5a54bf`…`256aff0` | Sprint 6 White Label branding implemented (branded **platform** `/login` screen). |
| 2026-10-28 | tag `v0.6.0` (`ebd6ac0`) | White Label Foundation released. Still zero occurrences of consent / returnUrl / HITL / HTML preview / mobile banking / 3DS in all `.md`. |
| 2026-10-28 | `4360085` | Sprint 6 release audit finalized. |
| 2026-10-29 | `31ed694` → `dce5eec` | HTML preview frame, response-panel integration, RouteDrawer source/preview toggle, redirect-follower telemetry — the v0.6.1 feature work begins. |
| 2026-10-30 | `bca3faa` = tag `v0.6.1` (`9bf1a0e`) | Code implementation completed; **`HTML_PREVIEW_DISCOVERY.md` is introduced in this same commit**, together with release notes and roadmap status. |

---

## 3. Feature-by-Feature Traceability & Classification

### 1. Custom HTML Login Pages
- **Documented Before v0.6.x?**: **NO**
- **Pre-v0.6.x file path(s)**: `MASTER_PRODUCT_ACTION_PLAN.md` (line 140 at both `v0.6.0` and HEAD), `SPRINT_04_DISCOVERY.md`, `SPRINT_04_IMPLEMENTATION_PLAN.md`
- **Exact excerpts**:
  - *Pre-v0.6.x (`MASTER_PRODUCT_ACTION_PLAN.md:140`, verified at tag `v0.6.0`)*:
    > `| 13 | Auth Wall (Login Page) | FRONTEND_COMPLETION_ROADMAP | **Critical** | High | Medium | Medium | **ACCEPT** |`
  - *Pre-v0.6.x (`MASTER_PRODUCT_ACTION_PLAN.md:272–273` at tag `v0.6.0`; rendered as ~285–287 at HEAD after the v0.6.1 table row was inserted)*:
    > `### Sprint 2: Authentication Wall`
    > `**Deliverables**: Login page, JWT-based session, API key enforcement, replace \`default-user\` with authenticated identity.`
  - *Emergence (`HTML_PREVIEW_DISCOVERY.md:13–14`, introduced in the `v0.6.1` tagged commit)*:
    > `During end-to-end validation of v0.6.0 (e2e/tests/mobile-banking-login-stub.e2e.spec.ts), Dynamic Mock proved capable of powering a complete, stateful Mobile Banking Login interactive workflow combining: text/html response templates with embedded CSS and JavaScript...`
- **Classification**: Pre-v0.6.x = **Related Vision** (platform identity: the planned "Login Page" was strictly the platform's own admin authentication wall, `frontend/src/app/login/page.tsx` / `POST /api/auth/login` — shipped as v0.4.0). Mock HTML login simulation: **none** prior to v0.6.x.

### 2. Mobile Banking Login Simulation
- **Documented Before v0.6.x?**: **NO** — terms `mobile banking` / `Mobile Banking` present in **0** `.md` files at `v0.5.1` and `v0.6.0`.
- **Pre-v0.6.x file path(s)** (nearest adjacent vision): `README.md` (line 49), `SPRINT_01_DISCOVERY.md`–`SPRINT_03_*.md` (ISO8583), `BANKING_SANDBOX_TUTORIAL.md`
- **Exact excerpts**:
  - *Pre-v0.6.x (`README.md:49`)*:
    > `- **Interactive Banking Demo Sandbox**: Dedicated Header and Sidebar launchers with visual ISO8583 transaction lifecycle flows (optional evaluation showcase)`
  - *Emergence (`HTML_PREVIEW_DISCOVERY.md:56`)*:
    > `| **Mobile Banking Login** | WebView / Redirect Auth (?returnUrl=...) | Verifies login form layout, MPIN inputs, JS fetch() submission, and return URL webhook triggers in one view. |`
- **Classification**: Pre-v0.6.x = **Related Vision** (ISO8583 financial backend message stubs: MTI 0100/0200/0400, a "banking sandbox" of transaction lifecycles — not a browser login simulation). Mobile Banking web/login mock: **none** prior to v0.6.x.

### 3. OAuth / SSO Login Screens
- **Documented Before v0.6.x?**: **NO** (as *simulated* OAuth/SSO login screens for downstream apps)
- **Pre-v0.6.x file path(s)**: `MASTER_PRODUCT_ACTION_PLAN.md:369`, `SPRINT_04_DISCOVERY.md:143–150`, `SPRINT_04_ARCHITECTURE_REVIEW.md`, `AGENT_SPECIALIZATION_PLAN.md`
- **Exact excerpts** (all verified at tag `v0.6.0`):
  - *Pre-v0.6.x (`MASTER_PRODUCT_ACTION_PLAN.md:369`)*:
    > `| **LDAP / OIDC / SAML** | Enterprise checkbox item | These are integration features. They should only be built when a specific enterprise pilot demands it. Abstract the auth interface now, implement adapters later. |`
  - *Pre-v0.6.x (`SPRINT_04_DISCOVERY.md:143,147,150`)*:
    > `- **Scope**: Corporate Single Sign-On (SSO) integration via Active Directory, Keycloak, Okta, or Azure AD.`
    > `- **Complexity**: Very High (redirect callbacks, metadata parsing, SSL truststores, external IdP mock testing).`
    > `- **Recommended Priority**: **DEFER to Sprint 7+**.`
- **Classification**: Pre-v0.6.x = **Related Vision** — inbound enterprise SSO for authenticating *operators into* Dynamic Mock (deferred to Sprint 7+), never an outbound/simulated OAuth `authorize` + consent surface. Simulated OAuth/SSO login screens: **none** prior to v0.6.x.

### 4. Consent Screens
- **Documented Before v0.6.x?**: **NO** — terms `consent` / `Consent` present in **0** `.md` files at `v0.5.1` and `v0.6.0`.
- **Emergence point**: `HTML_PREVIEW_DISCOVERY.md:57,61`
- **Exact excerpts**:
  - `| **OAuth2 / OIDC Login & Consent** | /authorize HTML login & scope consent screen | Lets identity & fintech teams preview branded login/consent screens and test "Approve" / "Deny" button redirects. |`
  - `| **Partner Onboarding & Banking Consent** | Open Banking PSD2 / FDX account selection UI | Shows multi-step consent HTML templates rendered with dynamic Handlebars data. |`
- **Classification**: **No prior occurrence at all.**

### 5. Return URL Callbacks
- **Documented Before v0.6.x?**: **NO** — terms `returnUrl` / `return_url` / `RETURN_URL` present in **0** `.md` files at `v0.5.1` and `v0.6.0`.
- **Emergence point**: `HTML_PREVIEW_DISCOVERY.md:16,36` and `e2e/tests/mobile-banking-login-stub.e2e.spec.ts`
- **Exact excerpts**:
  - *(`HTML_PREVIEW_DISCOVERY.md:16`)*: > `- Dynamic template token replacement (__RETURN_URL__ and Handlebars variables)`
  - *(e2e spec)*: > `CRITICAL DISTINCTION: Rendering the login page MUST NOT call the return URL! Only this action should trigger the backend to call the configured return URL.`
- **Classification**: **No prior occurrence at all.**

### 6. Redirect-Based Flows (Simulated HTTP Multi-Hop Redirects)
- **Documented Before v0.6.x?**: **NO** (as a *mock simulation* capability)
- **Pre-v0.6.x file path(s)**: `SPRINT_04_IMPLEMENTATION_PLAN.md:45`, `CHANGELOG.md:64,86`
- **Exact excerpts** (verified at tag `v0.6.0`):
  - *Pre-v0.6.x (`SPRINT_04_IMPLEMENTATION_PLAN.md:45`)*:
    > `- If !token && !isLoading: Redirect to /login via router.replace('/login').`
  - *Pre-v0.6.x (`CHANGELOG.md:64`)*:
    > `- \`AuthGuard.tsx\` — Client-side route guard; redirects unauthenticated users to \`/login\`.`
  - *Emergence (`RELEASE_NOTES_v0.6.1.md:87–95`)*:
    > `When testing endpoints that issue HTTP redirects (e.g., OAuth authentication flows or checkout redirects), the client test runner follows each hop: GET /mock/banking/login (302 Found) └── Location: /mock/banking/dashboard`
  - *Emergence (`HTML_PREVIEW_DISCOVERY.md:18`)*: > `- Conditional webhook triggering (webhook = { url, method }) and 302 redirect chains`
- **Classification**: Pre-v0.6.x = **Strongly Similar in terminology only** (Next.js client-side route redirects to the platform's `/login`). Simulated 302 mock redirect chains: **none** prior to v0.6.x.

### 7. Browser-Rendered Mock Pages
- **Documented Before v0.6.x?**: **NO** — `browser-rendered`, `HTML preview`, `HtmlPreview` present in **0** `.md` files at `v0.5.1` and `v0.6.0`.
- **Pre-v0.6.x file path(s)** (nearest adjacent): `PHASE4_PLAN.md:4` ("result/report step after — summary *preview* before submit" — an import modal), `SPRINT_06_IMPLEMENTATION_PLAN.md`, `SPRINT_06_ARCHITECTURE_REVIEW.md` (live branding *preview* cards)
- **Exact excerpts**:
  - *Pre-v0.6.x (`PHASE4_PLAN.md:4`)*: > `4. UI: full import modal (drag-drop + file input, summary preview before submit, result/report step after).`
  - *Emergence (`HTML_PREVIEW_DISCOVERY.md:20–22`)*:
    > `While Dynamic Mock's backend simulation engine executes these workflows flawlessly, the workspace UI treats HTML responses as plain text/source code. Both the Response Panel (ResponsePanel.tsx) and the Route Details Drawer (RouteDrawer.tsx) render raw HTML strings inside <pre> / JsonHighlighter blocks.`
    > `Discovery Verdict: STRONG GO for v0.6.1.`
- **Classification**: Pre-v0.6.x = **Related Vision** (static UI preview cards for admin theme colors / import metadata). Mock-response browser rendering: **none** prior to v0.6.x.

### 8. Interactive User Journeys & Human-in-the-Loop Simulations
- **Documented Before v0.6.x?**: **NO** — `human-in-the-loop` present in **0** `.md` files at `v0.5.1` and `v0.6.0`.
- **Pre-v0.6.x file path(s)**: `MASTER_PRODUCT_ACTION_PLAN.md:25`, `SPRINT_05_UX_DISCOVERY.md:277,302`
- **Exact excerpts** (verified at tag `v0.6.0`):
  - *Pre-v0.6.x (`MASTER_PRODUCT_ACTION_PLAN.md:25`)*: > `2. Unified stateful scenario engine across all protocols (REST + ISO8583)`
  - *Pre-v0.6.x (`SPRINT_05_UX_DISCOVERY.md:302`)*: > `| **Time-to-First-Mock (TTFM)** | ~3.5 minutes | **< 45 seconds** | First login to first successfully matched mock response |`
  - *Emergence (`HTML_PREVIEW_DISCOVERY.md:52`)*:
    > `Modern API integrations frequently cross the boundary between pure JSON APIs and browser-mediated user journeys ("human-in-the-loop" API flows). Supporting visual HTML preview directly targets: Mobile Banking Login, OAuth2 / OIDC Login & Consent, 3-D Secure (3DS) Challenge Screens...`
- **Classification**: Pre-v0.6.x = **Related Vision** (stateful scenario transitions across backend REST/ISO8583 calls; interactive test-runner ergonomics). Browser-mediated / human-in-the-loop journeys: **none** prior to v0.6.x.

---

## 4. Comprehensive Document Corpus Audit Summary

| Corpus (user-specified name → actual location) | Scope prior to v0.6.x | Pre-v0.6.x evidence of feature? |
|---|---|:---:|
| `MASTER_PRODUCT_ACTION_PLAN.md` | 12-sprint roadmap; Sprint 2 = Authentication Wall (platform login, v0.4.0); "What Should Not Be Built Yet" table defers LDAP/OIDC/SAML (line 369); release table v0.1.0–v0.6.0 | **NO** |
| `ROADMAP_AUTHORITY.md` | Authoritative milestone ledger. `git grep` at `v0.6.0` for login/oauth/consent/redirect/preview: **zero matches** | **NO** |
| `README.md` | Moats: ISO8583, Redis state machine, polyglot scripting, white labeling, self-hosting; `/login` hydration note (line 615) | **NO** |
| `docs/**` (`GOVERNANCE_GAP_ANALYSIS.md`) | Agent governance, clean-architecture enforcement, coding standards | **NO** |
| "sprint/**" (`SPRINT_01`–`SPRINT_06` at root) | ISO8583 productization, ISO simulator, banking sandbox, OAuth/SSO as **inbound** auth (deferred), UX segregation, white-label branding of platform `/login` | **NO** |
| "architecture/**" (`WHITE_LABEL_ARCHITECTURE.md`, sprint architecture reviews) | Single-tenant branding schema, caching, hexagonal layers; `/login` redirect guards | **NO** |
| "roadmap/**" (`FRONTEND_COMPLETION_ROADMAP.md`, `ISO8583_DESIGNER_ROADMAP.md`) | Field dictionaries, packager uploads, client-side route guards (`redirect → /login`) | **NO** |
| All other root `.md` (gap analyses, release reports, migration plans, governance) | — | **NO** |

---

## 5. Final Conclusion

### **The idea first emerged during the Mobile Banking Login / HTML Preview work after v0.6.0; v0.6.1 was a discovered capability, not a previously planned one.**

**Evidence chain (project records only):**

1. **The governing roadmaps did not plan it.** `MASTER_PRODUCT_ACTION_PLAN.md` schedules Sprints 1–12 (Sprint 6 = White Label; Sprint 7 = Scenario State Inspector) and its "What Should Not Be Built Yet" table defers SSO adapters. `ROADMAP_AUTHORITY.md` at tag `v0.6.0` contains no occurrence of login, OAuth, consent, redirect, or preview. The nine feature concepts have **zero** textual presence in any `.md` at `v0.5.1` and `v0.6.0`.

2. **Emergence via end-to-end validation, in the release commit itself.** `HTML_PREVIEW_DISCOVERY.md` states verbatim:
   > *"During end-to-end validation of v0.6.0 (e2e/tests/mobile-banking-login-stub.e2e.spec.ts), Dynamic Mock proved capable of powering a complete, stateful Mobile Banking Login interactive workflow combining text/html response templates... While Dynamic Mock's backend simulation engine executes these workflows flawlessly, the workspace UI treats HTML responses as plain text/source code... Discovery Verdict: STRONG GO for v0.6.1."*

   Git history confirms this document was created in commit `bca3faa` — the exact commit the `v0.6.1` tag (`9bf1a0e`) marks — i.e., the concept was captured at the moment it surfaced, not earlier.

3. **Nature of v0.6.1.** The release was a reactive, high-value discovery: the *existing* backend engine (Handlebars templates + GraalVM polyglot post-scripts + webhooks + 302 support) already could simulate web/login flows as a side effect of earlier capabilities (`v0.2.0` scenario engine and scripting), but documenting, planning, or productizing that as a feature never occurs in any pre-v0.6.x file. The trigger was frontend-only: HTML rendered as raw source. v0.6.1 delivered the sandboxed HTML Preview, RouteDrawer dual-mode, redirect-chain visualization, and webhook correlation as an out-of-band minor release on 2026-10-30 without altering domain entities or prior roadmap commitments.

**One-line answer**: *v0.6.1 implemented a newly discovered emergent capability — the ingredients (HTML templating, scripts, webhooks, 302s) pre-existed in the engine, but no pre-v0.6.x document planned custom HTML login pages, mobile banking login simulation, OAuth/SSO/consent screens, return-URL callbacks, redirect-based flows, browser-rendered mock pages, interactive user journeys, or human-in-the-loop simulations.*

# v0.6.1 – Interactive Simulation Experience

We are proud to announce the general availability of **Dynamic Mock v0.6.1**! 🚀

This release delivers the **Interactive Simulation Experience**, expanding Dynamic Mock from a backend mock server into a high-fidelity, interactive simulation and observability workbench. With hardware-grade sandboxed HTML rendering, zero double execution, real-time multi-criteria console exploration, step-by-step redirect visualization, and correlated webhook telemetry, engineering teams can now simulate and debug complex multi-step web and API flows entirely inside Dynamic Mock.

---

## 🌟 1. Overview

Modern enterprise architectures rely on intricate workflows—multi-hop OAuth/OIDC login redirects, transactional 3D-Secure web journeys, banking authorization callbacks, and asynchronous webhook notifications. Simulating these flows previously required complex external harnesses or left developers blind to intermediate protocol hops.

Dynamic Mock v0.6.1 solves this by turning mock responses into interactive, observable simulations:
- **Zero Double Execution HTML Preview**: Render dynamic HTML mock responses inside an isolated iframe without triggering duplicate HTTP requests against your mock engine.
- **Route Details Source / Preview Dual Mode**: Inspect full Handlebars templates without truncation or preview static mock pages directly in the route drawer.
- **Interactive Traffic Console Explorer**: Search, filter, expand, and diagnose traffic events in real time with sub-second millisecond precision timestamps (`HH:mm:ss.SSS`).
- **Redirect Chain Visualization**: Trace multi-hop HTTP redirect sequences (`302 Found → 302 Found → 200 OK`) with per-hop latency, URL, and header inspection.
- **Correlated Webhook Telemetry**: Asynchronously monitor webhooks triggered by simulated transactions, correlated to their originating parent request.

---

## 🚀 2. Major Features

### 🖥️ Sandboxed HTML Response Preview
Inspect and interact with web pages, login portals, error screens, and payment gateway redirects returned by mock endpoints:
- Automatic detection of HTML responses via `Content-Type: text/html`, `application/xhtml+xml`, `<!DOCTYPE html>`, and `<html>` tags.
- Dynamic tab navigation (`Preview | Body | Headers (N)`) defaulting to `Preview` for HTML payloads and standard `Body` for JSON/text.
- In-memory `srcdoc` rendering preventing unwanted side-effects or double executions.
- Direct `.html` export button (`response.html`) for offline validation and debugging.

### 📐 Route Details Dual Mode (`RouteDrawer`)
- Toggle between `[Source]` and `[Preview]` modes directly in the route drawer, defaulting safely to `Source`.
- Full raw template view with horizontal and vertical scrollbars, eliminating character truncation limits.
- Static preview mode mounts the mock template with scripts completely disabled (`sandbox=""`).

### 🔍 Interactive Traffic Console Explorer
- Comprehensive search bar filtering by HTTP method, path, HTTP status code, response time, Request ID, Route ID, and query parameters.
- Millisecond-precision timestamps (`[HH:mm:ss.SSS]`) and high-resolution duration metrics (`33.41ms`).
- Expandable event rows revealing dedicated tabs for Query Parameters, Request/Response Headers, Request/Response Payloads, Redirect Chains, Webhooks, and Raw Telemetry.
- Enhanced Request ID context (`Req: 1d72f8be-f1d2...`) and full Route ID exposure with one-click clipboard copying.

### 🔄 Multi-Hop Redirect Chain Tracker
- Integrated step-by-step redirect follower recording every intermediate 301/302/307/308 redirect hop.
- Visual stepper banner (`RedirectChainBanner`) displaying transitions, intermediate URLs, status codes, and individual hop latency.

### ⚡ Correlated Webhook Delivery Inspection
- Asynchronous correlation engine linking mock-triggered webhooks to their parent request via `parentEventId`.
- Full webhook payload inspection, target destination URLs, and delivery status indicators.

---

## 📊 3. Console Explorer Improvements

| Capability | Previous Behavior | v0.6.1 Interactive Experience |
|---|---|---|
| **Timestamp Precision** | Second-rounded `[13:01:32]` | Sub-second millisecond `[13:01:32.421]` (`HH:mm:ss.SSS`) |
| **Duration Metrics** | Integer or omitted | High-resolution duration (e.g., `33.41ms`) |
| **Request ID Visibility** | Truncated `ID: 1d72f8be...` | Expanded context `Req: 1d72f8be-f1d2...` + 1-click UUID copy |
| **Route ID Visibility** | Hidden in metadata | Fully exposed in collapsed & expanded headers with 1-click copy |
| **Search & Filtering** | Basic text match | Multi-criteria real-time search across method, path, status, duration, IDs, and query params |
| **Query Parameters** | Raw URL string only | Decoded key-value inspection table with copy support |
| **Payload Inspection** | Truncated inline text | Dedicated formatted views for Request/Response bodies |
| **Payload Protection** | Unbounded display | 32 KB truncation threshold + binary payload suppression |

---

## 🎨 4. HTML Preview Improvements

### The Zero Double Execution Guarantee
In traditional tooling, rendering an HTML preview often mounts an iframe pointing to the mock URL, triggering a second HTTP request against the mock route or downstream backend. In stateful or transactional simulations (e.g., balance deductions or single-use OTP validation), this secondary execution corrupts the scenario state.

Dynamic Mock v0.6.1 eliminates this flaw:
1. The initial execution captures the exact `response.body` returned by the mock server.
2. The `HtmlPreviewFrame` renders this captured response strictly in memory using the iframe `srcdoc` attribute.
3. Network traffic interceptors record **zero** automated network round-trips upon preview render.

### Non-Invasive URL Normalization Shim (`htmlPreviewShim`)
- Injects a `<base>` tag targeting the active mock workspace origin.
- Mounts `window.__DYNAMIC_MOCK_CONTEXT__` containing workspace metadata, route identifiers, and execution timestamps.
- Surgically normalizes relative hyperlinks and `<form action="/mock/...">` targets to ensure seamless multi-step simulation within the sandbox.
- Employs `MutationObserver` and postMessage signaling to automatically adjust iframe container height, preventing awkward nested scrollbars.

---

## 🔀 5. Redirect & Webhook Tracking

### Redirect Chain Visualization
When testing endpoints that issue HTTP redirects (e.g., OAuth authentication flows or checkout redirects), the client test runner follows each hop:
```
GET /mock/banking/login (302 Found)
  └── Location: /mock/banking/dashboard (302 Found)
        └── Location: /mock/banking/overview (200 OK)
```
The `RedirectChainBanner` visualizes each jump as an interactive stepper breadcrumb, exposing the status code, destination URL, headers, and per-hop execution timing.

### Webhook Correlation Telemetry
Mock routes configured with asynchronous webhook dispatches now publish enriched telemetry:
- Every dispatched webhook is tagged with `parentEventId` matching the triggering execution event.
- Console Explorer surfaces all child webhook executions under a dedicated **Webhooks** tab.
- Displays destination URL, payload body, response status, and round-trip delivery duration.

---

## 🛡️ 6. Security Improvements

Dynamic Mock v0.6.1 enforces defense-in-depth isolation for all simulated HTML content:

- **Strict Sandbox Flags**: Interactive previews use `sandbox="allow-scripts allow-forms allow-popups"`.
- **Origin Isolation**: Previews explicitly omit `allow-same-origin`. The iframe executes with an opaque `Origin: null`, preventing sandboxed code from accessing parent cookies, `localStorage`, `sessionStorage`, or executing cross-document scripts against the Dynamic Mock application.
- **Top-Navigation Prevention**: Previews explicitly omit `allow-top-navigation`, preventing simulated forms or scripts from redirecting or hijacking the host application window.
- **Static Preview Lockdown**: In the Route Details drawer, preview mode strictly enforces `sandbox=""`, completely disabling JavaScript execution.
- **No Referrer Leaks**: All preview frames declare `referrerPolicy="no-referrer"`.
- **Targeted CORS Configuration**: Spring Security's `WebConfig.java` enables permissive CORS (`allowedOriginPatterns("*")`) strictly for `/mock/**` paths to allow sandboxed `Origin: null` iframes to execute mock API requests, while administrative `/api/**` endpoints remain strictly restricted.

---

## ⚡ 7. Performance Improvements

- **In-Memory DOM Hydration**: Zero network overhead on preview mount using pre-captured response buffers.
- **Sub-3s Next.js Build**: Optimized React tree and selective bundle imports achieve production Next.js builds in under 2.8 seconds.
- **Redis Pub/Sub Stream Optimization**: Low-latency telemetry publication pipeline with bounded 32 KB payload payloads preventing buffer saturation during high-throughput mock load tests.
- **Efficient DOM Resizing**: Debounced `ResizeObserver` and `MutationObserver` handlers ensure smooth responsive scaling without layout thrashing.

---

## 🔄 8. Backward Compatibility

- **100% Backward Compatible**: All existing mock routes, collections, environments, scenario state machines, ISO8583 financial engines, and white-label configurations continue to work without modification.
- **Non-HTML Payloads Unaffected**: JSON, XML, and plain-text mock endpoints maintain their existing `Body | Headers (N)` tab structure and editing experience.
- **Zero Database Migration**: Requires no MongoDB schema changes or data migrations.

---

## 🚫 9. Breaking Changes

**None.** Dynamic Mock v0.6.1 introduces no breaking changes to REST, GraphQL, gRPC, or ISO8583 interfaces, configuration files, or database schemas.

---

## 📦 10. Upgrade Notes

Upgrading to **v0.6.1** is seamless and immediate:

### Docker Compose
Pull the latest images and restart your stack:
```bash
docker compose pull
docker compose up -d
```

### Verification
Once launched, verify all services are healthy:
```bash
docker compose ps
```
Navigate to your workspace, execute an HTML mock route (such as `/mock/banking/login`), and observe the new interactive **Preview** tab and enriched **Console Explorer**!

---

## ❤️ Community & Support

* Explore code & release assets: [GitHub Repository](https://github.com/sanjokpro/dynamic-mock-project)
* Report issues & feedback: [GitHub Issues](https://github.com/sanjokpro/dynamic-mock-project/issues)
* Documentation: [Dynamic Mock Docs](https://github.com/sanjokpro/dynamic-mock-project/tree/master/docs)

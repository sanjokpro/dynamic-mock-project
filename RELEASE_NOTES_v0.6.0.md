# v0.6.0 – White Label Foundation, Enterprise Customization & Polyglot Template Scripting

We are thrilled to announce the official release of **Dynamic Mock v0.6.0**! 🚀

This milestone release equips Dynamic Mock with end-to-end **White Label Foundation & Enterprise Customization**, empowering teams, platform engineering organizations, and OEM partners to deploy Dynamic Mock under their own custom corporate identity with zero image rebuilds. 

Additionally, v0.6.0 significantly expands the platform's developer simulation capabilities with **Polyglot Post-Script Access to Rendered Response Templates**, transforming Dynamic Mock into a fully programmable simulation platform where templates provide realistic mock payloads and scripts apply dynamic business logic in real time.

---

## 🌟 Release Overview

Dynamic Mock v0.6.0 bridges the gap between turnkey developer tools and enterprise-tailored platforms:
- **Instant White-Labeling**: Rebrand colors, logos, titles, and legal footers live through the UI or inject them via environment variables at container launch.
- **Enterprise-Grade UI Experience**: Built with Next.js 16 App Router Server-Side Rendering (SSR), delivering **0ms Flash of Unstyled Content (FOUC)** and automated ITU-R BT.601 luminance-safe contrast.
- **Programmable Simulation Engine**: Post-scripts in JavaScript or Python can now inspect and mutate fully rendered Handlebars templates, enabling complex dynamic flows with minimal code.
- **Zero-Migration Upgrade**: Pure single-tenant architecture with 100% backward compatibility across REST, GraphQL, gRPC, and ISO8583 engines.

---

## 🚀 Major Features

### 🎨 White Label Foundation

Transform Dynamic Mock into your organization's internal developer portal or branded partner sandbox:

* **Runtime Branding Engine**: Update corporate branding dynamically without rebuilding container images or restarting services.
* **Dynamic Logos & Icons**: Configure custom SVG, PNG, or WebP logo URLs with graceful fallback to standard iconography if the image is unavailable.
* **Dynamic Browser Favicon**: Synchronize browser tab favicons with your organization's custom branding icon.
* **Custom Document Titles**: Personalize the browser window and tab titles (e.g., `Acme Corp Mock Platform - API Workspace`).
* **Custom Palette & Automated Contrast**: Define your brand's Primary, Secondary, and Accent colors. Built-in ITU-R BT.601 luminance analysis automatically selects contrasting text foregrounds (`#ffffff` vs `#0f172a`) to guarantee WCAG compliance.
* **Branded Login Page**: Custom product title, organization name, legal footer, and optional background wallpaper for customer-facing or internal portals.
* **Admin-Only Branding Management**: Intuitive modal inside the workspace header allowing authorized administrators to adjust hex colors, preview changes in real time, and revert to platform defaults with one click.
* **Environment Variable Overrides (`WHITE_LABEL_*`)**: OEM and GitOps teams can inject branding deterministically via environment variables (`WHITE_LABEL_PRODUCT_NAME`, `WHITE_LABEL_PRIMARY_COLOR`, etc.), locking in corporate branding on container boot.
* **Zero-Rebuild Docker Distribution**: Ship a single immutable Docker image across different environments, clients, or business units with distinct branding configured strictly via runtime config.

### 🏢 Enterprise Readiness & Performance

* **Server-Side Rendered (SSR) Hydration**: The root layout pre-fetches instance branding on the server and injects an inline `<style id="ssr-branding-theme">` block into the initial HTML document.
* **Zero FOUC (Flash of Unstyled Content)**: Users experience immediate, correctly themed pages on the first paint—eliminating visual color pops and theme flickering during client hydration.
* **High-Performance Redis Branding Cache**: Effective branding configurations are cached in Redis (`config:branding:instance`) with a 24-hour TTL and synchronous eviction upon admin updates, backed by an automatic fail-open mechanism to MongoDB.
* **Strict Admin RBAC Controls**: Branding modification endpoints (`PUT /api/config/branding` and `POST /reset`) are strictly guarded with Spring Security Role-Based Access Control (`ROLE_ADMIN`). Public read endpoints allow unauthenticated login screens to display branding securely.

### ⚡ Polyglot Scripting Enhancement: Template → Script Pipeline

Dynamic Mock now unifies its Handlebars template engine and GraalVM Polyglot scripting runtime into a seamless two-phase pipeline. Post-scripts (JavaScript and Python) can now access `response.body`, which contains the fully rendered output from the template (including UUIDs, timestamps, and request parameters).

This turns templates into intermediate data models that scripts can parse, inspect, and mutate before returning the final response to the client.

#### JavaScript Example
```javascript
// Post-Script: Parse the rendered template JSON, mutate fields, and return
const data = JSON.parse(response.body);

// Append dynamic business rules or audit codes
data.trackingCode += "X";
data.processedAt = new Date().toISOString();

response.body = JSON.stringify(data, null, 2);
```

#### Python Example
```python
# Post-Script: Parse the rendered template JSON, mutate fields, and return
import json

data = json.loads(response['body'])

# Dynamically transform response payload
data['trackingCode'] += 'X'
data['status'] = 'ENRICHED'

response['body'] = json.dumps(data, indent=2)
```

#### The Power of the Pipeline
Instead of writing complex string concatenation or extensive boilerplate scripts, developers can generate realistic payloads using Handlebars helpers (`{{$randomUUID}}`, `{{$randomEmail}}`, `{{now}}`), and use lightweight post-scripts solely for custom business logic.

---

## 🛠️ Technical Highlights

* **Hexagonal Architecture Compliance**: All white-label domain logic is decoupled using inbound ports (`OrganizationConfigUseCase`) and outbound ports (`OrganizationConfigRepository`), ensuring clean separation between domain, application services, and infrastructure adapters.
* **Pure Domain Model**: The core `OrganizationConfig` entity is a pure Java POJO (`SINGLETON_ID = "instance"`) free from any Spring Data, Jackson, or database annotations.
* **Single-Tenant Architectural Invariant**: Strictly designed for single-tenant instances. Zero multi-tenant complexity, tenant tables, or request header pollution was introduced, keeping deployment simple, performant, and secure.
* **Robust Security & Input Sanitization**:
  * Strict hex code validation (`^#([A-Fa-f0-9]{6})$`) preventing CSS injection.
  * Field length constraints on product names and URLs.
  * Role-based access control protecting all administrative endpoints.

---

## 📊 Quality & Validation Metrics

Every commit and build artifact in v0.6.0 was validated through rigorous automated test suites and end-to-end container checks:

* **Backend Unit & Integration Tests**: **394 / 394 passed** (100% green).
* **Frontend Component & Unit Tests**: **89 / 89 passed** across 13 test suites (Vitest).
* **Production Builds**:
  * Spring Boot executable JAR (`bootJar`) generated successfully with 0 warnings.
  * Next.js 16 (Turbopack) production build completed in **1518ms** with **0 TypeScript errors**.
* **Live Docker Verification**: Verified seamless operation of all 5 stack services (`mongo`, `redis`, `mock-server`, `frontend`, `nginx`) on Port 80.
* **Zero Regressions**: 100% compatibility preserved across existing REST mocks, Stateful Banking Sandboxes, ISO8583 message simulators, and gRPC/GraphQL endpoints.

---

## 🔄 Upgrade Notes

Upgrading to **v0.6.0** is completely non-breaking and requires **zero database migrations**:

1. Pull the latest Docker images or update your deployment manifests:
   ```bash
   docker compose pull
   docker compose up -d
   ```
2. Existing deployments will automatically initialize with default Dynamic Mock branding without any disruption.
3. To customize branding via GitOps or container orchestration, supply optional environment variables:
   ```env
   WHITE_LABEL_PRODUCT_NAME="FinTech Sandbox"
   WHITE_LABEL_ORG_NAME="Acme Corp"
   WHITE_LABEL_PRIMARY_COLOR="#2563eb"
   WHITE_LABEL_LOGO_URL="https://example.com/logo.svg"
   ```

---

## ❤️ Thank You

A huge thank you to all contributors, enterprise evaluators, and community members whose architectural reviews, feedback, and real-world testing helped shape this release!

* Explore the docs & code: [GitHub Repository](https://github.com/sanjokpro/dynamic-mock-project)
* Report issues & feedback: [GitHub Issues](https://github.com/sanjokpro/dynamic-mock-project/issues)


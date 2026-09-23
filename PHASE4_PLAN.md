# Phase 4 Implementation Plan — Postman Parity & Collection Importer

> **Purpose:** Self-contained brief for the implementing agent (Gemini CLI / Antigravity). Every decision is already resolved — do not re-litigate them. Work top to bottom. Do not modify files outside the scope listed below.
>
> **Current state:** Backend = Spring Boot 4.0.1 / Java 21, clean-hexagonal (`domain/` pure, `application/service/`, `adapter/in/web/`, `adapter/out/`, `infrastructure/`). Frontend = Next.js 16 App Router in `frontend/`. Baseline: 327 backend tests green, frontend `tsc --noEmit` exit 0.

---

## 0. Decisions (resolved — do not change)

1. **Import scope:** Postman collection JSON **and** optional environment JSON files.
2. **Request headers/query params → matchers** (`matchers.headers` / `matchers.queryParams`), only entries with non-empty `value`, skip `disabled: true`. Multi-match registry (Phase 2) handles same-path variants — do not dedupe.
3. **Response templates:** use saved example (`response[]`) whose `code` is 2xx → body as `responseTemplate`, `code` as `responseStatus`, `header[]` as `responseHeaders`. Fallback when no example: `responseStatus` 200 + body `{"message": "Mocked from Postman collection"}`.
4. **UI:** full import modal (drag-drop + file input, summary preview before submit, result/report step after).
5. **Postman scripts** (`prerequest`/`test` events): **ignore** — record a warning in the import report, do not store.
6. **Re-import duplicates:** report, don't dedupe.
7. **Imported routes must land active** — add `active` to `CreateRouteRequest` (default `true` when absent) and set `active: true` in `RouteService.createRoute` when null. Keep `active` nullable in `MockRoute`/DB to avoid touching existing rows.

## 1. Backend Part A — Postman Template Aliases

### [MODIFY] `backend/src/main/java/com/dynamicmock/adapter/out/template/ResponseTemplateEngine.java`
Register **helper aliases** mapping Postman variables to existing `RandomDataFunctions` methods:

| Postman variable | Implementation |
|---|---|
| `{{$guid}}` | `RandomDataFunctions.randomUUID()` |
| `{{$isoTimestamp}}` | `java.time.Instant.now().toString()` |
| `{{now}}` | `RandomDataFunctions.timestamp()` |
| `{{uuid}}` | `RandomDataFunctions.randomUUID()` |
| `{{$randomFirstName}}`, `{{$randomLastName}}` | new `RandomDataFunctions.randomFirstName()/randomLastName()` — static arrays of ~20 common first/last names each |

### ⚠️ CRITICAL — helper/context shadowing
Handlebars resolves `{{uuid}}` / `{{now}}` against the **context map first**; helpers only fire when the key is absent. The template context (see `DynamicRouteDispatcher` ~line 197) binds `request`, `env`, `vars`, plus path variables. You MUST add tests proving:
- A context containing `uuid: "literal"` renders `literal`, NOT a random UUID (helpers must not shadow existing context keys).
- Path variables (`{{id}}`) still render.
- `request.*`, `env.*` references still render.

If Handlebars' helper-vs-context precedence makes the alias impossible without shadowing, implement the aliases as a **pre-processor that rewrites `{{$guid}}` → `{{$randomUUID}}` etc. in the template string before compilation**, and note the chosen mechanism in the code.

### [MODIFY] `backend/src/main/java/com/dynamicmock/adapter/out/template/RandomDataFunctions.java`
Add `randomFirstName()` / `randomLastName()`.

### [NEW] `backend/src/test/java/com/dynamicmock/core/template/PostmanAliasTest.java`
- `{{$guid}}` output parses as UUID; equals format of `{{$randomUUID}}`
- `{{$isoTimestamp}}` parses with `Instant.parse`
- `{{now}}` numeric; `{{$randomFirstName}}`/`{{$randomLastName}}` non-empty
- **Shadowing test** (the critical one) per above

## 2. Backend Part B — Postman Collection v2.1 Importer

### [NEW] `backend/src/main/java/com/dynamicmock/application/service/PostmanCollectionModel.java`
Typed DTOs (Jackson, Lombok `@Data/@Builder`) for the Postman v2.1 JSON shape: collection (`info`, `item[]`), item (nested `item[]` OR `request`), request (`method`, `url` as object `{raw, path[], query[]}` OR string, `header[]`, `body{mode, raw}`), response (saved example: `code`, `body`, `header[]`). Use `@JsonIgnoreProperties(ignoreUnknown = true)`. Tolerate `url` being a plain string OR object.

### [NEW] `backend/src/main/java/com/dynamicmock/application/service/PostmanImportService.java`
Application-layer service (constructor-injected `RouteService`, `WorkspaceCollectionService`, `WorkspaceEnvironmentService`, `ObjectMapper`):

1. **Environments** (from separate Postman environment JSON files: `name`, `values[] {key, value, enabled}`) → `WorkspaceEnvironmentService.createEnvironment(name, vars)` — skip `enabled: false` entries.
2. **Folders:** top-level `item[].item[]` folders → one `WorkspaceCollectionService.createCollection(folderName, "default-user", null)` each; nested folders flatten with `CollectionItem.name = "Parent/Child"`.
3. **Requests** → `routeService.createRoute(...)` with:
   - `method` from `request.method`
   - `path` from `url.raw` (strip scheme+host, keep path + query). Also support `url.path[]` + `url.query[]` when `raw` absent.
   - **Path variables:** Postman `:id` style → convert `:param` → `{param}` before saving (verify `RouteRegistry`'s regex substitution expects `{param}` — it does; see `infrastructure/filter/RouteRegistry.java` path-variable handling).
   - `request.header[]` (skip `disabled`, empty values) → `matchers.headers`
   - `url.query[]` (skip `disabled`, empty values) → `matchers.queryParams`
   - **No body matchers** from request bodies.
   - Response template per Decision 3.
4. **After createRoute:** `collectionService.addItemToCollection(collectionId, CollectionItem{protocol: "HTTP", resourceId, name: request name})` so route names survive (routes have no name field — names live on collection items).
5. **Report:** return counts (environments, collections, routes) + warnings list (skipped scripts, file-mode bodies, nameless requests) + per-route errors. Import continues after per-item failure (collect error, move on).

### [NEW] `backend/src/main/java/com/dynamicmock/adapter/in/web/ImportController.java`
```
POST /api/import/postman   (multipart/form-data)
  - file: collection JSON (required)
  - files[]: environment JSONs (optional, multiple)
```
Returns the `ImportReport`. Multipart limits in `application.yml`: `spring.servlet.multipart.max-file-size: 10MB`, `max-request-size: 25MB`.

### [MODIFY] `backend/src/main/resources/application.yml` — multipart limits only.

### [NEW] Tests
- `backend/src/test/java/com/dynamicmock/service/PostmanImportServiceTest.java` with fixture JSONs under `backend/src/test/resources/postman/` (minimal v2.1 collection with 1 folder, 2 requests — one with headers/query params/example response, one with `:id` path variable — plus one environment file).
- Assert: routes created active, path `:id` → `{id}`, header/query matchers populated, example body became `responseTemplate`, environment created with variables, warnings for skipped scripts, folder → collection with correctly-named items.
- Controller test for `ImportController` (MockMvc, follow `RouteControllerTest` patterns; use small inline JSON, `MockMvcRequestBuilders.multipart()`).

## 3. Frontend — Import Modal

### [NEW] `frontend/src/components/workspace/ImportModal.tsx`
- Controlled modal (open/onClose props), drag-drop + click-to-browse file input, accepts `.json`; client-side size/extension validation; list chosen files with remove buttons
- **Preview step:** after files chosen, show file names + sizes and what will happen; **Submit** → POST
- POST via `apiClient` (FormData to `/import/postman`), `X-API-KEY` attaches automatically. Add a `usePostmanImport` hook following existing hook conventions (`@tanstack/react-query` useMutation).
- **Result step:** render `ImportReport` (counts, warnings, errors). On success invalidate `routes` and `collections` queries so the sidebar refreshes.

### [MODIFY] `frontend/src/components/workspace/Header.tsx`
The `Import` button (Quick Actions, currently has NO onClick) opens `ImportModal`.

### [MODIFY] `frontend/src/types/index.ts`
Add `ImportReport` type: `{ environments: number; collections: number; routes: number; warnings: string[]; errors: string[] }`.

## 4. Verification (all must pass before you report done)

1. `cd backend && ./gradlew test --console=plain` → BUILD SUCCESSFUL, zero failures. Report the **exact test count** from `grep -h -o 'tests="[0-9]*"' build/test-results/test/*.xml | grep -o '[0-9]*' | awk '{s+=$1} END {print s}'`.
2. `cd frontend && npx tsc --noEmit` → exit 0.
3. `cd frontend && npm run build` → compiles, all pages prerender.
4. Manual smoke (if backend+frontend run locally): import the fixture collection via UI, verify sidebar rows + activation + a mock response renders with `{{$guid}}`.

## 5. Scope boundaries (hard)

- Do NOT touch: `RouteRegistry`, `RequestMatcher`, `DynamicRouteDispatcher`, `AsyncWebhookService`, `ScriptEngine`, `application.yml` beyond the two multipart lines, anything in `infrastructure/filter/`.
- Do NOT add a route `name` column.
- Do NOT auto-dedupe re-imports.
- Domain layer stays framework-annotation-free (project mandate).

## 6. Report format (respond with exactly this)

Walkthrough listing: files created/modified with 1-line purpose each; the two verification counts (backend tests total, tsc exit code); any deviations from this plan with reasons; the ImportReport shape you implemented.

# White Label Architecture Design

## 1. Current Architecture
Currently, the Dynamic Mock platform operates as a single-tenant, statically configured application.

### Frontend
- **Theme**: Statically compiled in `globals.css` with a binary dark/light mode toggle.
- **Branding**: The logo (Lucide `<Globe>`), favicon (`favicon.ico`), and product name ("Dynamic Mock") are hardcoded into React components (`Header.tsx`, `layout.tsx`).
- **Configuration**: Managed via build-time `.env` files. No runtime configuration endpoint exists.

### Backend
- **Storage**: MongoDB collections (`environments`, `mockRoutes`, `scenarios`, etc.) lack a `tenantId` or `organizationId`.
- **Configuration**: Uses `application.yml` for system configuration, but has no concept of UI or organizational configuration storage.

---

## 2. Target Architecture

The target architecture introduces a runtime branding system, preparing the foundation for eventual multi-tenancy while immediately solving white-label requirements for single-tenant enterprise deployments.

### 2.1 Organization Branding Model (MongoDB)
A new `OrganizationConfig` entity will be introduced in the backend:
```java
@Document(collection = "organization_config")
public class OrganizationConfig {
    @Id
    private String id; // e.g., "default" or tenant-id
    private String productName;
    private String organizationName;
    private String logoUrl;
    private String faviconUrl;
    
    // Hex colors
    private String primaryColor;
    private String secondaryColor;
    
    private String loginBackgroundUrl;
    private String footerText;
}
```

### 2.2 Administration & Backend
- **Branding API**: New `OrganizationConfigController` with `GET /api/config/branding` and `PUT /api/config/branding`.
- **Branding Storage**: Persisted in MongoDB under `organization_config`.
- **Branding Cache**: 
  - Read-heavy endpoint; will be cached in Redis with a TTL of 24 hours.
  - Updates via `PUT` will immediately invalidate the Redis cache key.

### 2.3 Frontend Implementation
- **BrandingProvider**: 
  - A React Context provider that fetches `/api/config/branding` at startup.
  - Exposes `useBranding()` hook to the rest of the app.
- **Runtime Theme Injection**:
  - Instead of static CSS variables, the `BrandingProvider` will inject a `<style>` tag into the DOM head:
    ```css
    :root {
      --primary: ${branding.primaryColor};
      --secondary: ${branding.secondaryColor};
    }
    ```
- **Dynamic Metadata & Favicon**:
  - `layout.tsx` will be refactored to fetch the configuration on the server side (Server Component) to dynamically inject `<title>` and `<link rel="icon">`.
- **Dynamic UI Components**:
  - `Header.tsx` will replace the Lucide `<Globe>` with `<img src={branding.logoUrl} />`.
  - Product name text will fall back to `branding.productName`.

### 2.4 Deployment Overrides
To support zero-touch enterprise deployments (e.g., Docker deployments where DB seeding is difficult), configuration can be overridden via Environment Variables:
- `WHITE_LABEL_PRODUCT_NAME`
- `WHITE_LABEL_LOGO_URL`
- `WHITE_LABEL_PRIMARY_COLOR`

When the backend starts, if these variables are present, they will take precedence or seed the database on initialization.

---

## 3. Migration Strategy

1. **Phase 1: Backend Scaffolding**
   - Create `OrganizationConfig` Mongo entity, repository, and service.
   - Implement `/api/config/branding` endpoint with Redis caching.
   - Implement Environment Variable bootstrap logic.
2. **Phase 2: Frontend Context & Hydration**
   - Create `BrandingProvider.tsx`.
   - Update `layout.tsx` to handle SSR branding (Title, Favicon).
3. **Phase 3: Component Refactoring**
   - Refactor `Header.tsx`, `Sidebar.tsx`, and `globals.css` to respect the injected CSS variables and branding properties.
   - Provide a default configuration fallback that mimics the current "Dynamic Mock" UI.
4. **Phase 4: Admin UI**
   - Build a "Settings > Branding" UI in the frontend to allow admins to upload logos and select colors via a color picker.

---

## 4. Risks

- **Server-Side Rendering (SSR) Flickering**: If the `BrandingProvider` relies entirely on client-side fetching, users will see the default blue "Dynamic Mock" theme for a split second before the custom theme loads. *Mitigation: Perform the fetch in Next.js Server Components and pass it down.*
- **CSS Variable Conflicts**: Tailwind's opacity modifiers (e.g., `bg-primary/50`) require CSS variables to be defined in RGB format (e.g., `255 255 255`). If the API returns a Hex code (`#3b82f6`), it must be converted to RGB channels before being injected into the DOM.
- **Image Hosting**: If users upload custom logos, the backend must support file storage (GridFS or S3). *Mitigation: Initial version will only accept remote URLs.*

---

## 5. Future Multi-Tenant Compatibility

While this design targets a single enterprise installation, it is fundamentally multi-tenant ready:
- The `OrganizationConfig` entity uses an `ID`. 
- In a future multi-tenant SaaS environment, the `tenantId` can be derived from the subdomain (e.g., `acme.dynamicmock.com`) or authentication token.
- The frontend `BrandingProvider` will extract the tenant ID from the host header and pass it to `/api/config/branding?tenant=acme`, retrieving tenant-specific white labeling seamlessly.

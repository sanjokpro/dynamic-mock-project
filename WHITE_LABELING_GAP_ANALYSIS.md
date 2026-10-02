# White Labeling Gap Analysis

## Overview
This document analyzes the current frontend architecture to determine its readiness for white-labeling (allowing enterprise customers to customize the UI with their own branding, logos, and themes).

## Findings

### 1. Theme Implementation
- **Current State**: Themes are implemented using raw CSS variables inside `frontend/src/app/globals.css`. A `ThemeContext.tsx` toggles a `.dark` class on the HTML root element. Tailwind CSS is configured to use these variables.
- **Gap**: There is no mechanism to fetch brand colors (e.g., a custom `--primary` hex code) from a backend API or configuration file. Colors are hardcoded into the build.

### 2. Logo Overrides
- **Current State**: The application logo is a hardcoded Lucide React icon (`<Globe />`) located in `frontend/src/components/workspace/Header.tsx`.
- **Gap**: There is no support for injecting a custom logo URL or SVG path. The logo cannot be changed without modifying the source code and recompiling the frontend.

### 3. Favicon Overrides
- **Current State**: The favicon is a static file located at `frontend/src/app/favicon.ico`.
- **Gap**: Next.js serves this static file by default. There is no dynamic `<link rel="icon">` injection based on organization config.

### 4. Product Name Overrides
- **Current State**: The product name "Dynamic Mock" is hardcoded in multiple places:
  - `frontend/src/app/layout.tsx` (`title: "Dynamic Mock API Workspace"`)
  - `frontend/src/components/workspace/Header.tsx` (`<span>Dynamic Mock</span>`)
- **Gap**: The application does not read the product name from an environment variable or backend configuration.

### 5. Organization-Specific Configuration
- **Current State**: The frontend relies on a simple `.env` configuration (e.g., `NEXT_PUBLIC_API_KEY`, `BACKEND_URL`), but has absolutely no concept of a "Tenant", "Organization", or "White Label Config".
- **Gap**: There is no backend endpoint (e.g., `/api/config/branding`) to serve organization-specific settings. 

## Conclusion
The frontend is currently **0% ready** for white-labeling. Every branding element (colors, logo, favicon, product name) is statically compiled into the React bundle. 

**Required Architecture Shift**:
To support white-labeling, the application must introduce a `BrandingProvider` or `TenantContext` that fetches configuration at runtime or build-time, dynamically injecting CSS variables and replacing static assets.

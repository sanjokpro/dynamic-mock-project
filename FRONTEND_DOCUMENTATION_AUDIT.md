# Frontend Documentation Audit

## Overview
This audit identifies references to the legacy frontend framework (Angular, Angular 17, AngularJS, NgModule, Angular CLI) following the migration to **Next.js App Router**. The purpose is to identify outdated references and propose updates to accurately reflect the current Next.js architecture.

## Findings

### `AGENT_SPECIALIZATION_PLAN.md`

- **Line 14:** `- **Scope**: Angular UI, backend DTOs, REST controllers.`
  - **Classification:** Incorrect current-state documentation
  - **Proposed Update:** `- **Scope**: Next.js UI, backend DTOs, REST controllers.`

### `README.md`

- **Line 114:** `- **Framework**: Angular 17+ with TypeScript`
  - **Classification:** Incorrect current-state documentation
  - **Proposed Update:** `- **Framework**: Next.js App Router with TypeScript`

- **Line 115:** `- **UI**: Angular Material with custom Nepali rupee papernote theme`
  - **Classification:** Incorrect current-state documentation
  - **Proposed Update:** `- **UI**: Tailwind CSS / React components with custom Nepali rupee papernote theme`

- **Line 128:** `- Angular CLI 17+ (for frontend)`
  - **Classification:** Incorrect current-state documentation
  - **Proposed Update:** `- Node.js 18+ (for Next.js frontend)`

- **Line 215:** `- run **backend and Angular separately** during development, or`
  - **Classification:** Incorrect current-state documentation
  - **Proposed Update:** `- run **backend and Next.js separately** during development, or`

- **Line 221:** `### 1. Dev: Backend + Angular Separate`
  - **Classification:** Incorrect current-state documentation
  - **Proposed Update:** `### 1. Dev: Backend + Next.js Separate`

- **Line 229:** `- **Frontend** (Angular dev server):`
  - **Classification:** Incorrect current-state documentation
  - **Proposed Update:** `- **Frontend** (Next.js dev server):`

- **Line 236:** `- Angular app runs on \`http://localhost:4200\``
  - **Classification:** Incorrect current-state documentation
  - **Proposed Update:** `- Next.js app runs on \`http://localhost:3000\`` (Default Next.js port)

- **Line 239:** `This is the recommended mode for daily development (hot reload on the Angular side).`
  - **Classification:** Incorrect current-state documentation
  - **Proposed Update:** `This is the recommended mode for daily development (Fast Refresh on the Next.js side).`

- **Line 243:** `Build Angular and bundle the \`dist\` output into Spring Boot static resources:`
  - **Classification:** Documentation bug / Incorrect current-state documentation
  - **Proposed Update:** `Build Next.js (static export) and bundle the \`out\` directory into Spring Boot static resources:` (Assuming static export is used).

- **Line 256:** `- Angular UI (served by Spring Boot): \`http://localhost:8080/\``
  - **Classification:** Incorrect current-state documentation
  - **Proposed Update:** `- Next.js UI (served by Spring Boot): \`http://localhost:8080/\``

### `backend/build.gradle`

- **Line 186:** `// Optional Angular build + bundle into static resources when enabled with -PbuildWithEmbeddedUi=true`
  - **Classification:** Documentation bug (Code Comment)
  - **Proposed Update:** `// Optional Next.js build + bundle into static resources when enabled with -PbuildWithEmbeddedUi=true`

## Summary
- **Total Angular References Found:** 12
- **AngularJS References Found:** 0
- **NgModule References Found:** 0
- **Angular CLI References Found:** 1 (Included in Angular references)

All findings have been classified as "Incorrect current-state documentation" or "Documentation bug" because they actively misinform developers about the current frontend architecture. These files should be updated to reflect the Next.js App Router stack.

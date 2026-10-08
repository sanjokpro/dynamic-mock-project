# Sprint 3 Architecture Review: Sandbox Delivery Mechanism

## 1. Context and Problem Statement
The Sprint 3 Execution Plan proposed `SandboxDataInitializer.java`—an automatic backend startup script that detects an empty database and injects the Reference Banking Sandbox. 

While this maximizes the "First-Time User Experience" (zero clicks), it introduces severe enterprise safety risks. Automatic data injection in enterprise software is generally considered a dangerous anti-pattern. This document evaluates alternative delivery mechanisms to find the optimal balance between the "5-minute wow" and enterprise-grade safety.

## 2. Evaluation of Approaches

### A. Automatic Backend Startup Initialization (`SandboxDataInitializer.java`)
- **User Experience**: Excellent (0 clicks).
- **Enterprise Safety**: **FAIL**. If a production database is spun up fresh, the application will inject test data into a production environment. Even guarded by an environment variable, operator error can cause severe pollution.
- **Maintainability**: Poor. Building complex relational entities (Workspaces, Environments, Endpoints, Scenarios, Mocks) via Java code requires constant maintenance as the internal domain model evolves.
- **Production Risk**: High.

### B. Importable Banking Sandbox Bundle (Manual JSON Upload)
- **User Experience**: **FAIL**. A new evaluator must find a JSON file in the GitHub repo, download it, navigate to an Import screen in the UI, upload it, and hope it works. This destroys the frictionless "wow" moment.
- **Enterprise Safety**: Excellent. Zero accidental pollution.
- **Maintainability**: Good. 
- **Production Risk**: Zero.

### C. Example Workspace Package (Mongo Init / Docker Volume)
- **User Experience**: Excellent for Docker, terrible for bare-metal/JAR deployments.
- **Enterprise Safety**: Good. Only `docker-compose` users get the data.
- **Maintainability**: **FAIL**. Maintaining raw MongoDB initialization scripts or BSON dumps is extremely fragile across schema versions.
- **Production Risk**: Low.

### D. Hybrid Approach (Internal Bundle + 1-Click UI)
This approach combines the safety of an import bundle with the UX of automatic initialization.
1. The Sandbox is saved as a standard Export JSON file and embedded in the backend classpath (`src/main/resources/sandbox/banking-sandbox.json`).
2. A single API endpoint is exposed: `POST /api/sandbox/load`.
3. If the frontend detects 0 workspaces on the home screen, the Empty State displays a prominent button: **"Load Reference Banking Sandbox (5-Minute Quickstart)"**.
4. Clicking the button triggers the API, which reads the internal JSON and passes it through the standard internal Import service.

- **User Experience**: Excellent (1 click, highly visible).
- **Enterprise Safety**: Excellent (explicit user action required; no automatic startup pollution).
- **Maintainability**: Excellent (The JSON file is just a standard export. No Java builder code to maintain).
- **Upgrade Risk**: Low (relies on the standard Import service, which must be kept backward-compatible anyway).
- **Production Risk**: Zero.

---

## 3. Recommended Approach

**Recommendation:** Approach D - Hybrid Approach (Internal Bundle + 1-Click UI)

### Rationale
The Hybrid Approach satisfies all constraints perfectly:
1. **Preserves the 5-minute wow**: A single click on an empty dashboard instantly populates the environment. It is arguably a better UX than automatic injection because the user *understands* that they just loaded demo data, rather than being confused by data they didn't create.
2. **Avoids Production Pollution**: The backend never writes data on startup. Production instances will start empty, as they should.
3. **Avoids Operational Complexity**: No special Docker flags, no Mongo dumps, and no Java builder classes. The Sandbox is just a static JSON file bundled in the JAR.

---

## 4. Sprint 3 Implementation Implications

Changing from automatic injection to the Hybrid Approach alters the Sprint 3 deliverables slightly:

**What we DO NOT build:**
- We do not build `SandboxDataInitializer.java`.
- We do not write Java code to construct ISO8583 entities.

**What we DO build:**
1. **JSON Asset**: Create the Sandbox manually in the UI once, export it, and save it to `backend/src/main/resources/sandbox/banking-sandbox.json`.
2. **Backend API**: Create `POST /api/sandbox/load` in a new `SandboxController.java`. This endpoint simply reads the classpath JSON and delegates to the `ImportService`.
3. **Frontend Empty State**: Update the Workspace/Dashboard empty state to include the "Load Reference Banking Sandbox" button.

### Refined Fastest Path to "Wow":
- Minute 1: User runs `docker-compose up -d` and opens the UI.
- Minute 2: User sees the Empty State and clicks **"Load Reference Banking Sandbox"**. 
- Minute 3: Workspace populates. User opens Simulator and sends 0100 Auth.
- Minute 4: User opens Scenario Inspector and observes state transition to `AUTHORIZED`.
- Minute 5: User sends 0420 Reversal and observes state transition to `REVERSED`.

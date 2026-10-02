# Licensing Wording Review

**Date**: 2026-10-02  
**Scope**: jPOS AGPL-3.0 licensing language across project documentation  
**Trigger**: Review of Sprint 1 licensing documentation identified language that could be interpreted as legal guarantees or definitive legal analysis

---

## Summary

Sprint 1 introduced licensing documentation for the first time (`LICENSE`, `THIRD_PARTY_LICENSES.md`). During review, the initial wording in several documents was found to be too assertive — making definitive statements about AGPL compliance that could be interpreted as legal advice or guarantees. This review documents the specific changes made and the rationale.

---

## Documents Updated

| Document | Status |
|---|---|
| [`THIRD_PARTY_LICENSES.md`](THIRD_PARTY_LICENSES.md) | ✅ Updated |
| [`SPRINT_01_COMPLETION_REPORT.md`](SPRINT_01_COMPLETION_REPORT.md) | ✅ Updated |
| [`README.md`](README.md) | ✅ Updated (also corrected factual error) |
| [`frontend/WORKFLOW.md`](frontend/WORKFLOW.md) | ✅ Updated |

---

## Wording Changes

### 1. THIRD_PARTY_LICENSES.md — jPOS inline Note

**Before:**
> Dynamic Mock does not modify jPOS source code. jPOS is included as an unmodified binary dependency. Organizations with concerns about AGPL terms may contact jPOS to discuss a commercial license at https://jpos.org/license.

**After:**
> Dynamic Mock is distributed under the Apache License 2.0. jPOS is distributed separately under AGPL-3.0 and is used by Dynamic Mock as an unmodified runtime dependency. Organizations should independently review the licensing requirements of all third-party dependencies and consult qualified legal counsel if they have questions regarding their specific usage, distribution, hosting, or compliance obligations. For organizations that require commercial licensing terms for jPOS, additional licensing options may be available from the jPOS project at https://jpos.org/license.

**Rationale**: The original wording implied a legal position ("does not modify" → therefore no concern). The revised wording is factual about how jPOS is used and routes compliance questions to qualified legal counsel.

---

### 2. THIRD_PARTY_LICENSES.md — Extended AGPL Section (bottom of file)

**Before (section title):**
> ## Important Note on jPOS and AGPL

**After (section title):**
> ## Note on jPOS and AGPL-3.0

**Before (key paragraph):**
> **Dynamic Mock's position**: Dynamic Mock uses jPOS as an unmodified binary dependency, in the same way an application uses a standard library. Dynamic Mock does not modify jPOS source code. The ISO8583 simulation functionality is implemented in Dynamic Mock's own adapter layer...

**After:**
> Dynamic Mock is distributed under the Apache License 2.0. jPOS is distributed separately under the GNU Affero General Public License v3 (AGPL v3) and is used by Dynamic Mock as an unmodified runtime dependency. Dynamic Mock's ISO8583 simulation is implemented in its own adapter layer... which interacts with jPOS through its public API.

**Before (closing line):**
> Organizations evaluating Dynamic Mock for enterprise deployment should conduct their own legal review regarding AGPL compliance for their specific use case. jPOS also offers commercial licensing for organizations that require non-AGPL terms.

**After:**
> Organizations should independently review the licensing requirements of all third-party dependencies and consult qualified legal counsel if they have questions regarding their specific usage, distribution, hosting, or compliance obligations.
> 
> For organizations that require commercial licensing terms for jPOS, additional licensing options may be available from the jPOS project.
> 
> **This document is informational only and does not constitute legal advice.**

**Rationale**: Removed the "Dynamic Mock's position" framing which implied an official legal stance. Removed the phrase "conduct their own legal review regarding AGPL compliance" (implies a specific compliance obligation exists) and replaced with a general recommendation for independent legal review on any licensing questions.

---

### 3. SPRINT_01_COMPLETION_REPORT.md — jPOS section

**Before:**
> **jPOS AGPL position** (critical for enterprise adoption):
> Dynamic Mock uses jPOS as an **unmodified runtime dependency**. The AGPL copyleft provisions apply only to modifications served over a network. Our adapter layer... uses jPOS via its public API — it does not modify jPOS source. **Organizations can use Dynamic Mock under Apache 2.0.** Those requiring non-AGPL jPOS terms should contact jPOS for a commercial license.

**After:**
> **jPOS licensing** (documented in `THIRD_PARTY_LICENSES.md`):
> 
> Dynamic Mock is distributed under the Apache License 2.0. jPOS is distributed separately under AGPL-3.0 and is used by Dynamic Mock as an unmodified runtime dependency...
> 
> Organizations should independently review the licensing requirements of all third-party dependencies and consult qualified legal counsel...

**Rationale**: Three specific problems with the original:
1. `"The AGPL copyleft provisions apply only to modifications served over a network"` — this is a legal interpretation of the AGPL, not a statement of fact. AGPL's scope is contested in legal contexts.
2. `"Organizations can use Dynamic Mock under Apache 2.0"` — this is a definitive legal guarantee. Whether an organization can use a combined work legally depends on their specific deployment, modification, and distribution scenario — not something a README should assert.
3. `"Our adapter layer... does not modify jPOS source"` — the phrase "Our" implies an authoritative organizational voice making a legal claim.

---

### 4. README.md — License Section

**Before (factual error):**
> AGPL-3.0 (due to jPOS dependency for ISO8583 support)
> For commercial licensing options, contact the maintainers.

**After:**
> Dynamic Mock is distributed under the **[Apache License 2.0](LICENSE)**.
> 
> This project uses third-party open-source libraries. Notable dependency:
> - **jPOS** (`org.jpos:jpos:2.1.9`) is distributed under the GNU Affero General Public License v3 (AGPL-3.0) and is used by Dynamic Mock as an unmodified runtime dependency.
> 
> Organizations should independently review the licensing requirements of all third-party dependencies and consult qualified legal counsel...

**Rationale**: This section contained a **factual error** — it stated the project is AGPL-3.0, when in fact the project itself is Apache 2.0. jPOS is a dependency with its own separate license. The README now correctly distinguishes between the project's own license and the licenses of its dependencies.

---

## Principles Applied

The following principles guided the revised wording:

1. **State facts, not legal interpretations.** Document what the code does (uses jPOS as an unmodified runtime dependency) without asserting what that means legally.

2. **No definitive compliance statements.** Whether a particular deployment complies with a license depends on the specific circumstances, jurisdiction, and applicable law. Documentation should not make definitive compliance claims.

3. **Route legal questions to qualified counsel.** The standard practice for enterprise software is to recommend independent legal review rather than providing in-documentation legal opinions.

4. **Maintain transparency.** The factual information about how jPOS is used is preserved — this is important for enterprise evaluation. Only the interpretive claims are removed.

5. **Apply the "informational only" disclaimer consistently.** The disclaimer at the top of `THIRD_PARTY_LICENSES.md` is reinforced at the bottom where the most legally sensitive content appears.

---

## What Was Not Changed

The following content was intentionally preserved as factual statements of fact (not legal interpretation):

- jPOS version, license identifier (AGPL-3.0), and upstream URL
- Description of how Dynamic Mock uses jPOS (as an unmodified runtime dependency)
- Names of the adapter files (`Iso8583Server.java`, `Q2ServerManager.java`)
- Reference to jPOS commercial licensing availability
- The general description of all other dependencies and their licenses
- `SPRINT_01_DISCOVERY.md` — this is a historical analysis document; the AGPL language there describes the discovery analysis at that time and is appropriate in that context

---

> **This document is informational only and does not constitute legal advice.**

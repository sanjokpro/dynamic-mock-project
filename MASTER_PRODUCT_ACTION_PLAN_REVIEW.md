# Master Product Action Plan — Executive Review

**Date**: 2026-10-01  
**Role**: Independent Strategic Review  
**Document Under Review**: MASTER_PRODUCT_ACTION_PLAN.md  
**Status**: AMENDMENT RECOMMENDATIONS

---

## 1. Where the Current Roadmap is Correct

The MASTER_PRODUCT_ACTION_PLAN.md makes several strategically sound decisions that should be preserved:

1. **ISO8583 as the #1 priority is correct.** The competitive analysis is airtight — no competitor touches ISO8583. The plan to invest in Data Dictionary, Packager Upload, and Message Simulator before anything else is the right call. This must not change.

2. **Rejecting Nested Collections, Protocol Editor Segregation, and Agent Specialization is correct.** These are internal complexity with zero user-facing value. The rejections should stand.

3. **Deferring RBAC, LDAP, SAML, Multi-Tenancy is correct.** These are enterprise checklist items that create zero differentiation. The plan correctly identifies that basic auth is sufficient to unblock deployment without building the full IAM stack prematurely.

4. **The "Ship the moat, then defend it" conclusion is correct.** The product's only strategic asset is the ISO8583 + Stateful Simulation + Self-Hosting combination. Every sprint that doesn't strengthen this combination is a sprint wasted.

5. **Identifying the frontend as the critical weakness is correct.** The backend is mature. The frontend is the bottleneck between "technically works" and "product someone would pay for."

---

## 2. Where the Roadmap Should Change

### RECOMMENDATION A: Open Source Packaging Should Move Earlier — **ACCEPTED WITH MODIFICATION**

**The current plan places Open Source Packaging at Sprint 3.** This is almost correct but contains a subtle sequencing error.

**The problem:** Sprint 1 (ISO8583 Data Dictionary) and Sprint 2 (Auth Wall) are implementation sprints that produce working code. But Sprint 3 (README + CI) is the sprint that tells the world the code exists. Between Sprint 1 and Sprint 3, there is no CI pipeline. This means:

- Sprint 1 code could break Sprint 2 code.
- There is no automated quality gate for the first two sprints of the most critical work.

**The fix is not to move the full README rewrite earlier.** The README cannot be rewritten without screenshots of the ISO8583 Data Dictionary (Sprint 1 dependency is real). However, **CI must ship in Sprint 1, not Sprint 3.**

| Item | Current Sprint | Recommended Sprint | Rationale |
|---|---|---|---|
| GitHub Actions CI (build + test) | Sprint 3 | **Sprint 1** | CI is infrastructure, not packaging. It must exist before any implementation sprint. |
| LICENSE file | Sprint 3 | **Sprint 1** | One file, 5 minutes. Legal foundation must be laid before any code is publicly promoted. |
| README Rewrite | Sprint 3 | Sprint 3 (unchanged) | Correctly depends on Sprint 1 screenshots. |
| CONTRIBUTING.md | Sprint 12 (Community) | **Sprint 3** | Move it up. If the README attracts developers, they need onboarding immediately, not 9 sprints later. |

**Strategic Value**: High. CI prevents the most dangerous phase of the project (rapid implementation) from introducing undetected regressions.  
**Adoption Impact**: Medium. LICENSE is table stakes for enterprise evaluation.  
**Complexity**: Low. GitHub Actions for a Gradle + Next.js build is templated work.  
**Risk**: Near zero.

**Verdict**: ACCEPT — with the nuance that CI and LICENSE move to Sprint 1, while the full README rewrite stays at Sprint 3.

---

### RECOMMENDATION B: Reference Banking Sandbox — **ACCEPTED AS A NEW SPRINT**

This is the strongest recommendation of the three and addresses a genuine blind spot in the current plan.

**The blind spot:** The current roadmap builds features. It does not build *stories*. When a bank evaluates this product, they don't want to see "ISO8583 Data Dictionary" and "Packager Upload" as checkboxes. They want to see:

> *"Here is a pre-configured sandbox. Open it. Send an Authorization (0100). Watch the state change to AUTHORIZED. Send a Reversal (0400). Watch the state change to REVERSED. See the response codes update automatically. This is what your QA team will use instead of connecting to the live switch."*

That is not a feature. That is a *product experience.* And it requires zero new code — only configuration, example data, and documentation.

**What the Reference Banking Sandbox contains:**
1. Pre-configured ISO8583 endpoint with a custom packager.
2. Pre-configured Scenarios: Authorization → Capture → Reversal → Chargeback.
3. Pre-configured Mocks with realistic response fields (PAN masking, response codes, STAN generation).
4. A step-by-step walkthrough document ("Banking Sandbox Tutorial").
5. A `docker compose` profile that loads the sandbox on first boot.

**Where it should be positioned:** Between Sprint 6 (Message Simulator) and Sprint 7 (Scenario State Inspector). The sandbox requires the Data Dictionary (Sprint 1), Auth (Sprint 2), and the Message Simulator (Sprint 6) to be complete. It becomes the *demonstration of everything built so far.*

**Revised position: Sprint 7 (renumbering existing Sprint 7 → Sprint 8).**

**Strategic Value**: **Critical.** This is the difference between "here are features" and "here is how a bank uses this." It is the product's first real sales asset.  
**Business Value**: **Critical.** A downloadable, working banking sandbox that prospects can `docker compose up` and experience is the single most powerful acquisition tool for the target market.  
**Adoption Impact**: **Very High.** Example-driven adoption is 5x more effective than feature-list-driven adoption.  
**Complexity**: **Low.** This is configuration and documentation, not implementation.  
**Risk**: Low. The underlying features are already built or planned.

**Verdict**: ACCEPT — Insert as Sprint 7. It becomes the project's flagship demo artifact and the centerpiece of the README.

---

### RECOMMENDATION C: jPOS Licensing Assessment — **ACCEPTED AS A PRE-LAUNCH GATE**

This recommendation is important, but the current plan already acknowledges it (Risk R10: "jPOS licensing (AGPL) may concern enterprise legal teams"). The question is whether it needs to be elevated from a risk register line item to an explicit sprint deliverable.

**The facts:**
- jPOS is licensed under **AGPL v3**.
- AGPL requires that if you modify jPOS and provide it as a network service, you must release the source code of your modifications.
- Dynamic Mock does not modify jPOS source code — it uses jPOS as a library dependency.
- However, AGPL's "network interaction" clause is broader than LGPL. Enterprise legal teams at banks are notoriously conservative about AGPL.
- The current project has **no LICENSE file at all**, making the licensing posture completely undefined.

**The risk is real but bounded.** Dynamic Mock is itself planned to be open source. If the project uses Apache 2.0, it must clearly document the AGPL dependency and explain that jPOS is an unmodified runtime dependency. This is a documentation and legal assessment task, not an engineering task.

**This should NOT consume a full sprint.** It should be a mandatory pre-launch gate that is completed as part of Sprint 1 (alongside the LICENSE file) or Sprint 3 (alongside the README rewrite).

| Action | Sprint | Effort |
|---|---|---|
| Add LICENSE file (Apache 2.0 recommended) | Sprint 1 | 1 hour |
| Add `THIRD_PARTY_LICENSES.md` documenting jPOS AGPL dependency | Sprint 1 | 2 hours |
| Add a "Licensing" section to the README explaining the relationship | Sprint 3 | 30 minutes |
| If enterprise counsel raises concerns, evaluate jPOS commercial license option | When needed | External |

**Strategic Value**: Medium. Most open-source evaluators check the LICENSE file first.  
**Business Value**: High for banks. Legal compliance is non-negotiable.  
**Adoption Impact**: Could be a silent blocker — enterprises will silently disqualify the product if licensing is ambiguous.  
**Complexity**: Very Low.  
**Risk**: High if ignored; near-zero if documented.

**Verdict**: ACCEPT — as a Sprint 1 deliverable (not a separate sprint). Add LICENSE + THIRD_PARTY_LICENSES.md alongside the CI pipeline work.

---

## 3. Revised Sprint Order

Based on the three recommendations, the revised sprint sequence is:

| Sprint | Goal | Changes from Original |
|---|---|---|
| **Sprint 1** | ISO8583 Data Dictionary + Packager Upload + **CI Pipeline + LICENSE + jPOS Licensing Docs** | Added: CI, LICENSE, THIRD_PARTY_LICENSES.md |
| **Sprint 2** | Authentication Wall | Unchanged |
| **Sprint 3** | README Rewrite + Open Source Packaging + **CONTRIBUTING.md** | Moved CONTRIBUTING.md up from Sprint 12 |
| **Sprint 4** | White Label — Backend | Unchanged |
| **Sprint 5** | White Label — Frontend | Unchanged |
| **Sprint 6** | ISO8583 Message Simulator | Unchanged |
| **Sprint 7** | **Reference Banking Sandbox** *(NEW)* | New sprint. Configuration + documentation, no new code. |
| **Sprint 8** | Scenario State Inspector | Was Sprint 7 |
| **Sprint 9** | Frontend Hardening | Was Sprint 8 |
| **Sprint 10** | Deployment & Operations | Was Sprint 9 |
| **Sprint 11** | ISO8583 Advanced UX | Was Sprint 10 |
| **Sprint 12** | GraphQL & gRPC Designer Polish + Community Ecosystem | Merged original Sprints 11 & 12 |

**Net effect:** One sprint added (Banking Sandbox), one merged (GraphQL/gRPC + Community). Total remains 12 sprints. The first 7 sprints now tell a coherent story: *build the moat (1), lock the door (2), tell the world (3), customize the paint (4-5), show it working (6-7).*

---

## 4. Final Recommendation

The MASTER_PRODUCT_ACTION_PLAN.md is a strong document. It correctly identifies the strategic thesis and makes defensible prioritization decisions. The three amendments improve it without contradicting it:

1. **CI earlier** — protects quality during the most critical implementation phase.
2. **Banking Sandbox** — transforms the product from a feature list into a demonstrable outcome. This is the single highest-ROI addition to the roadmap because it requires no new code but creates the most compelling sales artifact.
3. **jPOS licensing documentation** — removes a silent enterprise blocker with minimal effort.

**The one thing I would challenge most strongly in the original plan:**

The original plan's "Brutally Honest Conclusion" says *"The product needs fewer documents and more shipped, polished features."* This is correct as a general principle, but the Banking Sandbox recommendation reveals a subtlety: **the right document can be more valuable than the right feature.** A pre-configured sandbox with a walkthrough tutorial is "just documentation" — but it demonstrates the product's value in a way that no individual feature can. The product needs fewer *analysis* documents and more *experience* documents.

**The corrected thesis:**

> Ship the moat. Package it as an experience. Then defend it.

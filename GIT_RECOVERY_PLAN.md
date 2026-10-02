# Git Recovery Plan

**Date**: 2026-10-02  
**Branch**: master  
**Status**: PLAN — not yet executed

---

## 1. Current State

### Branch Position
```
origin/master  →  3717252  (last pushed commit)
HEAD           →  7ccc8d4  (2 commits ahead, not pushed)
```

### Unpushed Commits
| SHA | Message | Problem |
|---|---|---|
| `92b9b03` | docs: finalize governance and roadmap baseline | **Contains gradle.zip (128 MB) and gradle9.zip (136 MB), gradle-8.7/, gradle-9.2.1/** |
| `7ccc8d4` | feat: complete sprint 1 ISO8583 productization foundation | Contains `backend/ci_build/` (generated test artifacts, should not be version-controlled) |

### Large Objects in Unpushed History
| File | Size | Commit |
|---|---|---|
| `backend/gradle9.zip` | 129 MB | `92b9b03` |
| `backend/gradle.zip` | 128 MB | `92b9b03` |
| `backend/ci_build/jacoco/test.exec` | 1 MB | `7ccc8d4` |

**Total oversized content: ~257 MB** — GitHub will reject the push (100 MB file size limit).

### Current Working Tree State
- The gradle artifacts are **already staged for deletion** (user ran `git rm` on them)
- `ci_build/` is tracked in `7ccc8d4` and is not yet staged for removal
- `.gitignore` has already been updated (by user) to exclude `backend/gradle-*/`, `backend/*.zip`, `backend/.gradle/`
- `.gitignore` does **not** yet exclude `backend/ci_build/` — this needs to be added

### git ls-files verification (pre-recovery)
```
git ls-files | grep -E "gradle\.zip|gradle9\.zip|gradle-8\.|gradle-9\."
```
Returns: empty ✅ (the gradle directories/zips are already not tracked in the working index — they were staged for deletion by the user)

**HOWEVER** — they **are** stored in commit object `92b9b03` in the git object store. The commits have not been pushed, but the objects exist locally. The git history must be rewritten before pushing.

---

## 2. Root Cause

When the governance commit (`92b9b03`) was made, the `.gitignore` did not contain rules for `backend/gradle-*/` and `backend/*.zip`. The Gradle wrapper bootstrap process downloaded these distributions into the `backend/` directory, and they were swept up by `git add .` or `git add -A`.

The Sprint 1 commit (`7ccc8d4`) also included `backend/ci_build/` because the CI build dir (`-Dorg.gradle.project.buildDir=ci_build`) was not excluded by `.gitignore`. The standard `.gitignore` rule `**/build/` does not match `ci_build/`.

---

## 3. Chosen Recovery Method: Soft Reset + Selective Recommit

### Why not `git commit --amend`?
The artifacts are in commit `92b9b03` (the older of the two), not in `HEAD`. Amend only modifies `HEAD`. Amending would require amending `92b9b03` and then rebasing `7ccc8d4` on top of it — essentially the same as an interactive rebase.

### Why not interactive rebase (`git rebase -i`)?
Interactive rebase is the correct tool but requires editing individual commits in sequence. For exactly 2 unpushed commits, a **soft reset** is simpler, less error-prone, and achieves the same result.

### Chosen: Soft Reset to `origin/master`

**How it works:**
1. `git reset --soft origin/master` — moves `HEAD` back to `origin/master` (3717252) while **preserving all file changes in the staging area**. No work is lost.
2. Unstage the artifacts we never want committed (`backend/gradle*`, `backend/*.zip`, `backend/ci_build/`).
3. Add `backend/ci_build/` to `.gitignore`.
4. Re-stage the legitimate changes.
5. Make two clean commits matching the original intent.

**Why this is safest:**
- No force-push to remote (these are unpushed commits — no one else is affected)
- No data loss — all application code and docs remain in the working tree
- Simple and reversible up until step 5
- The git object store will retain the old objects locally until GC runs, but since they were never pushed, they will never reach GitHub

---

## 4. Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Accidentally re-staging the artifacts | Low | Explicit `git reset HEAD backend/gradle*` before `git add` |
| Losing Sprint 1 application code | Low | Soft reset preserves all files on disk |
| `ci_build/` still being tracked after gitignore update | Low | Explicit `git rm -r --cached backend/ci_build/` before gitignore update |
| Missing a large binary in the re-commit | Medium | Verify with `git rev-list --objects HEAD ^origin/master` before pushing |

---

## 5. Rollback Plan

Before executing, a backup tag is created:
```bash
git tag backup/pre-recovery-$(date +%Y%m%d-%H%M%S)
```

To restore to the pre-recovery state at any point:
```bash
git reset --hard <backup-tag>
# Discard the soft-reset working tree changes (if any were needed):
# git checkout -- .
```

---

## 6. Exact Commands

### Phase 1: Backup
```bash
git tag backup/pre-recovery-20261002
git log --oneline -3  # verify
```

### Phase 2: Update .gitignore to also exclude ci_build
Add to `.gitignore`:
```
backend/ci_build/
```

### Phase 3: Soft Reset
```bash
git reset --soft origin/master
```
This moves HEAD to `3717252` and stages all changes from both commits.

### Phase 4: Unstage Artifacts
```bash
# Unstage gradle distributions (they were committed in 92b9b03)
git restore --staged 'backend/gradle.zip'
git restore --staged 'backend/gradle9.zip'
git restore --staged 'backend/gradle-8.7/'
git restore --staged 'backend/gradle-9.2.1/'

# Unstage ci_build (was committed in 7ccc8d4)
git rm -r --cached backend/ci_build/ 2>/dev/null || git restore --staged backend/ci_build/
```

### Phase 5: Verify Nothing Large Remains Staged
```bash
git diff --cached --stat | sort -t'|' -k2 -rn | head -10
# Verify gradle artifacts not staged:
git diff --cached --name-only | grep -E "gradle.*zip|gradle-[89]|ci_build"
# Should return empty
```

### Phase 6: Re-commit in Two Clean Commits

**Commit 1** (governance baseline — everything except Sprint 1 and ci_build):
```bash
git add -A
git restore --staged 'backend/ci_build/' 2>/dev/null; true  # belt-and-suspenders
git commit -m "docs: finalize governance and roadmap baseline"
```

**Commit 2** (Sprint 1 — application code and docs only):
```bash
git add -A
git commit -m "feat: complete sprint 1 ISO8583 productization foundation"
```

### Phase 7: Final Verification
```bash
# No gradle artifacts tracked
git ls-files | grep -E "gradle\.zip|gradle9\.zip|gradle-8\.|gradle-9\."
# No large objects in unpushed history  
git rev-list --objects HEAD ^origin/master | git cat-file --batch-check='%(objecttype) %(objectname) %(objectsize) %(rest)' | awk '$3 > 1000000 {print $3/1048576"MB", $4}' | sort -rn
# Should show nothing >100MB
```

### Phase 8: Push
```bash
git push origin master
```

---

> **This plan will be executed after review.**

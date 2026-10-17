# AGENTS.md — Veyra Maintainer Guide

> **Read this first.** This file is the single source of truth for any AI agent or human maintainer working on this repository. Follow it exactly — especially the Feature vs Compat rule.

## 1. What Is This Repo?

- **Upstream:** `https://github.com/DrKLO/Telegram` (official Telegram for Android)
- **Fork:** `https://github.com/x1cen/Veyra` (capital `V` — GitHub redirects `veyra` → `Veyra`)
- **Purpose:** Privacy-first Telegram client. Keeps full upstream stability/speed, layers Veyra privacy, anti-censorship, and power-user features on top.
- **Current upstream base:** `13.0.2` / `7175` — commit `f32ffeec763f0bdbf4bec25b52d654f77bff733d` on `origin/master`.
- **Language:** User (owner) speaks Persian. Reply in Persian unless asked otherwise. This document is in English for agent interoperability.

## 2. Branch Model (Rebase Workflow)

This repo uses a **Rebase Workflow** — never merge upstream into the feature branch. This is how all mature Telegram forks (Nekogram, etc.) stay maintainable across Telegram's ~2-3 week release cycle.

```
upstream/master  (DrKLO/Telegram)
      ↓  fetch + ff
    master       — CLEAN mirror of upstream, NEVER edited manually
      ↓  rebase
    veyra        — FEATURE branch, default branch (or intended default)
                 — all Veyra features live here as isolated commits
                 — rebased on top of master on every upstream update
```

| Branch | Purpose | Edit? | Tracks |
|--------|---------|-------|--------|
| `master` | Clean upstream snapshot | **NEVER** edit — only `git fetch upstream` + fast-forward | `upstream/master` |
| `veyra` | All Veyra features | Feature commits only | `master` via rebase |
| `dev`, `main`, `dev-*` | **DELETED** — do not recreate | — | — |

**Global rerere is enabled:** `git config --global rerere.enabled true` — once you resolve a conflict, Git remembers it for next rebase.

## 3. Current State (2026-10-17)

- History was **squashed and rebuilt from scratch** on `master` (`f32ffee`). Old messy history was removed.
- Linear history:

```
f32ffee  update to 13.0.2 (7175)                           [master]
c9633f1  feat(veyra): initial Veyra feature branch          [veyra — 41 new files, ~12k lines]
edbd56d  feat(veyra): wire UI + resources (feature-only)    [veyra — B/C/D hooks, ~48 files]
a0380d6  docs: sync README from dev — veyra default badge   [veyra]
48b39b7  fix(ci): checkout submodules recursive              [veyra — current HEAD]
4f43ed5  fix(docs): restore assets/logo.png + icon.png      [veyra — current HEAD after logo fix]
```

Check with `git -C /root/veyra-work log --oneline veyra -6` and `git branch -vv`.

- **Remote:** `origin` = `https://github.com/x1cen/Veyra.git` (also `x1cen/veyra` works via redirect). `upstream` = `https://github.com/DrKLO/Telegram.git`.
- **Local workdir:** `/root/veyra-work` — branch `veyra` checked out.
- **`.git` is large (~2.9 GB)** due to full history + submodules. Pushes need `http.postBuffer 524288000` and may take minutes — use `timeout 300` and `--progress`.
- **GitHub state after cleanup (2026-10-17):**
  - Remote branches: `master`, `veyra`, `dev` (dev remains because GitHub refuses to delete the current default branch — see §9)
  - Tags `v13.0.2` / `v13.0.2-dev` deleted (local + remote). Only upstream `release-*` tags remain — leave them.
  - Actions history: fully wiped (`total_count: 0` then one new run). Releases: `0`.
  - README refs updated to `branch=veyra` and `Upstream Base v13.0.2`.

## 4. Feature vs Compat Rule — CRITICAL

When porting from any old `dev` branch or cherry-picking Veyra code, **only port feature hunks**. Never port compat/sync hunks. Violating this causes 60+ conflicts on every upstream bump.

**FEATURE (port this):**
- Any hunk where added lines contain: `VeyraConfig`, `org.veyra`, `Aether`, `Veyra`, `HiddenContentManager`, `MonetHelper`, `ProxyDiscovery`, `ghost`, `antiDelete`, `editHistory`, `reactionHistory`
- New files under `org.veyra/**`, `org/veyra/**`, `Veyra*Activity.java`, `MonetHelper.java`, `AetherActivity.java`, etc.

**COMPAT (NEVER port):**
- `TMessagesProj/jni/third_party/boringssl/**` (16k files)
- `TLRPC.java` `LAYER` bump, `TL_ephemeral.java`, `ApiScheme.cpp`, `tl/` folder
- `welcome` / `ephemeral` wiring that is not Veyra-gated
- `CMakeLists.txt` / `TgNetWrapper.cpp` drift
- `BuildVars.java` hardcoding `7175` / `DEBUG = false` — keep upstream `BuildConfig.*` wiring
- `build.gradle` downgrades (compileSdk, cmake, NDK)
- `AndroidManifest.xml` permission removals
- Import reorders, `PackageInfo`/`Choreographer`/`media3` churn without Veyra signal

**How to enforce:** `git diff master..dev -- <file> |` keep only hunks where `+` lines match feature signals. Verify with `grep -c Veyra` after apply. Document decisions in `docs/FEATURE_VS_COMPAT.md`.

Reference: `docs/FEATURE_VS_COMPAT.md` and `/tmp/veyra-groupB`, `/tmp/veyra-groupC`, `/tmp/veyra-groupD` filtered patches (if still present).

## 5. What Is on `veyra` (Feature Inventory)

### 5.1 New modules (41 files, ~12k lines)
- `TMessagesProj/src/main/java/org/veyra/VeyraConfig.java` — central feature toggles
- `TMessagesProj/src/main/java/org/veyra/*Activity.java` (×18) — settings screens
- `TMessagesProj/src/main/java/org/veyra/client/**` — anti-delete / ghost / Aether / proxy logic
- `TMessagesProj/src/main/java/org/telegram/helpers/MonetHelper.java`

### 5.2 Core hooks (Group A)
- `MessagesController.java` (27 Veyra sites) — ghost, anti-delete, edit history
- `MessagesStorage.java` (19) — init/clear/history
- `MessageObject.java` — disableBigEmoji, edit flag
- `ConnectionsManager.java` — ghost in `sendRequestInternal` via `VeyraSecurity.getVendor()` (kept `media3`, NOT downgraded to `exoplayer2`)
- `SendMessagesHelper.java` — developer guard

### 5.3 UI hooks (Groups B/C — 45 files)
- `ApplicationLoader`, `LocaleController`, `AndroidUtilities`, `browser/Browser`, `SecretChatHelper`, `FileRefController`, `MessagePreviewParams`, `DialogsActivity`, `ProfileActivity`, `MainTabsActivity`
- `ChatActivity`, `Theme`, `StoriesController`, `PasscodeView`, `DialogCell`, `ChatAvatarContainer`, `AlertsCreator`, `UndoView`, `ChatActivityEnterView`, `EmojiView`, `InstantCamera`, `ProxyListActivity`, `ProxyDiscoveryActivity`, `VoIPHelper`, `AetherActivity`, `AetherLogsActivity`, `LaunchActivity`, etc.

### 5.4 Wiring (Group D — feature-only)
- `BuildVars.java` — added `BUILD_VENDOR/BUILD_VEYRA/fuLabel/PIN_*` + `DEVELOPER_USER_ID` branding, kept `BUILD_VERSION_STRING = BuildConfig.BUILD_VERSION_STRING` and `DEBUG_VERSION = BuildConfig.*` (no hardcode)
- `strings.xml` — appended 226 `Veyra*` strings, **kept** upstream welcome strings (`EditAdminUserManageWelcomeMessages` etc.), kept `AppName=Telegram`
- `AndroidManifest.xml` — added `org.veyra.client.aether.AetherService` only, kept all upstream permissions
- `TMessagesProj/build.gradle` — added `commons-lang3:3.20.0` only, kept `compileSdk 36`, `cmake 3.22.1`
- `assets/logo.png` (523 KB) + `assets/icon.png` (239 KB) — required for README header

### 5.5 Hook markers
Future hooks should be wrapped with:
```java
// CUSTOM HOOK START — Veyra: <feature name>
// ... code ...
// CUSTOM HOOK END
```
Makes `git diff` and `grep` trivial.

## 6. Workflows & Scripts

### `scripts/veyra-rebase.sh` (49 lines, executable)
One-command updater:
```bash
./scripts/veyra-rebase.sh
# does: git fetch upstream; checkout master; ff to upstream/master; checkout veyra; rebase master; push if clean
```
Updated to use `master`/`veyra` names (was `master_13.0.2`/`veyra-custom`). If conflicts, it stops — resolve, `git add .`, `git rebase --continue`, then `git push --force-with-lease origin veyra`.

### `.github/workflows/veyra-auto-rebase.yml`
Daily at 06:00 Asia/Tehran — fetches upstream, rebases if clean, pushes, opens an Issue if conflict.

### `.github/workflows/veyra-build.yml`
Manual `workflow_dispatch` on `veyra`:
- Inputs: `veyra-version` (default `13.0.2`), `arch` (`arm64-v8a` default for veyra, `all` for stable), `veyra-changes`, `multi-pass`, `prerelease`
- **Checkout MUST be:** `uses: actions/checkout@v6` with `with: { submodules: recursive, fetch-depth: 1 }` — without this `TMessagesProj_Modules/media/core_settings.gradle` is missing and build fails with `Could not read script .../media/core_settings.gradle`.
- Builds `gradle :TMessagesProj_AppStandalone:assembleAfatStandalone --parallel --build-cache --max-workers=4`
- Outputs `Veyra.<version>-<abi>.apk` + `SHA256SUMS.txt` as GitHub Release

## 7. Submodules & Build

- `.gitmodules` lists ~19 submodules (`boringssl`, `ffmpeg`, `libvpx`, `dav1d`, `tgcalls`, `td`, `TMessagesProj_Modules/media` at `https://github.com/Arseny271/media.git`, etc.)
- `TMessagesProj_Modules/media` is **required** — `settings.gradle` line 18: `apply from: file("TMessagesProj_Modules/media/core_settings.gradle")`
- CI and local clone must init submodules: `git submodule update --init --recursive` or `checkout` with `submodules: recursive`.
- `gradle.properties` + `TMessagesProj/config/release.keystore` are injected via secrets in CI (`APP_ID/APP_HASH`, `VEYRA_KEYSTORE_BASE64`, etc.) — do not commit real values.

## 8. How to Update Upstream (Agent Instructions)

When user says "update", "rebase", "sync with latest Telegram":

1. `git -C /root/veyra-work fetch upstream`
2. `git -C /root/veyra-work checkout master && git merge upstream/master --ff-only` (or `git rebase upstream/master` if linear history required)
3. `git -C /root/veyra-work checkout veyra && git rebase master`
   - If conflicts: resolve only feature hunks, keep upstream for compat files (`BuildVars`, `TLRPC LAYER`, `boringssl`, etc.). Use `git status`, `git diff`, `grep Veyra` to verify.
   - `git add <resolved>` → `git rebase --continue`
4. `git -C /root/veyra-work push origin master veyra --force-with-lease` (use `--force` if lease fails after history rewrite; ensure `http.postBuffer` is set)
5. Optionally trigger build: `gh workflow run veyra-build.yml --ref veyra` or via API dispatch.

Never edit `master` manually. Never merge `master` into `veyra` — always rebase.

## 9. Known Manual Step — Default Branch

`veyra` is intended to be the default branch. As of 2026-10-17 the GitHub API returns `403 Resource not accessible by personal access token` when trying `PATCH /repos/x1cen/Veyra {default_branch: veyra}`.

- Token: `github_pat_11B4OMMWQ...` (fine-grained PAT, user `x1cen` id `253544026`, expires `2026-11-15`) stored in `/tmp/gh_cred` as `https://oauth2:<PAT>@github.com` plus raw PAT on second line with email `253544026+x1cen@users.noreply.github.com`. It has `contents:write` but **lacks `administration:write`** for repo settings.
- **Manual fix required (once):** GitHub web → `x1cen/Veyra` → `Settings` → `General` → `Default branch` → switch `dev` → `veyra` → `Update`.
- After that, delete remote `dev`: `git push origin --delete dev` (currently blocked because `dev` is still default).
- `main` already deleted. After default switch, no extra branches should remain except `master` + `veyra`.

## 10. Cleanup Already Done (Do Not Repeat)

- History squashed to linear `master → veyra` (old graft/shallow cleaned)
- Workflow runs wiped: `total_count` 253 → 0 (then one new run)
- Releases wiped: 2 releases + 2 tags (`v13.0.2`, `v13.0.2-dev`) deleted
- `main` branch deleted remotely; `dev` local branches deleted (`dev`, `dev-backup-broken`, `dev-welcome-broken`), `dev` remote pending default switch
- README synced from old `dev` with `branch=veyra` + `Upstream v13.0.2` badge
- `assets/logo.png` + `assets/icon.png` restored (README was broken after rebase)

## 11. Quick Reference Commands

```bash
# status
git -C /root/veyra-work status --short
git -C /root/veyra-work branch -vv
git -C /root/veyra-work log --oneline -6 --graph --all
git -C /root/veyra-work ls-remote origin | grep heads

# update upstream
git -C /root/veyra-work fetch upstream && git -C /root/veyra-work fetch origin
./scripts/veyra-rebase.sh   # or manual steps in §8

# build locally (requires NDK 27.2.12479018, CMake 3.22.1, JDK 21)
git submodule update --init --recursive
gradle :TMessagesProj_AppStandalone:assembleAfatStandalone --parallel --build-cache

# trigger CI build
TOKEN=$(cat /tmp/gh_cred | head -n1 | sed 's|.*:||; s|@github.*||')
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  https://api.github.com/repos/x1cen/Veyra/actions/workflows/366056272/dispatches \
  -d '{"ref":"veyra","inputs":{"veyra-version":"13.0.2","arch":"arm64-v8a"}}'

# monitor build
curl -s -H "Authorization: Bearer $TOKEN" https://api.github.com/repos/x1cen/Veyra/actions/runs?per_page=3 \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('total_count')); [print(r['id'], r['status'], r.get('conclusion'), r['head_branch']) for r in d.get('workflow_runs',[])[:5]]"

# cleanup actions (wipe history)
for id in $(curl -s -H "Authorization: Bearer $TOKEN" "https://api.github.com/repos/x1cen/Veyra/actions/runs?per_page=100" | python3 -c "import json,sys; d=json.load(sys.stdin); print(' '.join(str(r['id']) for r in d.get('workflow_runs',[])))"); do curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "https://api.github.com/repos/x1cen/Veyra/actions/runs/$id" > /dev/null; done
```

## 12. Agent Rules

- **Before any git push, confirm with user** unless they explicitly said "push".
- **Never push compat downgrades.** If unsure whether a hunk is feature or compat, ask or leave it out.
- **Keep `scripts/veyra-rebase.sh` and both workflows in sync** when renaming branches.
- **README assets:** `assets/logo.png` must exist — `README.md` line 2 references `assets/logo.png`.
- **Submodules:** Never commit `TMessagesProj_Modules/media` as regular files — it must stay a gitlink (`160000`).
- **Secrets:** Never log `APP_ID`, `APP_HASH`, `VEYRA_KEYSTORE_BASE64`. They come from `secrets.*` in CI.
- **Language:** User writes Persian — answer in Persian; code/comments in English.

---
*Last updated: 2026-10-17 — veyra HEAD `4f43ed5` on master `f32ffee` (13.0.2). Maintainer: x1cen.*

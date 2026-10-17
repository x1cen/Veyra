# AGENTS.md — Veyra Maintainer Guide

> **Read this first.** This file is the single source of truth for any AI agent or human maintainer working on this repository. Follow it exactly — especially the Dev/Veyra rule and the Feature vs Compat rule.

## 1. What Is This Repo?

- **Upstream:** `https://github.com/DrKLO/Telegram` (official Telegram for Android)
- **Fork:** `https://github.com/x1cen/Veyra` (capital `V` — GitHub redirects `veyra` → `Veyra`)
- **Purpose:** Privacy-first Telegram client. Keeps full upstream stability/speed, layers Veyra privacy, anti-censorship, and power-user features on top.
- **Language:** User speaks Persian — reply in Persian unless asked otherwise. This document is in English for agent interoperability.

## 2. Branch Model (Rebase Workflow)

This repo uses a **Rebase Workflow** — never merge upstream into the feature branch. This is how all mature Telegram forks stay maintainable across Telegram's ~2-3 week release cycle.

```
upstream/master  (DrKLO/Telegram)
      ↓  fetch + ff
    master       — CLEAN mirror of upstream — NEVER edited manually
      ↓  rebase
    veyra        — FEATURE branch — all Veyra features live here
      ↑  sync after testing
    dev          — TESTING branch — branched from veyra, for experiments
```

| Branch | Purpose | Edit? | Tracks |
|--------|---------|-------|--------|
| `master` | Clean upstream snapshot | **NEVER** edit — only `git fetch upstream` + fast-forward | `upstream/master` |
| `veyra` | Stable feature branch (default) | Only via approved sync from `dev` | `master` via rebase |
| `dev` | Testing / development | All new work goes here | `veyra` |

**Global rerere is enabled:** `git config --global rerere.enabled true` — once you resolve a conflict, Git remembers it.

## 3. Working Rule — Dev First, Veyra After Approval

- **All new changes, experiments, and fixes MUST be done on `dev`.** Never commit directly to `veyra`.
- Test on `dev` — trigger CI build on `dev` to verify the APK compiles and features work.
- **Only after the user explicitly approves** (`"sync to veyra"`, `"روی veyra بزن"`, `"تایید"` etc.) sync `dev` to `veyra`:
  ```bash
  git checkout veyra
  git merge dev --ff-only        # if dev is ahead of veyra
  # or, if veyra diverged: git rebase dev / cherry-pick
  git push origin veyra
  ```
- If `dev` was rebased, use `git checkout veyra && git reset --hard dev && git push --force-with-lease origin veyra` only with user approval.
- `master` is never touched except by upstream sync.

## 4. Feature vs Compat Rule — CRITICAL

When porting any code (from upstream, from old branches, or cherry-picking), **only port feature hunks**. Never port compat/sync hunks. Violating this causes dozens of conflicts on every upstream bump.

**FEATURE (port this):**
- Any hunk where added lines contain: `VeyraConfig`, `org.veyra`, `Aether`, `Veyra`, `HiddenContentManager`, `MonetHelper`, `ProxyDiscovery`, `ghost`, `antiDelete`, `editHistory`, `reactionHistory`
- New files under `org.veyra/**`, `Veyra*Activity.java`, `MonetHelper.java`, `AetherActivity.java`, etc.

**COMPAT (NEVER port):**
- `TMessagesProj/jni/third_party/boringssl/**`
- `TLRPC.java` `LAYER` bump, `TL_ephemeral.java`, `ApiScheme.cpp`, `tl/` folder
- `welcome` / `ephemeral` wiring that is not Veyra-gated
- `CMakeLists.txt` / `TgNetWrapper.cpp` drift unrelated to features
- `BuildVars.java` hardcoding `BUILD_VERSION` / `DEBUG = false` — keep upstream `BuildConfig.*` wiring
- `build.gradle` downgrades (compileSdk, cmake, NDK)
- `AndroidManifest.xml` permission removals
- Import reorders, `PackageInfo`/`Choreographer`/`media3` churn without Veyra signal

**How to enforce:** `git diff master..dev -- <file> |` keep only hunks where `+` lines match feature signals. Verify with `grep -c Veyra` after apply.

## 5. Feature Inventory (what lives on `veyra` / `dev`)

### New modules
- `TMessagesProj/src/main/java/org/veyra/VeyraConfig.java` — central feature toggles
- `TMessagesProj/src/main/java/org/veyra/*Activity.java` — settings screens
- `TMessagesProj/src/main/java/org/veyra/client/**` — anti-delete / ghost / Aether / proxy
- `TMessagesProj/src/main/java/org/telegram/helpers/MonetHelper.java`

### Core hooks
- `MessagesController.java` — ghost, anti-delete, edit history
- `MessagesStorage.java` — init/clear/history
- `MessageObject.java` — disableBigEmoji, edit flag
- `ConnectionsManager.java` — ghost via `VeyraSecurity.getVendor()` (keep `media3`, not `exoplayer2`)
- `SendMessagesHelper.java` — developer guard

### UI hooks
- `ApplicationLoader`, `LocaleController`, `AndroidUtilities`, `browser/Browser`, `SecretChatHelper`, `FileRefController`, `MessagePreviewParams`, `DialogsActivity`, `ProfileActivity`, `MainTabsActivity`
- `ChatActivity`, `Theme`, `StoriesController`, `PasscodeView`, `DialogCell`, `ChatAvatarContainer`, `AlertsCreator`, `UndoView`, `ChatActivityEnterView`, `EmojiView`, `InstantCamera`, `ProxyListActivity`, `ProxyDiscoveryActivity`, `VoIPHelper`, `AetherActivity`, `AetherLogsActivity`, `LaunchActivity`, etc.

### Wiring (feature-only)
- `BuildVars.java` — Veyra branding fields only, keep `BuildConfig.*` for version/debug
- `strings.xml` — appended `Veyra*` strings, keep all upstream strings including welcome
- `AndroidManifest.xml` — added `AetherService` only, keep all upstream permissions
- `TMessagesProj/build.gradle` — added `commons-lang3` only, keep upstream SDK/cmake versions
- `assets/logo.png` + `assets/icon.png` — required for README header (`README.md` line 2 references `assets/logo.png`)

### Hook markers
Wrap future hooks with:
```java
// CUSTOM HOOK START — Veyra: <feature name>
// ... code ...
// CUSTOM HOOK END
```

## 6. Workflows & Scripts

### `scripts/veyra-rebase.sh`
One-command updater: `git fetch upstream`, fast-forward `master` to `upstream/master`, rebase `veyra` on `master`, push if clean. If conflicts — resolve, `git add .`, `git rebase --continue`, then `git push --force-with-lease origin veyra`.

### `.github/workflows/veyra-auto-rebase.yml`
Daily auto-check (06:00 Asia/Tehran) — fetches upstream, rebases if clean, pushes, opens an Issue on conflict.

### `.github/workflows/veyra-build.yml`
Manual `workflow_dispatch` — works on any branch (`veyra` or `dev`):
- Inputs: `veyra-version`, `arch` (`arm64-v8a` default, `all` for universal), `veyra-changes`, `prerelease`
- **Checkout must be** `actions/checkout@v6` with `with: { submodules: recursive, fetch-depth: 1 }` — without it `TMessagesProj_Modules/media/core_settings.gradle` is missing and build fails.
- Builds `gradle :TMessagesProj_AppStandalone:assembleAfatStandalone --parallel --build-cache`

## 7. Submodules & Build

- `.gitmodules` lists submodules (`boringssl`, `ffmpeg`, `libvpx`, `dav1d`, `tgcalls`, `td`, `TMessagesProj_Modules/media` at `https://github.com/Arseny271/media.git`, etc.).
- `TMessagesProj_Modules/media` is **required** — `settings.gradle` applies `TMessagesProj_Modules/media/core_settings.gradle`. CI and local clone must use `submodules: recursive`.
- `gradle.properties` + `TMessagesProj/config/release.keystore` are injected via secrets in CI — do not commit real values.

## 8. How to Update Upstream

When user says "update", "rebase", "sync with latest Telegram":

1. `git fetch upstream`
2. `git checkout master && git merge upstream/master --ff-only`
3. `git checkout dev && git rebase master`  — resolve conflicts (feature-only, keep upstream for compat files)
   `git add <resolved>` → `git rebase --continue`
4. Push `dev`, trigger CI build on `dev`, verify.
5. Only after user approval: sync to `veyra` (see §3).

Never edit `master` manually. Never merge `master` into `veyra`/`dev` — always rebase.

## 9. Version Notes

- Build version comes from the workflow input `veyra-version` (or current upstream tag) — do not hardcode a version number in this guide. Read it from the latest upstream tag or `BuildConfig` at build time.
- This guide itself must not contain hardcoded build numbers or commit SHAs that will go stale — keep it generic.

## 10. Quick Reference Commands

```bash
# status
git status --short; git branch -vv; git log --oneline --graph --all -8

# work on dev
git checkout dev
# ... make changes ...
git add -A && git commit -m "feat: ..."
git push origin dev
# trigger CI on dev via workflow_dispatch (ref=dev)

# sync approved dev → veyra
git checkout veyra && git merge dev --ff-only && git push origin veyra

# update upstream (on dev first)
git fetch upstream; git checkout master; git merge upstream/master --ff-only
git checkout dev; git rebase master; # resolve if needed
git push origin master dev
```

## 11. Agent Rules

- **All new work on `dev`** — never commit directly to `veyra` without user approval.
- **Never push compat downgrades.** If unsure whether a hunk is feature or compat, leave it out and ask.
- **Before any push, confirm with user** unless explicitly told to push.
- **Keep `scripts/veyra-rebase.sh` and both workflows in sync** when renaming branches.
- **README assets:** `assets/logo.png` must exist — `README.md` references it.
- **Submodules:** Never commit `TMessagesProj_Modules/media` as regular files — it must stay a gitlink (`160000`).
- **Secrets:** Never log `APP_ID`, `APP_HASH`, `VEYRA_KEYSTORE_BASE64`.
- **Language:** User writes Persian — answer in Persian; code/comments in English.

<p align="center">
  <img src="assets/logo.png" width="220" alt="Veyra Logo" />
</p>

<h1 align="center">🛡️ Veyra</h1>

<p align="center">
  <b>Privacy-first Telegram client for Android</b><br>
  Built on the official upstream Telegram codebase, with extra privacy controls and power-user tools layered on top
</p>

<p align="center">
  <a href="https://github.com/x1cen/Veyra/actions/workflows/veyra-build.yml"><img src="https://github.com/x1cen/Veyra/actions/workflows/veyra-build.yml/badge.svg?branch=main" alt="Build Status"></a>
  <a href="https://github.com/x1cen/Veyra/releases/latest"><img src="https://img.shields.io/github/v/release/x1cen/Veyra?label=Stable%20Release" alt="Latest Release"></a>
  <a href="https://github.com/DrKLO/Telegram"><img src="https://img.shields.io/badge/Upstream%20Base-v12.9.2%2B-2481CC.svg" alt="Upstream Base"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--2.0-blue.svg" alt="License"></a>
</p>

---

## Overview

Veyra is a privacy-first Android client built on the official upstream Telegram codebase. It keeps the stability and speed people already expect from stock Telegram, then adds privacy protections, power-user utilities, full ad removal, anti-censorship networking, and its own visual identity.

---

## Key Features

### 🛡️ Privacy and Stealth

* Online Status Visibility Control: choose whether to broadcast your online status, hide your presence completely, go offline automatically after sending a message, or stay always online.
* Granular Ghost Mode: separate toggles for each privacy signal, online status, typing and recording indicators, read receipts, voice and video note playback status, and story view visibility. All enabled by default.
* Mark as Read on Reply: messages stay marked unread until you actually reply to the conversation.
* Anti-Delete Message Retention: keeps messages and media that conversation partners revoke. Deleted incoming messages get a dimmed appearance with a DELETED label. Your own sent messages are not affected. The Anti-Delete settings screen has per-chat-type toggles (private, groups, channels, bots) plus a Clear All button to wipe every retained record at once.
* Message Edit History: logs previous versions of edited messages automatically, with per-chat-type toggles matching Anti-Delete. Set a per-message limit (5 to 100 versions), choose what happens on overflow (drop the oldest entry or stop logging), and view the full edit timeline from the History button in the message context menu.
* Hide Typing Indicator: stops "typing..." and audio-recording indicators from reaching the other side.
* Block Incoming Secret Chats: automatically declines secret chat requests from other users.
* Hide Proxy Connecting Status: hides the "Connecting to proxy..." line everywhere it would normally show.
* Variable-Length Security PINs (6 to 24 digits): flexible PIN length for the Main Passcode, Duress Code, and Settings Lock.
* Accurate Edited Marker: a message is marked edited only when its text actually changed, reactions never trigger the badge.
* Emergency Duress Code: a secondary PIN that silently wipes all local app data and accounts the moment it's entered.
* Dedicated Settings Lock: protect the Veyra settings screen with its own PIN, separate from the app passcode.

### 👥 Contact and Profile Intelligence

* Mutual Contact Indicator: a blue dot on the bottom-left of a user's avatar in the chat list shows when they also have your number saved.
* Mutual Contact Row in Profile: a row on the user's profile page spells out mutual contact status explicitly.
* Telegram Premium Status Row: shows whether a user currently has an active Premium subscription, visible on every private user's profile.
* Group and Channel Permissions View: non-admin members can open the group or channel info panel and see the full set of member permissions (sending messages, media, polls, and so on) in read-only form, instead of hitting a dead end.

### 🗂️ Media Storage

* Structured Media Organisation: saved media lands in dedicated subfolders under `Documents/Telegram/`, Video Notes, Voice Notes, Videos, Photos, Audio, Documents, and Animations, kept separate from the regular gallery.
* Download-State Gated Save Options: "Save to gallery" and "Save to downloads" only appear once a file has fully downloaded, so you can't trigger an empty-save error.
* Video Message Quality Enhancement: round video messages record at 640x640 with a 2.8 Mbps bitrate for a noticeably sharper result.
* Video Message Watermark Removal: the Telegram logo overlay is not rendered onto recorded or saved round video messages.

### 💬 Composing and Chat Actions

* Hide Content: long-press any message to hide it locally. Hidden messages disappear from the chat view without being deleted. Tap the Chats tab 7 times in quick succession to bring everything back.
* Delete Veyra Cache on Message Delete: manually deleting a message that Anti-Delete had retained also wipes the matching Veyra records (anti-delete entry and edit history) for that message.
* Deleted Account Chat Handling: when a conversation partner deletes their Telegram account, the input bar is replaced with a "Deleted Chat" button. Tapping it asks for confirmation, then cleans up the dialog along with its Veyra anti-delete records and edit history in one pass.
* Mention by Name: insert a user's real display name instead of their `@username`.
* Hide Send As Button: hides the channel and profile identity switcher.
* Disable Quick Reaction Double-Tap: stops accidental emoji reactions from double-taps.
* Timestamps with Seconds: message times show the exact second, not just hour and minute.
* Strip Bot Link Trackers: strips `utm_*`, `fbclid`, and `gclid` parameters from links automatically.
* Anonymous Forward by Default: forwarded messages hide the sender name and quote by default.
* Disable Large Emoji Rendering: single emoji render at normal text size instead of oversized.
* Jump to First Message: jump straight to a chat's first message from the 3-dot menu.
* Copy Dialog ID: quickly copy a chat's Telegram Dialog or Peer ID.

### 🖼️ Media and Camera

* Rear Camera for Video Messages: round video messages default to the rear camera.
* Send Typed Text with Stickers and GIFs: typed captions are kept when you send a sticker or GIF alongside them.
* Keep Original Filename on Download: downloaded documents keep their original filename.

### 📋 Chat List

* Mutual Contact Dot: blue indicator dot on the avatar for mutual contacts, same as above.
* Disable Global Search: stops queries from reaching public Telegram channels, bots, and users.
* Disable Media Thumbnails in Dialogs: hides video and photo previews in the chat list.
* Colored Last-Seen Indicator Dots: live dots on avatars (yellow under 15 minutes, orange under 30, red under 60).
* Dialog ordering stays chronological by default, newest activity always surfaces to the top. Pinning unread or unmuted chats above the rest is available as an opt-in setting and is off by default.

### ⚙️ Power Controls and Interaction

* Message Details Inspector: inspect message ID, DC, sender ID, exact media byte size, and timestamps, exportable as JSON.
* Bulk Message Operations: forward multiple messages to Saved Messages, or unpin several pinned messages, in one action.
* Rich Inline Button Actions: long-press a bot's inline buttons to copy their callback data, query, ID, or URL.
* Call Confirmation Dialog: asks for confirmation before placing a VoIP call.
* External Link Confirmation: asks for confirmation before opening an external URL.
* Automatic Tracker Stripper: cleans `utm_*`, `fbclid`, `gclid`, `si`, and `igsh` automatically.
* Skip 5-Second Undo Toast: delete, clear, and archive actions happen instantly instead of waiting on the undo window.
* Disable Vibration: a single switch for all haptic feedback.
* Disable Link Previews by Default: stops the automatic webpage lookup while composing a message.

### 🛠️ Group Administration

* Delete All Messages in Group: wipe an entire group's history after a confirmation prompt.
* Upgrade Group to Supergroup: convert a basic group into a supergroup.
* Auto-Delete Timer: set up automatic message deletion for private chats and groups.

### 📱 QR Code Integration

* Login via QR Code: log in by scanning a generated QR code.
* Direct QR Login Confirmation: scan and approve login QR codes in one step.
* Share via QR Code: export sticker packs, channels, groups, and proxies as QR codes.

### 🌐 Network and Anti-Censorship

* Native WEB Proxy Tunnel: full web proxy transport through an isolated background Android System WebView, letting it bypass DPI restrictions without native overhead.

### 🔒 Security Layer

* Screen Capture Blocking: a toggle in Privacy Settings (off by default) that applies `FLAG_SECURE` to app windows, blocking screenshots and recent-apps thumbnails from leaking conversation content.
* Emergency Wipe System (7-tap logo trigger): tapping the Telegram logo 7 times rapidly opens the emergency protocol menu, with confirmation required before either action runs:
  * Local Emergency Wipe: immediately and securely purges all local databases, cached data, downloaded media, and the `Documents/Telegram` folder, then logs the app out.
  * Full Remote Wipe and Account Self-Destruction: checks network status, wipes local media and documents, leaves every joined group and channel, runs two-way chat deletion across all private contacts, and permanently deletes the Telegram account. In offline mode, it prompts you to do a local wipe instead or retry once back online.
* Root and Magisk Compatibility: works normally on Magisk-rooted Android without false-positive lockouts, while keeping active memory and session integrity checks in place.
* Diagnostic and Crash Logging: catches unhandled exceptions automatically and saves crash traces plus device metrics to `Documents/Telegram/crash_log.txt` and `Documents/Telegram/Logs/veyra_crashes.log`.
* AES-256-GCM Encryption API: a built-in encryption layer for sensitive local data (session tokens, preferences) using AES-256-GCM with randomised 12-byte IVs. Everything Veyra stores locally is encrypted this way except the user-facing `Documents` folder, which stays as plain files by design so you can access what you save from outside the app.
* Session Token Integrity: HMAC-SHA256 validation protects session tokens against tampering.

### 🙈 Stealth and Content Management

* Hidden Chats and Messages: hide an entire conversation from the chat list, or hide individual messages. Restore everything hidden with 7 quick taps on the Chats tab.

### 🎨 UI and Regional Features

* Native Persian Solar Calendar: dates and numbers switch to the Persian Solar Hijri (Shamsi) calendar when the `fa` locale is active, with full Farsi translations throughout the app.
* Telegram ID and DC in Profiles: tap to copy a user's ID or DC straight from their profile.
* Permanent Ad Suppression: sponsored posts and search ads stay off, no Premium required.
* Unrestricted Forward and Copy: bypasses forward and copy restrictions in protected chats.
* Expanded Limits: up to 100 pinned chats, 500 favourite stickers, 1000 GIFs, and 30 folders.
* Settings JSON Backup and Restore: export your Veyra configuration as JSON and restore it later.

---

## Feature Comparison Matrix

| Capability | Official Telegram | Standard Forks | Veyra |
| :--- | :---: | :---: | :---: |
| Upstream Telegram Architecture | Yes | Outdated / Partial | Latest Stable |
| Online Status Toggle (Show / Hide) | Privacy Settings Only | Partial | Instant Client Toggle |
| Granular Ghost Mode (5 signals) | No | No | Full |
| Mark as Read on Reply | No | No | Built-in |
| Anti-Delete Message Cache | No | Modded | Per-chat-type, with Clear All |
| Message Edit History Log | No | No | Built-in (5 to 100 edits, per-chat-type, viewer) |
| Mutual Contact Indicator | No | No | Blue dot + profile row |
| Telegram Premium Status in Profile | No | No | Active / Not active row |
| Group Permissions View for Non-Admins | No | No | Read-only permissions panel |
| Structured Media Storage | No | Partial | 7 typed subfolders |
| Video Message HQ Recording | 384p / 1 Mbps | No | 640p / 2.8 Mbps |
| Screen Capture Blocking | No | No | FLAG_SECURE on all windows |
| Hook/Debugger Detection | No | No | Frida + Xposed + wipe |
| AES-256-GCM Data Encryption | No | No | Built-in API |
| Message Details & JSON Export | No | Third-Party | Built-in Native |
| Bot Button Long-Press Menu | No | Limited | Full (Callback/ID/Query/URL) |
| Native Solar Hijri (Shamsi) Calendar | No | Third-party pack | Fully Native, with Farsi UI |
| URL Tracker Removal (`cleanUrl`) | No | No | Automatic |
| Web Proxy (WebView Tunnel) | Experimental | No | Fully Integrated |
| Call & Link Action Confirmation | No | No | Built-in Modal Prompts |
| Complete Channel Ad Suppression | No (Premium Only) | Partial | Built-in Permanent Free |
| Unrestricted Forward & Copy | Blocked | Modded | Seamless Bypass |
| Unlocked Pins & Stickers | 5 pins / 5 fave | Variable | 100 pins / 500 stickers |

---

## Architecture and Codebase Structure

The Veyra-specific code is kept modular so it doesn't entangle with upstream Telegram internals:

* `org.telegram.messenger.VeyraConfig`: persistent configuration controller for all privacy switches and feature flags, including per-chat-type rules for Anti-Delete and Edit History.
* `org.telegram.ui.VeyraSettingsActivity`: the main Veyra settings hub, linking out to Ghost Mode, Anti-Delete, and Edit History sub-screens.
* `org.veyra.client.VeyraEmergencyHandler`: emergency protocol executor for the local wipe and full remote self-destruction flows.
* `org.veyra.client.VeyraCrashHandler`: diagnostic and crash reporter that logs to public storage.
* `org.veyra.client.VeyraSecurityGuard`: the security layer, FLAG_SECURE, hook detection, AES-256-GCM, HMAC session validation.
* `org.veyra.client.VeyraMediaSaver`: structured media storage engine that routes saved files into typed subfolders.
* `org.veyra.client.VeyraEditHistoryManager`: SQLite-backed edit history log with a configurable per-message limit and per-chat-type rules.
* `org.veyra.client.VeyraAntiDelete`: SQLite-backed anti-delete record store with per-chat-type rules and bulk clear support.
* `org.veyra.client.HiddenContentManager`: local hide/restore manager for chats and messages hidden from view.
* `org.telegram.ui.MessageDetailsActivity`: native inspection view for technical message metadata and JSON export.
* `org.telegram.messenger.shamsicalendar.*`: standalone Persian Solar Hijri calendar calculations.
* `org.telegram.utils.proxy.*`: web proxy transport infrastructure.
* `org.telegram.messenger.MessagesController`: ad suppression, online status, and anti-delete logic hooks.
* `org.telegram.tgnet.ConnectionsManager`: low-level network filter for Ghost Mode and typing/presence suppression.

---

## Building from Source

### Prerequisites
* JDK 21 (Amazon Corretto or OpenJDK)
* Android SDK 35 (Platform 35, Build-Tools 35.0.0)
* Android NDK 27.2.12479018
* CMake 3.22.1
* Gradle 8.14.5

### Local Compilation

1. Clone the repository and check out the `main` branch:
   ```bash
   git clone https://github.com/x1cen/Veyra.git -b main
   cd Veyra
   ```

2. Configure your environment in `local.properties`:
   ```properties
   sdk.dir=/path/to/android-sdk
   cmake.dir=/path/to/android-sdk/cmake/3.22.1
   ```

3. Build the standalone release APK:
   ```bash
   ./gradlew :TMessagesProj_AppStandalone:assembleAfatStandalone --parallel --build-cache
   ```

4. The built package lands at:
   ```
   TMessagesProj_AppStandalone/build/outputs/apk/afat/standalone/Veyra.1.1.2.apk
   ```

---

## Automated CI/CD (GitHub Actions)

Continuous integration and artifact distribution run through GitHub Actions:
* Workflow: `.github/workflows/veyra-build.yml`
* Release Artifacts: builds, signs, and uploads release APKs to [GitHub Releases](https://github.com/x1cen/Veyra/releases) automatically.

---

## 💖 Donations and Support

If Veyra has been useful to you and you'd like to support ongoing development, contributions are welcome through any of these:

* GRAM:
  ```
  UQCyGgaTKVc4U2db4fO6T2HlhEcRjDrCzQudpLjRdOYnAlye
  ```

* USDT (BEP-20):
  ```
  0xfB7e73F63C3A22BcbffF5A9f5452D9f6c95a2772
  ```

* BTC:
  ```
  bc1qr4njyyu9a3w5lckhlws68xrfy0d0dy7vdfy2qa
  ```

* TRX:
  ```
  TNaktPgTmzpz8LUYexmTY9Tfi5yK6JLbVp
  ```

* ETH:
  ```
  0xfB7e73F63C3A22BcbffF5A9f5452D9f6c95a2772
  ```

---

## Security and Privacy Guarantee

* Veyra never routes, proxies, or stores your personal data, credentials, or private keys on any third-party server.
* All network traffic connects directly to official Telegram MTProto datacenters.
* All signing keys and API secrets live in encrypted GitHub Secrets and get injected at build time. None of them are ever committed to source code.
* Screen capture is blocked at the window level across every activity, when the setting is turned on.

---

## License

This project is licensed under the GNU General Public License v2.0 (GPLv2), in compliance with Telegram for Android's upstream licensing terms.
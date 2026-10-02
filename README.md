<p align="center">
  <img src="assets/logo.png" width="220" alt="Veyra Logo" />
</p>

<h1 align="center">Veyra</h1>

<p align="center">
  <b>Executive Privacy-First Telegram Client for Android</b><br>
  Built on official upstream Telegram architecture with sovereign privacy and power-user features
</p>

<p align="center">
  <a href="https://github.com/x1cen/Veyra/actions/workflows/veyra-build.yml"><img src="https://github.com/x1cen/Veyra/actions/workflows/veyra-build.yml/badge.svg?branch=main" alt="Build Status"></a>
  <a href="https://github.com/x1cen/Veyra/releases/latest"><img src="https://img.shields.io/github/v/release/x1cen/Veyra?label=Stable%20Release" alt="Latest Release"></a>
  <a href="https://github.com/DrKLO/Telegram"><img src="https://img.shields.io/badge/Upstream%20Base-v12.9.2%2B-2481CC.svg" alt="Upstream Base"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--2.0-blue.svg" alt="License"></a>
</p>

---

## Overview

**Veyra** is an executive-tier, privacy-first Android client built upon the official upstream Telegram codebase. It harmonizes the rock-solid stability and speed of official Telegram with advanced privacy protections, power-user utilities, full ad suppression, anti-censorship networking, and an exclusive visual identity.

---

## Key Features

### 🛡️ Privacy & Stealth

* **Online Status Visibility Control:** Choose whether to broadcast your online status, completely hide your online presence, hide online status and go offline after sending messages, or stay always online.
* **Granular Ghost Mode:** Individual toggles for each privacy signal — online status, typing/recording actions, read receipts, voice & video note played status, and story view visibility. All enabled by default.
* **Mark as Read on Reply:** Messages remain unread until you actively reply to the conversation.
* **Anti-Delete Message Retention:** Retains messages and media revoked by conversation partners. Deleted incoming messages are visually marked with a red border and red timestamp. Your own sent messages are unaffected.
* **Message Edit History:** Automatically logs previous versions of edited messages. Configure per-message limit (5–100), overflow behaviour (drop oldest or stop logging), and view the full edit timeline with a single tap on the **History** button in the message context menu.
* **Hide Typing Indicator:** Prevents sending "typing..." or audio recording indicators to peers.
* **Block Incoming Secret Chats:** Auto-declines secret chat initiation requests from other users.
* **Hide Proxy Connecting Status:** Hides the "Connecting to proxy..." status line in all surfaces.
* **Variable-Length Security PINs (6–24 Digits):** Flexible PIN lengths across Main Passcode, Duress Code, and Settings Lock.
* **Accurate "Edited" Marker:** Messages are marked as edited only when the text is genuinely changed — reactions never trigger an edited badge.
* **Emergency Duress Code:** Dedicated secondary PIN that silently wipes all local app data and accounts upon entry.
* **Dedicated Settings Lock:** Protect Veyra Settings with a separate PIN.

### 👥 Contact & Profile Intelligence

* **Mutual Contact Indicator:** A blue dot appears on the bottom-left of a user's avatar in the chat list when they have also saved your number (mutual contact).
* **Mutual Contact Row in Profile:** An info row in the user's profile page explicitly confirms mutual contact status.
* **Telegram Premium Status Row:** Displays whether the user currently has an active Telegram Premium subscription — visible in every private user profile.

### 🗂️ Media Storage

* **Structured Media Organisation:** Saved media is filed into dedicated subfolders inside `Documents/Telegram/` — `Video Notes/`, `Voice Notes/`, `Videos/`, `Photos/`, `Audio/`, `Documents/`, `Animations/` — independently of the gallery.
* **Download-State Gated Save Options:** "Save to gallery" and "Save to downloads" options appear only after a file has been fully downloaded, preventing empty-save errors.
* **Video Message Quality Enhancement:** Round video messages record at 640×640 resolution with 2.8 Mbps bitrate for significantly sharper clarity.
* **Video Message Watermark Removal:** The Telegram logo overlay is not rendered onto recorded or saved round video messages.

### 💬 Composing & Chat Actions

* **Hide Content:** Long-press any message to hide it locally. Hidden messages are removed from the chat view without deletion. Tap the Chats tab 7 times in quick succession to restore all hidden messages.
* **Delete Veyra Cache on Message Delete:** When you manually delete a message that was retained by anti-delete, both the Telegram local entry and all Veyra cached data (anti-delete record, edit history) are wiped together.
* **Deleted Account Chat Handling:** When a conversation partner deletes their Telegram account, the input bar is replaced with a **"Deleted Chat"** button. Tapping it shows a confirmation dialog and then performs a full cleanup — dialog, Veyra anti-delete records, and edit history — in one action.
* **Mention by Name:** Insert the user's real display name instead of `@username`.
* **Hide "Send As" Button:** Hides the channel/profile identity switch button.
* **Disable Quick Reaction Double-Tap:** Prevent accidental emoji reactions.
* **Timestamps with Seconds:** Accurate display including exact seconds.
* **Strip Bot Link Trackers:** Auto-strip `utm_*`, `fbclid`, `gclid` from external links.
* **Anonymous Forward by Default:** Default forward previews hide sender name and quote.
* **Disable Large Emoji Rendering:** Render single emojis at normal text size.
* **Jump to First Message:** Jump to the first message in any chat from the 3-dot menu.
* **Copy Dialog ID:** Quickly copy the Telegram Dialog/Peer ID.

### 🖼️ Media & Camera

* **Rear Camera for Video Messages:** Record round video messages with the rear camera by default.
* **Send Typed Text with Stickers & GIFs:** Retain typed captions when sending stickers or GIFs.
* **Keep Original Filename on Download:** Preserve original document filenames.

### 📋 Chat List

* **Mutual Contact Dot:** Blue indicator dot on avatar for mutual contacts (see above).
* **Disable Global Search:** Prevent querying public Telegram channels, bots, and users globally.
* **Disable Media Thumbnails in Dialogs:** Hide video/photo previews in the chat list.
* **Colored Last-Seen Indicator Dots:** Real-time dots on user avatars (yellow ≤15m, orange ≤30m, red ≤60m).
* **Dialog Priority Sorting:** Sort unread chats or unmuted dialogs at the top.

### ⚙️ Power Controls & Interaction

* **Message Details Inspector:** Inspect Message ID, DC, Sender ID, exact media bytes, timestamps — exportable as JSON.
* **Bulk Message Operations:** Forward multiple messages to Saved Messages or unpin multiple pinned messages in bulk.
* **Rich Inline Button Actions:** Long-press bot inline buttons to copy callback data, queries, IDs, or URLs.
* **Call Confirmation Dialog:** Confirm before launching VoIP calls.
* **External Link Confirmation:** Confirm before opening external URLs.
* **Automatic Tracker Stripper:** Auto-clean `utm_*`, `fbclid`, `gclid`, `si`, `igsh`.
* **Skip 5-Second Undo Toast:** Execute delete/clear/archive instantly.
* **Disable Vibration:** Global haptic feedback switch.
* **Disable Link Previews by Default:** Stops automatic webpage search when composing.

### 👥 Group Administration

* **Delete All Messages in Group:** Purge entire group histories with a confirmation dialog.
* **Upgrade Group to Supergroup:** Convert basic groups to supergroups.
* **Auto-Delete Timer:** Configure message auto-deletion for private chats and groups.

### 📱 QR Code Integration

* **Login via QR Code:** Log in by scanning a generated QR code.
* **Direct QR Login Confirmation:** Instant scan and approval of login QR codes.
* **Share via QR Code:** Export sticker packs, channels, groups, proxies as QR codes.

### 🌐 Network & Anti-Censorship

* **Native WEB Proxy Tunnel:** Full Web Proxy transport via isolated background Android System WebView, enabling bypass of DPI restrictions without native overhead.

### 🔒 Security Layer

* **Screen Capture Blocking:** `FLAG_SECURE` applied to all app windows — prevents screenshots and recent-apps thumbnails from leaking conversation content.
* **Hook Framework Detection:** Detects active Frida, Xposed, and LSPosed instrumentation at runtime. If a debugger is attached alongside a hook framework (i.e. an active attack), Veyra performs an immediate data wipe and terminates.
* **Root Awareness:** On rooted devices, elevated security monitoring is activated. Root alone does not trigger a wipe — only confirmed active instrumentation does.
* **AES-256-GCM Encryption API:** Provides a built-in encryption layer for sensitive local data (session tokens, preferences) using AES-256-GCM with randomised 12-byte IVs.
* **Session Token Integrity:** HMAC-SHA256 validation guards session tokens against tampering.

### 🎨 UI & Regional Features

* **Native Persian Solar Calendar:** Dates and numbers in Persian Solar Hijri (Shamsi) when `fa` locale is selected.
* **Telegram ID & DC in Profiles:** Clickable User ID and DC with one-tap copy.
* **Permanent Ad Suppression:** Disables sponsored posts and search ads without Premium.
* **Unrestricted Forward & Copy:** Bypasses forward/copy restrictions in protected chats.
* **Expanded Limits:** Up to 100 pinned chats, 500 favourite stickers, 1000 GIFs, 30 folders.
* **Settings JSON Backup & Restore:** Export and restore your Veyra configuration as JSON.

---

## Feature Comparison Matrix

| Capability | Official Telegram | Standard Forks | Veyra |
| :--- | :---: | :---: | :---: |
| Upstream Telegram Architecture | Yes | Outdated / Partial | Latest Stable |
| Online Status Toggle (Show / Hide) | Privacy Settings Only | Partial | Instant Client Toggle |
| Granular Ghost Mode (5 signals) | No | No | Full |
| Mark as Read on Reply | No | No | Built-in |
| Anti-Delete Message Cache | No | Modded | SQLite-safe with Deleted Badge |
| Message Edit History Log | No | No | Built-in (5–100 edits, viewer) |
| Mutual Contact Indicator | No | No | Blue dot + profile row |
| Telegram Premium Status in Profile | No | No | Active / Not active row |
| Structured Media Storage | No | Partial | 7 typed subfolders |
| Video Message HQ Recording | 384p / 1 Mbps | No | 640p / 2.8 Mbps |
| Screen Capture Blocking | No | No | FLAG_SECURE on all windows |
| Hook/Debugger Detection | No | No | Frida + Xposed + wipe |
| AES-256-GCM Data Encryption | No | No | Built-in API |
| Message Details & JSON Export | No | Third-Party | Built-in Native |
| Bot Button Long-Press Menu | No | Limited | Full (Callback/ID/Query/URL) |
| Native Solar Hijri (Shamsi) Calendar | No | Third-party pack | Fully Native |
| URL Tracker Removal (`cleanUrl`) | No | No | Automatic |
| Web Proxy (WebView Tunnel) | Experimental | No | Fully Integrated |
| Call & Link Action Confirmation | No | No | Built-in Modal Prompts |
| Complete Channel Ad Suppression | No (Premium Only) | Partial | Built-in Permanent Free |
| Unrestricted Forward & Copy | Blocked | Modded | Seamless Bypass |
| Unlocked Pins & Stickers | 5 pins / 5 fave | Variable | 100 pins / 500 stickers |

---

## Architecture & Codebase Structure

The enhancements in Veyra are modularised to preserve upstream stability:

* `org.telegram.messenger.VeyraConfig` — Persistent configuration controller for all privacy switches and feature flags.
* `org.telegram.ui.VeyraSettingsActivity` — Unified settings hub with Ghost Mode and Edit History sub-screens.
* `org.veyra.client.VeyraSecurityGuard` — Security layer: FLAG_SECURE, hook detection, AES-256-GCM, HMAC session validation.
* `org.veyra.client.VeyraMediaSaver` — Structured media storage engine routing files to typed subfolders.
* `org.veyra.client.VeyraEditHistoryManager` — SQLite-backed edit history log with configurable per-message limit.
* `org.veyra.client.HiddenContentManager` — Local hide/restore manager for temporarily hidden messages.
* `org.telegram.ui.MessageDetailsActivity` — Native inspection view for technical message metadata and JSON export.
* `org.telegram.messenger.shamsicalendar.*` — Clean, standalone Persian Solar Hijri calendar calculations.
* `org.telegram.utils.proxy.*` — Web Proxy transport infrastructure.
* `org.telegram.messenger.MessagesController` — Ad-suppression, online status, anti-delete bypass.
* `org.telegram.tgnet.ConnectionsManager` — Low-level network filter for Ghost Mode and typing/presence suppression.

---

## Building from Source

### Prerequisites
* JDK 21 (Amazon Corretto or OpenJDK)
* Android SDK 35 (Platform 35, Build-Tools 35.0.0)
* Android NDK 27.2.12479018
* CMake 3.22.1
* Gradle 8.14.5

### Local Compilation

1. Clone the repository and checkout the `main` branch:
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

4. The built package will be located at:
   ```
   TMessagesProj_AppStandalone/build/outputs/apk/afat/standalone/Veyra.1.0.15.apk
   ```

---

## Automated CI/CD (GitHub Actions)

Continuous integration and artifact distribution are automated via GitHub Actions:
* **Workflow:** `.github/workflows/veyra-build.yml`
* **Release Artifacts:** Automatically builds, signs, and uploads release APKs to [GitHub Releases](https://github.com/x1cen/Veyra/releases).

---

## Donations & Support

If you find Veyra useful and would like to support ongoing development and maintenance, contributions are gratefully welcomed:

* **GRAM:**
  ```
  UQCyGgaTKVc4U2db4fO6T2HlhEcRjDrCzQudpLjRdOYnAlye
  ```

* **USDT (BEP-20):**
  ```
  0xfB7e73F63C3A22BcbffF5A9f5452D9f6c95a2772
  ```

* **BTC:**
  ```
  bc1qr4njyyu9a3w5lckhlws68xrfy0d0dy7vdfy2qa
  ```

* **TRX:**
  ```
  TNaktPgTmzpz8LUYexmTY9Tfi5yK6JLbVp
  ```

* **ETH:**
  ```
  0xfB7e73F63C3A22BcbffF5A9f5452D9f6c95a2772
  ```

---

## Security & Privacy Guarantee

* Veyra never routes, proxies, or stores your personal data, credentials, or private keys on external third-party servers.
* All network traffic connects directly to official Telegram MTProto datacenters.
* All sensitive signing keys and API secrets are maintained in encrypted GitHub Secrets and injected at build time — never committed to source code.
* Screen capture is blocked at the window level across all activities.

---

## License

This project is licensed under the GNU General Public License v2.0 (GPLv2), in compliance with Telegram for Android upstream licensing terms.

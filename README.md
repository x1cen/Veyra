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
* **Online Status Visibility Control:** Choose whether to broadcast your online status (default), completely hide your online presence (appear offline at all times), hide online status and instantly go offline after sending messages, or stay always online.
* **Mark as Read on Reply:** Messages from others remain unread (single checkmark) until you actively reply to the conversation.
* **Anti-Delete Message Retention:** Retains messages and media revoked by conversation partners, marked clearly with a red "Deleted" badge while safely disabling reply/forward on deleted entries.
* **Full Ghost Mode:** Disables sending read receipts across all dialogs when enabled.
* **Hide Typing Indicator:** Prevents sending "typing..." or audio recording indicators to peers.
* **Block Incoming Secret Chats:** Auto-declines secret chat initiation requests from other users.
* **Hide Proxy Connecting Status:** Hides the "Connecting to proxy..." status line in chat action bar, launch activity, and profile headers.
* **Dedicated Settings Lock:** Protect Veyra Settings with a separate, dedicated PIN passcode independent of main app lock.

### 💬 Composing & Chat Actions
* **Mention by Name:** Insert the user's real display name instead of `@username` when mentioning in messages.
* **Hide "Send As" Button:** Hide the channel/profile identity switch button in the message input bar to avoid misclicks.
* **Disable Quick Reaction Double-Tap:** Prevent sending unintended emoji reactions on double-tapping chat messages.
* **Timestamps with Seconds:** Accurate display of message send times including exact seconds.
* **Strip Bot Link Trackers:** Automatically strip external tracking parameters (`utm_*`, `fbclid`, `gclid`) when opening external links from chats.
* **Anonymous Forward by Default:** Automatically default forward previews to hide sender name and quotes.
* **Disable Large Emoji Rendering:** Render single emojis at normal text size instead of oversized stickers.
* **Jump to First Message:** Jump instantly to the first message in any channel, group, or conversation from the 3-dot header menu.
* **Copy Dialog ID:** Quickly copy the Telegram Dialog/Peer ID from the 3-dot header menu.

### 🖼️ Media & Camera
* **Rear Camera for Video Messages:** Start recording round video messages using the rear camera instead of front camera by default.
* **Send Typed Text with Stickers:** Retain and attach whatever text you currently typed in the input bar as a caption when sending stickers.
* **Send Typed Text with GIFs:** Send pre-typed text as a caption when selecting and sending GIFs.
* **Keep Original Filename on Download:** Preserve the original document filename when downloading media files.

### 📋 Chat List Customization
* **Disable Global Search:** Prevent querying public Telegram channels, bots, and users globally from the main search bar.
* **Disable Media Thumbnails in Dialogs:** Option to disable video and photo thumbnail previews in the chat list.
* **Colored Last-Seen Indicator Dots:** Real-time colored indicators on user avatars showing recent offline time (yellow: ≤15m, orange: ≤30m, red: ≤60m).
* **Dialog Priority Sorting:** Sort and prioritize unread chats or unmuted dialogs dynamically at the top of your chat list.

### ⚙️ Power Controls & Interaction
* **Message Details Inspector:** Inspect any message to view Message ID, Datacenter (DC), Sender ID, Forward details, exact media bytes, timestamps, and export complete message metadata as structured JSON directly to the `Downloads/Veyra/` directory with instant system sharing.
* **Bulk Message Operations:** Bulk forward multiple selected messages directly to "Saved Messages" or unpin multiple pinned messages simultaneously from the action mode dropdown.
* **Rich Inline Button Actions:** Long-press any bot inline button to copy callback data (`callback_data` in UTF-8 or Base64), inline queries, user IDs, or URLs.
* **Add @Name on Username Long-Press:** Quickly append `@username` mention directly into the chat input bar from the long-press menu.
* **Call Confirmation Dialog:** Prompts for user confirmation before launching VoIP voice or video calls to prevent accidental calls.
* **External Link Confirmation:** Safeguards against unvetted URLs by confirming external link launches before opening in a browser.
* **Automatic Tracker Stripper (`Clean URLs`):** Automatically cleans marketing and analytics trackers (`utm_*`, `fbclid`, `gclid`, `si`, `igsh`) when copying links.
* **Skip 5-Second Undo Toast:** Executes delete, clear, or archive actions instantly without waiting on the 5-second countdown timer.
* **Disable Vibration:** Global switch to completely turn off all haptic feedbacks and vibrations.
* **Disable Link Previews by Default:** Stops automatic webpage search when drafting links to maintain privacy.
* **Custom Storage & Cache Path:** Direct access to internal storage and external SD card cache configuration.

### 👥 Group Moderation & Administration
* **Delete All Messages in Group:** Purge entire group chat histories with a single confirmation dialog (available for group creators and administrators).
* **Upgrade Group to Supergroup:** Convert standard basic groups directly into supergroups to unlock megagroup capacities, admin roles, and public links.
* **Auto-Delete Timer for Private Chats & Groups:** Configure message auto-deletion countdown timers seamlessly across 1-on-1 chats and private groups.

### 📱 QR Code Integration & Sharing
* **Login via QR Code:** Log into your Telegram account by generating a QR code on the phone screen for another client to scan (`TL_auth_exportLoginToken`).
* **Direct QR Login Confirmation:** Instant scan and approval of login QR codes (`TL_auth_acceptLoginToken`).
* **Share via QR Code:** Export and share sticker packs, channels, groups, and proxies as beautiful native QR codes with a single tap.

### 🌐 Network & Anti-Censorship
* **Native WEB Proxy Tunnel:** Full Web Proxy transport implementation based on an isolated background Android System WebView session, enabling bypass of aggressive DPI and network restrictions without native C++ overhead.

### 🎨 UI & Regional Features
* **Dialog Priority Sorting:** Sort and prioritize unread chats or unmuted dialogs dynamically at the top of your chat list.
* **Disable Trending Stickers:** Hide the trending and featured sticker suggestions tab from the sticker picker.
* **Unblock All / Deleted Users:** Batch unblock all blocked users or specifically filter and unblock deleted accounts from the Privacy settings.
* **Native Persian Solar Calendar (گاه‌شمار خورشیدی):** Automatically displays dates and numbers using the Persian Solar Hijri (Shamsi) calendar when Persian language (`fa`) is selected.
* **Telegram ID & DC in Profiles:** Displays clickable User ID, Group/Channel ID, and Datacenter (DC) inside profile pages with one-tap copy.
* **Permanent Ad Suppression:** Completely disables sponsored channel posts and sponsored search ads without requiring a Telegram Premium subscription.
* **Unrestricted Forward & Copy (`NoForwards` Bypass):** Bypasses copy and forward restrictions in protected channels and groups.
* **Bypass Android Content Restrictions:** Displays sensitive channels and content restricted specifically on Android.
* **Expanded Limits:** Up to 100 pinned chats, 500 favorite stickers, 1000 GIFs, and 30 folders.
* **Rich Categorized Veyra Settings:** Completely redesigned modular settings hub categorized into Privacy & Security, Chat List, Composing & Messages, Media & Camera, Controls & General, and Backup & Restore.
* **Settings JSON Backup & Restore:** Export your custom Veyra configuration as JSON to clipboard or restore/reset anytime.

---

## Feature Comparison Matrix

| Capability | Official Telegram | Standard Forks | Veyra |
| :--- | :---: | :---: | :---: |
| Upstream Telegram Architecture | Yes | Outdated / Partial | Latest Stable |
| Online Status Toggle (Show / Hide) | Privacy Settings Only | Partial | Instant Client Toggle |
| Mark as Read on Reply | No | No | Built-in |
| Anti-Delete Message Cache | No | Modded | SQLite-safe with Deleted Badge |
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

The enhancements in Veyra are modularized to preserve upstream stability:

* `org.telegram.messenger.VeyraConfig`: Persistent configuration controller managing user preferences and privacy switches.
* `org.telegram.ui.VeyraSettingsActivity`: Unified settings interface for all custom Veyra features.
* `org.telegram.ui.MessageDetailsActivity`: Native inspection view for technical message metadata and JSON serialization.
* `org.telegram.messenger.shamsicalendar.*`: Clean, standalone Persian Solar Hijri calendar calculations and numeral formatters.
* `org.telegram.utils.proxy.*`: Web Proxy transport and tester infrastructure utilizing `androidx.webkit`.
* `org.telegram.messenger.MessagesController`: Ad-suppression rules, online status dispatching, and content restriction bypasses.
* `org.telegram.tgnet.ConnectionsManager`: Low-level network filter for Ghost Mode, Hide Typing, and presence updates.

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
   TMessagesProj_AppStandalone/build/outputs/apk/afat/standalone/Veyra.1.0.3.apk
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
* All sensitive signing keys and API secrets are maintained in encrypted GitHub Secrets.

---

## License

This project is licensed under the GNU General Public License v2.0 (GPLv2), in compliance with Telegram for Android upstream licensing terms.

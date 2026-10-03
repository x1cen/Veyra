<p align="center">
  <img src="assets/logo.png" width="220" alt="Veyra Logo" />
</p>

<h1 align="center">Veyra</h1>

<p align="center">
  <b>Privacy-focused Telegram client for Android</b><br>
  Based on official upstream Telegram source with added privacy options and usability tweaks.
</p>

<p align="center">
  <a href="https://github.com/x1cen/Veyra/actions/workflows/veyra-build.yml"><img src="https://github.com/x1cen/Veyra/actions/workflows/veyra-build.yml/badge.svg?branch=main" alt="Build Status"></a>
  <a href="https://github.com/x1cen/Veyra/releases/latest"><img src="https://img.shields.io/github/v/release/x1cen/Veyra?label=Release" alt="Latest Release"></a>
  <a href="https://github.com/DrKLO/Telegram"><img src="https://img.shields.io/badge/Upstream%20Base-v12.9.2%2B-2481CC.svg" alt="Upstream Base"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--2.0-blue.svg" alt="License"></a>
</p>

---

## What is Veyra

Veyra is a fork of official Telegram for Android. It keeps the upstream codebase and stability, but adds practical privacy controls, anti-delete retention, edit history tracking, ad removal, and interface options that official Telegram lacks.

---

## Features

### Privacy and Anti-Delete

* Dedicated Anti-Delete Screen: Keep messages and media deleted by others. Deleted messages show a clear deleted marker and are rendered with lower opacity so they are easy to spot. You can choose which chat types to protect (private chats, groups, channels, bots).
* Message Edit History: Keeps a record of previous versions of edited messages, backed by SQLite. You can set the history limit (between 5 and 100 entries per message) and choose whether to drop the oldest version when full. Filterable by chat type (enabled by default for private chats, groups, and channels; off for bots).
* Ghost Mode Controls: Independent switches for online presence, typing status, read receipts, and voice or video play status.
* Mark as Read on Reply: Incoming messages stay unread until you actually send a reply.
* Hide Typing: Stops sending "typing..." and audio recording status.
* Block Secret Chats: Automatically declines incoming secret chat requests.
* Hide Proxy Connecting Text: Removes the connecting status text from headers and menus.
* Screen Capture Blocking: Optional window-level protection (FLAG_SECURE) to prevent screenshots and task switcher previews.

### Emergency Wipe

Accessible directly from the three-dot menu on the main screen (with a confirmation prompt before running):
* Local Wipe: Clears local databases, caches, downloaded files, and logs you out immediately.
* Full Wipe: Wipes local data, deletes your sent messages in groups, deletes private chats for both sides, leaves channels, and sends a final account deletion request.

### Chat List and Navigation

* Hide Chat: Select chats and hide them from the main list.
* View Details for Non-Admins: View group and channel member lists, admins, and permissions even if you are not an admin. Non-editable fields stay read-only.
* Dialog Sorting: Optional sorting for unread or unmuted chats at the top. Standard timestamp ordering remains default.
* Disable Global Search: Turns off public channel and user results in search.
* Media Previews in Dialogs: Option to turn off image and video thumbnails in the chat list.
* Last Seen Indicators: Color-coded dots on user avatars based on recent activity.
* Jump to First Message: Jump straight to the beginning of any conversation from the chat menu.
* Copy Dialog ID: Copy the raw peer ID directly from the menu.

### Composing and Media

* Rear Camera for Video Notes: Option to start round video messages with the rear camera.
* Captions on Stickers and GIFs: Keep your typed text as a caption when sending a sticker or GIF.
* Keep Original Filename: Saves files using their original name instead of internal Telegram IDs.
* Confirm Calls and Links: Dialogs to prevent accidental calls or opening untrusted links.
* Clean URLs: Automatically strips tracking parameters (utm_*, fbclid, gclid) when copying or opening web links.
* Anonymous Forwarding: Option to forward messages without quote headers by default.
* Disable Big Emoji: Displays single emojis at standard text size.
* Seconds in Timestamps: Displays exact seconds on message timestamps.

### System and Localization

* Full Persian (Farsi) Localization: Dedicated Persian translations across all Veyra settings screens.
* Solar Hijri (Shamsi) Calendar: Shows dates in Persian calendar when Persian language is selected.
* No Ads: Official Telegram sponsored channel messages are disabled.
* Keystore Encryption: Sensitive local caches use AES-256-GCM hardware-backed keys.
* Unlocked Limits: Higher limits for pinned chats, favorite stickers, GIFs, and folders.
* Settings Backup and Restore: Export and import your configuration via JSON.
* In-App Update Checker: Check for new releases directly from Telegram settings.

---

## Comparison

| Feature | Official Telegram | Standard Mods | Veyra |
| :--- | :---: | :---: | :---: |
| Base Codebase | Official | Often Outdated | Latest Upstream |
| Anti-Delete Message Retention | No | Basic | Chat-type filtering and dimmed display |
| Message Edit History | No | No | Encrypted SQLite storage with viewer |
| Non-Admin View Details | No | Partial | Read-only details with member lists |
| Ghost Mode Controls | Privacy Settings | Partial | Granular per-signal toggles |
| Emergency Wipe (Local and Full) | No | No | Built-in with confirmation dialogs |
| Solar Hijri Calendar | No | Third-party | Native |
| Ad Removal | Premium Only | Varies | Permanently disabled |
| URL Tracker Removal | No | No | Automatic |
| Screen Capture Protection | No | No | Configurable FLAG_SECURE |
| Call and Link Confirmation | No | No | Built-in prompts |

---

## Building from Source

### Requirements
* JDK 21 (OpenJDK or Amazon Corretto)
* Android SDK 35 (Build-Tools 35.0.0)
* Android NDK 27.2.12479018
* CMake 3.22.1
* Gradle 8.14.5

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/x1cen/Veyra.git -b dev
   cd Veyra
   ```

2. Set paths in `local.properties`:
   ```properties
   sdk.dir=/path/to/android-sdk
   cmake.dir=/path/to/android-sdk/cmake/3.22.1
   ```

3. Build the release APK:
   ```bash
   ./gradlew :TMessagesProj_AppStandalone:assembleAfatStandalone --parallel --build-cache
   ```

4. The output APK will be generated under:
   ```
   TMessagesProj_AppStandalone/build/outputs/apk/afat/standalone/
   ```

---

## CI/CD

Release builds are handled by GitHub Actions (`.github/workflows/veyra-build.yml`). Every tag and dispatch produces signed standalone APKs for arm64-v8a, armeabi-v7a, x86, x86_64, and universal architectures.

Download signed APKs on the [Releases page](https://github.com/x1cen/Veyra/releases).

---

## Privacy Notice

* Veyra connects directly to official Telegram MTProto datacenters.
* No proxy, analytics, or telemetry servers sit between you and Telegram.
* All sensitive API credentials, keys, and tokens are injected via GitHub Secrets at build time and are not hardcoded in source.

---

## License

GNU General Public License v2.0 (GPLv2), matching upstream Telegram for Android.

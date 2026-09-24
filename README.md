# Veyra

Veyra is an advanced, privacy-first Android client built on the official upstream Telegram v12.9.2 architecture. It merges Telegram core reliability with essential power-user capabilities, enhanced privacy controls, an ad-free messaging experience, and an executive luxury visual identity.

[![Build and Release Veyra APK](https://github.com/x1cen/Veyra/actions/workflows/veyra-build.yml/badge.svg?branch=dev)](https://github.com/x1cen/Veyra/actions/workflows/veyra-build.yml)
[![Pre-release](https://img.shields.io/github/v/release/x1cen/Veyra?include_prereleases&label=Testing%20Build)](https://github.com/x1cen/Veyra/releases)
[![Upstream Telegram Base](https://img.shields.io/badge/Upstream%20Base-v12.9.2-2481CC.svg)](https://github.com/DrKLO/Telegram)
[![License: GPL-2.0](https://img.shields.io/badge/License-GPL--2.0-blue.svg)](LICENSE)

---

## Key Highlights

* **Telegram ID & DC Inspection:** Direct display of Telegram User ID, Group ID, Channel ID, Bot ID, Topic ID, and Datacenter (DC) inside the standard Profile details column across all chat types, complete with instant one-tap copy functionality.
* **Ad-Free Communication:** Complete removal of unsolicited sponsored messages in public channels, suppression of sponsored search results in global dialog search, and elimination of video advertising.
* **Content Freedom (Forced Copy):** Unrestricted text selection, message copying, and media forwarding across protected groups and channels (`isChatNoForwards` override).
* **Anti-Revoke Protection:** Local preservation of messages and time-limited media deleted by counterparties in private and group conversations.
* **Security & Screen Capture Rights:** Clean removal of restrictive `FLAG_SECURE` window limitations, allowing screenshots, screen sharing, and screen recording when permitted by the user.
* **Sovereign Luxury Identity:** Custom sculpted metallic gold and obsidian emblem designed specifically for Veyra, available as an adaptive icon across all modern Android versions (Android 8.0 through Android 15).
* **Google Play Protect Compliance:** Clean native build signed with standard RSA-2048 keys and free from heuristic-triggering test routines or anti-tamper crashes.

---

## Feature Comparison Matrix

| Capability | Official Telegram | Standard Third-Party Forks | Veyra |
| :--- | :---: | :---: | :---: |
| Official Upstream 12.9.2 Base | Yes | Partial | Yes |
| Profile Telegram ID & DC Display | No | Top Banner / Header | Integrated Profile Column |
| Complete Channel Ad Suppression | No (Premium Required) | Partial | Built-in Free |
| Global Search Ad Suppression | No | No | Built-in Free |
| Unrestricted Forward & Copy (`NoForwards`) | Blocked | Add-on | Seamless Built-in |
| Screenshot Protection Override | Blocked | Modded | Built-in Unrestricted |
| Anti-Delete Message Cache | No | Modded | Native Local SQLite |
| Clean Signing (No Heuristic Warnings) | Yes | Variable | Verified RSA-2048 Clean |

---

## Architecture & Codebase Structure

The project maintains upstream synchronization with official Telegram while isolating enhancements cleanly within structured packages:

* `org.veyra.client.VeyraSecurity`: Security verification, keystore hash validation, and safe integrity checks.
* `org.veyra.client.VeyraAntiDelete`: Local SQLite storage and recovery helpers for deleted messages and media.
* `org.veyra.helpers.MonetHelper`: Dynamic theme extraction and Android Material You palette adaptation.
* `org.telegram.ui.ProfileActivity`: Native UI integration for chat identifiers and datacenter diagnostics.
* `org.telegram.messenger.MessagesController`: Ad-suppression rules and content-restriction bypass logic.

---

## Building from Source

### Prerequisites
* JDK 21 (Amazon Corretto or OpenJDK)
* Android SDK 35 (Platform 35, Build-Tools 35.0.0)
* Android NDK 27.2.12479018
* CMake 3.22.1
* Gradle 8.14.5

### Local Compilation

1. Clone the repository and checkout the `dev` branch:
   ```bash
   git clone https://github.com/x1cen/Veyra.git -b dev
   cd Veyra
   ```

2. Configure environment and signing properties in `local.properties`:
   ```properties
   sdk.dir=/path/to/android-sdk
   cmake.dir=/path/to/android-sdk/cmake/3.22.1
   ```

3. Assemble the standalone universal APK:
   ```bash
   ./gradlew :TMessagesProj_AppStandalone:assembleStandalone
   ```

4. The compiled package will be located at:
   ```
   TMessagesProj_AppStandalone/build/outputs/apk/afat/standalone/app.apk
   ```

---

## Automated CI/CD (GitHub Actions)

Continuous integration and artifact distribution are automated via GitHub Actions:
* **Workflow:** `.github/workflows/veyra-build.yml`
* **Signing:** Automates custom keystore injection via encrypted repository secrets.
* **Pre-release Testing:** Development artifacts are automatically published as tagged pre-releases for preview testing.

---

## Security & Privacy Boundary

* No personal data or credentials are intercepted, proxied, or transmitted to any third-party infrastructure.
* All network traffic communicates directly with official Telegram MTProto datacenters.
* Sensitive repository secrets (keystores, passwords, API identifiers) must remain stored in GitHub Secrets and should never be committed into source control.

---

## License

This project is licensed under the GNU General Public License v2.0 (GPLv2), in compliance with Telegram for Android upstream licensing terms.

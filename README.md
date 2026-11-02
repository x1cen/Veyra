<p align="center">
  <img src="assets/logo.png" width="200" alt="Veyra Logo" />
</p>

<h1 align="center">🛡️ Veyra</h1>

<p align="center">
  <b>Privacy-focused, refined Telegram client for Android</b><br>
  Built on the official upstream Telegram codebase (v13.1.1) with enhanced stability, performance optimizations, and user-centric controls.
</p>

<p align="center">
  <a href="https://github.com/x1cen/Veyra/actions/workflows/veyra-build.yml"><img src="https://github.com/x1cen/Veyra/actions/workflows/veyra-build.yml/badge.svg?branch=master" alt="Build Status"></a>
  <a href="https://github.com/x1cen/Veyra/releases/latest"><img src="https://img.shields.io/github/v/release/x1cen/Veyra?label=Release" alt="Latest Release"></a>
  <a href="https://github.com/DrKLO/Telegram"><img src="https://img.shields.io/badge/Upstream%20Base-v13.1.1-2481CC.svg" alt="Upstream Base"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--2.0-blue.svg" alt="License"></a>
</p>

---

## Overview

**Veyra** is an independent Android messaging client based on official Telegram v13.1.1 (Build 7237). It preserves the fluid UX, end-to-end MTProto security, and core speed of official Telegram, while offering a cleaner experience, improved local data handling, and optimized resource management.

---

## Building from Source

### Prerequisites
* **JDK**: 21 (OpenJDK / Amazon Corretto)
* **Android SDK**: API 35 / 36 (Build Tools `35.0.0`+)
* **Android NDK**: `27.2.12479018`
* **CMake**: `3.22.1`

### Local Build
1. Clone the repository with submodules:
   ```bash
   git clone --recursive https://github.com/x1cen/Veyra.git
   cd Veyra
   ```

2. Set your SDK and CMake paths in `local.properties`:
   ```properties
   sdk.dir=/path/to/android-sdk
   cmake.dir=/path/to/android-sdk/cmake/3.22.1
   ```

3. Compile standalone release APKs:
   ```bash
   ./gradlew :TMessagesProj_AppStandalone:assembleAfatStandaloneRelease --parallel
   ```
   Outputs will be placed under `TMessagesProj_AppStandalone/build/outputs/apk/afat/standalone/` (`universal` and per-ABI APKs: `arm64-v8a`, `armeabi-v7a`, `x86_64`).

### Automated CI/CD
Automated multi-architecture packaging, signing, and release publishing are handled via GitHub Actions in [`.github/workflows/veyra-build.yml`](.github/workflows/veyra-build.yml).

---

## Donations & Support

If you find Veyra useful and would like to support ongoing maintenance and development:

* **TON / GRAM**:
  ```text
  UQCyGgaTKVc4U2db4fO6T2HlhEcRjDrCzQudpLjRdOYnAlye
  ```

* **USDT (BEP-20)** / **ETH**:
  ```text
  0xfB7e73F63C3A22BcbffF5A9f5452D9f6c95a2772
  ```

* **BTC**:
  ```text
  bc1qr4njyyu9a3w5lckhlws68xrfy0d0dy7vdfy2qa
  ```

* **TRX**:
  ```text
  TNaktPgTmzpz8LUYexmTY9Tfi5yK6JLbVp
  ```

---

## License

Licensed under the [GNU General Public License v2.0 (GPLv2)](LICENSE), in compliance with Telegram for Android upstream licensing terms.

# AGram

AGram is an optimized, privacy-respecting client for Android based on Telegram 12.9.2.

- **Package Name:** `org.agram.messanger`
- **Application Name:** AGram
- **Coexistence:** Can be installed side-by-side with official Telegram and other clients.

## Key Features

- **Media Freedom:** Save expiring (TTL) and restricted media directly to storage.
- **Anti-Delete Support:** Retain messages locally even if deleted by sender.
- **Privacy & Security:** Removes tracking SDKs, billing libraries, and proprietary telemetry.
- **Clean UI:** Authentic Telegram launcher icon with Material 3 dynamic Monet theming.

## Building from Source

To build AGram APK from source using Gradle:

```bash
./gradlew :TMessagesProj_AppStandalone:assembleStandalone
```

The resulting APK will be located under `TMessagesProj_AppStandalone/build/outputs/apk/afat/standalone/`.

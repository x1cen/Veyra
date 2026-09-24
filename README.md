# AGram

AGram is a modified Telegram client for Android based on Telegram upstream 12.9.2. It adds local anti-delete logging, view-once media export, and system Monet colors while keeping the original Telegram UI and launcher icon intact.

It uses an independent application ID (`org.agram.messanger`), so you can install and run it alongside the official Telegram app, Telegram Beta, or web wrappers without conflicts.

## Modifications and Features

- **Local Anti-Delete:** Incoming messages that get deleted by the sender remain visible in your local chat history, marked with an indicator. Deletion history is stored in your device's local database.
- **Expiring Media Export:** View-once and self-destructing photos and videos can be saved directly to local storage without restrictions.
- **Parallel Installation:** Uses the package name `org.agram.messanger`. It does not replace or interfere with any other Telegram installation on the same device.
- **Stock Launcher Design:** Retains the official Telegram paper plane launcher icon and branding assets.
- **Monet Dynamic Theming:** Adapts interface accent colors to your Android 12+ wallpaper palette.
- **Telemetry and Billing Removed:** Google Play Billing and unnecessary vendor analytics libraries have been stripped out.
- **Custom Keystore Support:** Can be signed with an independent private release keystore to avoid Google Play Protect signature conflicts.

### Note on Passkey Login

Logging in via Passkeys (FIDO2 / WebAuthn) is intentionally disabled. Android Credential Manager restricts `telegram.org` passkeys to Telegram's official signing certificates through Google Digital Asset Links (`/.well-known/assetlinks.json`). Third-party forks with custom package names cannot claim that origin. Use your phone number and login code (SMS or active session) to sign in.

## Project Structure

```
.
|-- TMessagesProj/                  # Main Telegram Android client source
|   |-- src/main/java/org/agram/    # AGram custom mod implementations
|   |   |-- client/                 # Anti-delete, media export, and security logic
|   |   \-- helpers/                # Monet dynamic color engine
|   \-- config/                     # Build configuration and signing keys
|-- TMessagesProj_App/              # Standard application module
|-- TMessagesProj_AppStandalone/    # Standalone APK build module (no Google Play dependencies)
\-- .github/workflows/              # Automated CI/CD pipelines
```

## Building

### Requirements

- Android Studio Jellyfish (or newer) / command-line tools
- JDK 21 (Amazon Corretto 21 recommended)
- Android SDK with Platform 35
- Android NDK 27.2.12479018
- CMake 3.22.1

### Local Build

1. Clone the repository:
   ```bash
   git clone https://github.com/x1cen/agram.git
   cd agram
   ```

2. Make sure `local.properties` contains your Android SDK path:
   ```properties
   sdk.dir=/path/to/your/android-sdk
   ```

3. Build the standalone release APK:
   ```bash
   ./gradlew :TMessagesProj_AppStandalone:assembleStandalone
   ```

The compiled APK will be generated at:
`TMessagesProj_AppStandalone/build/outputs/apk/afat/standalone/`

### Automated CI with GitHub Actions

The repository includes a GitHub Actions workflow (`.github/workflows/agram-build.yml`) that compiles and signs the standalone APK on Ubuntu runners.

To run builds with your own credentials, configure these repository secrets in **Settings -> Secrets and variables -> Actions**:

- `API_ID`: Your Telegram API ID from my.telegram.org
- `API_HASH`: Your Telegram API Hash from my.telegram.org
- `APP_ID`: Application ID string
- `APP_HASH`: Application Hash string
- `AGRAM_KEYSTORE_BASE64`: Base64-encoded release `.keystore` file
- `AGRAM_RELEASE_STORE_PASSWORD`: Keystore store password
- `AGRAM_RELEASE_KEY_ALIAS`: Keystore alias
- `AGRAM_RELEASE_KEY_PASSWORD`: Keystore key password

Trigger the workflow from the **Actions** tab using the **Run workflow** button. Completed APK artifacts are automatically attached to GitHub Releases.

## License

AGram is distributed under the GNU General Public License v2.0 or later, matching upstream Telegram for Android. See the `LICENSE` file for full terms.

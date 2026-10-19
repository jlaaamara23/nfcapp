# Android Employee App (NFC Attendance)

Native Java Android application for employees to clock in/out by scanning workplace NFC tags (NTAG213/NTAG215).

## Requirements

- Android Studio Hedgehog or newer
- JDK 17
- Android device/emulator with NFC (physical device required for real tag scans)
- Backend running and reachable from the phone

## Configure API URL

Default physical-phone URL is `http://10.0.0.16:8080` (change to your PC LAN IP).

For emulator, use `http://10.0.2.2:8080` in `app/build.gradle`:

```gradle
buildConfigField "String", "API_BASE_URL", "\"http://YOUR_LAN_IP:8080\""
```

Cleartext HTTP is allowed in development via `network_security_config.xml` and
`android:usesCleartextTraffic="true"`. Use HTTPS in production.

## Build & install APK

### From Android Studio

1. Open the `android-app` folder in Android Studio.
2. Let Gradle sync.
3. Run on a device/emulator, or **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Install the generated APK from `app/build/outputs/apk/debug/app-debug.apk`.

### From command line

```bash
cd android-app
chmod +x gradlew   # after generating the wrapper in Android Studio once
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If the Gradle wrapper is not present yet, open the project once in Android Studio to generate it, or install the Android Gradle Plugin toolchain locally.

## Seed login (development)

- Username: `employee1`
- Password: `Employee123!`

## NFC tag payload

Write an NDEF URI/Text record to the tag using the Admin panel payload, for example:

```text
nfcattendance://station/GATE-01?rv=NFC-XXXXXXXXXXXXXXXX
```

The app extracts `GATE-01` and sends it with the authenticated employee JWT. The phone clock is never trusted as the attendance timestamp.

## Screens

1. Login
2. Attendance home (large Clock In / Clock Out)
3. NFC scan status
4. Attendance history
5. Profile

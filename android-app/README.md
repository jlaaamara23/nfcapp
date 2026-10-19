# Android Employee App (NFC Attendance)

Native Java Android application for employees to clock in/out by scanning workplace NFC tags (NTAG213/NTAG215).

## Requirements

- Android Studio Hedgehog or newer
- JDK 17
- Android device with NFC (required for real tag scans)
- Backend reachable at `https://api.sreamline.tech`

## Configure API URL

Production (Play Store) URL is already set in `app/build.gradle`:

```gradle
buildConfigField "String", "API_BASE_URL", "\"https://api.sreamline.tech\""
```

For local LAN testing only, temporarily switch to `http://YOUR_PC_IP:8080` and allow cleartext again in the manifest / network security config.

## Build debug APK

1. Open the `android-app` folder in Android Studio.
2. Let Gradle sync.
3. Run on a device, or **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## Publish to Google Play Store

### 1. Create a release keystore (once)

In Android Studio: **Build > Generate Signed App Bundle / APK…**

Or from a terminal (save the passwords somewhere safe — if you lose the keystore you cannot update the app):

```bash
keytool -genkey -v -keystore nfc-attendance-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias nfcattendance
```

Keep `nfc-attendance-release.jks` and the passwords offline / in a password manager. Do **not** commit the `.jks` file to Git.

### 2. Build a signed AAB

In Android Studio:

1. **Build > Generate Signed App Bundle / APK…**
2. Choose **Android App Bundle**
3. Select your keystore / alias / passwords
4. Choose **release**
5. Finish

Output file:

`app/release/app-release.aab`

### 3. Play Console checklist

1. Open [Google Play Console](https://play.google.com/console)
2. **Create app** → name e.g. `נוכחות NFC`
3. Complete:
   - App access / privacy policy URL (host a simple page on `https://sreamline.tech` or `https://admin.sreamline.tech`)
   - Data safety form (login, attendance timestamps, employee profile)
   - Content rating questionnaire
   - Target audience (18+)
   - Store listing: short description, full description, icon 512×512, feature graphic 1024×500, phone screenshots (2+)
4. **Production** (or Internal testing first) → **Create new release** → upload `app-release.aab`
5. Roll out

### 4. Version bumps for updates

Before each new Play upload, increase both in `app/build.gradle`:

```gradle
versionCode 2   // must increase every upload
versionName "1.0.1"
```

## Seed login (development)

- Username: `employee1`
- Password: `Employee123!`

Create real employees from the Admin panel: `https://admin.sreamline.tech`

## NFC tag payload

Write an NDEF URI/Text record using the Admin panel payload, for example:

```text
nfcattendance://station/GATE-01?rv=NFC-XXXXXXXXXXXXXXXX
```

The app extracts the station code and sends it with the authenticated employee JWT. The phone clock is never trusted as the attendance timestamp.

## Screens

1. Login
2. Attendance home (large Clock In / Clock Out)
3. NFC scan status
4. Attendance history
5. Profile

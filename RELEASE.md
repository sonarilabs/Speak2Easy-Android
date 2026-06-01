# Release / Play Store guide

End-to-end steps to ship Speak2Easy to Play's **Internal Testing** track.
This is a checklist — most items are one-time setup; the recurring ones are
flagged **[every release]**.

---

## One-time setup

### 1. Generate the upload keystore

The keystore holds your *upload key* (Google Play App Signing keeps the
actual signing key in their KMS). Store it somewhere safe and back it up —
losing it means contacting Google support to reset, with potential downtime.

```bash
keytool -genkey -v -keystore ~/.android/speak2easy-upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias speak2easy-upload
```

You'll be prompted for:
- **Keystore password** — pick a strong one
- **Key password** — same as keystore password is fine
- Name / org / locality — purely cosmetic, used in the cert subject

**Store both passwords in 1Password (or equivalent) immediately.**

### 2. Configure Gradle to use the keystore

Append to **`~/.gradle/gradle.properties`** (the user-level file, not the
project's `gradle.properties` — that one is committed to git):

```properties
SPEAK2EASY_KEYSTORE_FILE=/Users/kratos/.android/speak2easy-upload.jks
SPEAK2EASY_KEYSTORE_PASSWORD=<your-keystore-password>
SPEAK2EASY_KEY_ALIAS=speak2easy-upload
SPEAK2EASY_KEY_PASSWORD=<your-key-password>
```

`app/build.gradle.kts` reads these via `providers.gradleProperty(...)`. If the
file is missing, release builds silently fall back to the debug keystore (so
the build doesn't break on CI or a fresh clone) — those builds are only good
for local verification, **never upload** them to Play.

### 3. Create the Play Console app

1. Pay **$25** at https://play.google.com/console (one-time, lifetime).
2. **Create app** → app name "Speak2Easy", default language English (US),
   "App" (not Game), Free, agree to declarations.
3. Complete the **App content** forms (left sidebar) before any track will
   accept an upload:
   - Privacy Policy: `https://sonarilabs.ai/privacy`
   - App access: provide demo credentials if your app gates content behind
     login. Google reviewers use these to test.
   - Ads: No
   - Content rating questionnaire (~5 min)
   - Target audience (13+ since you collect email/audio)
   - **Data safety**: declare email, audio recordings, optional location
     (country / state / city from onboarding). Mark all as collected for
     "App functionality", encrypted in transit, deletable on request.
   - News app: No
   - COVID-19 contact tracing: No
   - Government app: No
4. **Store listing** (placeholder is fine for internal testing):
   - Short description (80 chars)
   - Full description (4000 chars)
   - App icon: 512×512 PNG (use a high-res export of `speak2easy_logo.png`)
   - Feature graphic: 1024×500 PNG
   - At least 2 phone screenshots (16:9 or 9:16)

### 4. Set up Play App Signing

Under **Setup → App integrity → App signing**:
- Choose **Use Google-generated key** (recommended). Google generates the
  app signing key and keeps it; you only ever handle the upload key.
- The first AAB you upload finalizes this.

### 5. Register the Play signing key with Google OAuth

> ⚠️ **You MUST do this after your first AAB upload**, otherwise Google
> Sign-In silently fails for users who install via Play.

When Google signs your AAB, it produces a *different* signing cert than
your local upload key. Currently your GCP Android OAuth client only knows
your debug-key SHA-1 (`42:0F:97:75:1A:3D:D8:B0:6D:6A:4E:C4:13:27:5A:93:81:A9:2E:CE`).

After the first upload:
1. Play Console → **Setup → App integrity → App signing** → copy the
   "App signing key certificate" **SHA-1 fingerprint**
2. https://console.cloud.google.com/apis/credentials → open your Android
   OAuth client (`...sp1v6p0cmfgn1fkvknfjq1svvoq7eheb...`)
3. Click **Add fingerprint** and paste the Play SHA-1 (don't remove the
   debug one — you need both: debug for `adb install` builds, Play for
   Play-distributed builds)
4. Save. Propagation is usually instant but can take up to 5 min.

**Backend changes: none.** The backend only validates Google ID tokens
against the *Web Client ID* via JWKS — it never sees or checks SHA-1
fingerprints. The same `GOOGLE_OAUTH_CLIENT_IDS` env var works for both
debug-installed and Play-installed clients.

---

## Every release

### 1. **[every release]** Bump `versionCode` in `app/build.gradle.kts`

```kotlin
versionCode = 2  // was 1
versionName = "1.0.1"  // user-visible string
```

Play **rejects** AABs with a `versionCode` that's been used before.
`versionName` is freeform but conventionally semver.

### 2. **[every release]** Build the signed bundle

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### 3. **[every release]** Smoke-test the release build locally

R8 obfuscation can break things that worked in debug (reflection, serializer
resolution, etc.). Always install the *release* AAB on a real device before
uploading:

```bash
# One-time: install bundletool
brew install bundletool

# Extract device-specific APKs and install
bundletool build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=/tmp/speak2easy.apks --connected-device \
  --ks=$HOME/.android/speak2easy-upload.jks \
  --ks-key-alias=speak2easy-upload
bundletool install-apks --apks=/tmp/speak2easy.apks
```

Smoke-test the critical paths: sign-in (Google + Email), Practice session,
Writing trace, Settings, daily notification fires.

### 4. **[every release]** Upload to Internal Testing

1. Play Console → **Testing → Internal testing → Create new release**
2. **Upload `app-release.aab`** (drag-and-drop)
3. **Release name** auto-fills from `versionName`
4. **Release notes** — ~500 chars in English (other locales optional)
5. **Save → Review release → Start rollout to Internal testing**

### 5. **[once]** Add testers

- **Testing → Internal testing → Testers tab → Create email list**
- Add Gmail addresses (work emails work only if their Workspace has Play access)
- Copy the **opt-in URL** ("Copy link" button)
- Send to testers — they click → "Become a tester" → install via Play Store
  in 10–30 min

Up to **100** internal testers, no Google review needed, rolls out almost
immediately.

---

## Promoting to wider testing

When ready for more testers:

- **Closed testing**: invite by email list, larger groups, ~hours for rollout
- **Open testing**: anyone with the opt-in link; **requires 12 closed testers
  for 14 days** before promotion (Google rule added 2024)
- **Production**: full Play Store listing, requires Google review (1–7 days
  first time, usually <24h for updates)

---

## Things to verify before going public

- [ ] R8 release build doesn't crash on Google Sign-In, Practice session start,
      or Writing SVG load (these are the reflection-heavy paths)
- [ ] Daily reminder notification fires with the new icon (`ic_notification`,
      not the launcher square)
- [ ] No `Authorization: Bearer ...` lines in `adb logcat` from a release APK
      (confirms `BuildConfig.DEBUG` gate works)
- [ ] `dumpsys backup --users 0` on a release install shows the app as
      backup-disabled (confirms `allowBackup="false"` took effect)
- [ ] Play Console → **Reach and devices** shows your target device list as
      expected (Pixel, Galaxy, Z Flip, etc.)
- [ ] Privacy Policy and Terms URLs render on a fresh browser session

## Xiaomi Mi 8 (MIUI) build, install, and testing guide

Prereqs
- Enable Developer options: Settings > About phone > tap MIUI version 7 times
- Enable USB debugging: Settings > Additional settings > Developer options > USB debugging
- Install Android Platform Tools (adb) on your PC

Build
- From project root: `./gradlew assembleDebug`
- APK output: `app/build/outputs/apk/debug/app-debug.apk`

Install
- Connect the Mi 8 via USB with debugging allowed
- Verify device: `adb devices`
- Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

MIUI settings to avoid background kills
- Autostart: Settings > Apps > Manage apps > DialLog > Autostart ON
- Battery: Settings > Battery > App battery saver > DialLog > No restrictions
- Lock in Recents (optional): open app, open Recents, pull down to lock

Permissions to grant at first launch
- Microphone: RECORD_AUDIO
- Modify audio settings: MODIFY_AUDIO_SETTINGS
- Contacts: READ_CONTACTS
- Phone state: READ_PHONE_STATE

Notes about call audio and Android restrictions
- Modern Android blocks capturing the other party's voice during calls. Your app monitors the microphone (local voice) only. Expect remote audio to be unavailable on most ROMs/firmware, including MIUI.

Test plan (quick)
- Launch and accept all permissions
- Tap Start/Stop button to simulate tracking; verify speaking/listening counters increment
- Open contact selection; verify contacts list loads
- Make/receive a call; move phone to ear; verify proximity toggles mic tracking
- After call, confirm a log entry is created in the list

Troubleshooting
- If app is closed unexpectedly, revisit MIUI Autostart/No restrictions
- If Firebase/Analytics is used, ensure Google Play services is present (Global ROM). On China ROMs without GMS, guard analytics calls
- If installation fails: `adb uninstall com.example.diallog002` then reinstall


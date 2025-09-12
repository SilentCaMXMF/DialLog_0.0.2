# F-Droid Packaging Request: DialLog - Auto Call Tracker

## App Information

**App Name:** DialLog - Auto Call Tracker  
**Package ID:** com.pedroocalado.diallog  
**Version:** 3.0.0 (30000)  
**Source Repository:** https://github.com/SilentCaMXMF/DialLog_0.0.2  
**License:** GPL-3.0  
**Website:** https://github.com/SilentCaMXMF/DialLog_0.0.2  

## Description

DialLog is a privacy-focused call tracking app that automatically monitors speaking and listening patterns during phone calls with selected favorite phone numbers. Perfect for professionals wanting to improve their communication skills through data-driven insights.

**Key Features:**
- Automatic call tracking for individually selected phone numbers (not entire contacts)
- Real-time audio analysis to measure talk vs listen time
- Comprehensive call history and analytics
- Communication style analysis  
- Complete privacy - all data stays on device
- No tracking, no ads, completely open source

## Technical Details

**Build System:** Gradle (Kotlin DSL)  
**Language:** Kotlin  
**Minimum SDK:** 21 (Android 5.0)  
**Target SDK:** 33  
**Architecture:** MVVM with Android Room database  

**Dependencies:** All FOSS dependencies from Maven Central
- androidx.* libraries
- Room database  
- Kotlin Coroutines
- Material Design Components

## Permissions Required

- `RECORD_AUDIO` - For audio level analysis during calls (no recordings made)
- `READ_PHONE_STATE` - To detect call state changes
- `READ_CONTACTS` - To display contact names  
- `CALL_PHONE` - For direct calling from favorites
- `READ_CALL_LOG` - For call detection

## Privacy & Security

- **No internet permissions** - All data stays on device
- **No recordings made** - Only audio levels analyzed
- **No tracking** - Completely private
- **Open source** - Full code transparency
- **GPL-3.0 licensed** - Copyleft protection

## Build Instructions

```bash
git clone https://github.com/SilentCaMXMF/DialLog_0.0.2.git
cd DialLog_0.0.2
./gradlew assembleDebug
```

## Fastlane Metadata

The app includes complete fastlane metadata structure:
- `/fastlane/metadata/android/en-US/title.txt`
- `/fastlane/metadata/android/en-US/short_description.txt`  
- `/fastlane/metadata/android/en-US/full_description.txt`
- `/fastlane/metadata/android/en-US/changelogs/30000.txt`

## Release Tag

Tagged release: `v3.0.0` - https://github.com/SilentCaMXMF/DialLog_0.0.2/releases/tag/v3.0.0

## Additional Notes

This app is particularly valuable for:
- Business professionals monitoring client communication
- Anyone wanting to improve listening skills  
- Privacy-conscious users preferring local-only data storage
- People interested in communication pattern analytics

The app has been thoroughly tested and is ready for production use. All code is open source and available for review.

---

**Submitted by:** Pedro Calado  
**Contact:** Available via GitHub issues  
**Date:** 2025-09-12
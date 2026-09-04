# Android 17 Preview and Weather Entry Fix Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Restore the real wallpaper in the main-screen preview and stop the weather provider/location entries from crashing on Android 17.

**Architecture:** Keep the widget preview UI unchanged, but let the Activity window render the system wallpaper behind the preview instead of reading the wallpaper bitmap through the deprecated `WallpaperManager.getDrawable()` path. Replace SlimAdapter's reflective generic registration with its default injector API so Kotlin compiler changes cannot make the two weather screens fail during `onCreate`.

**Tech Stack:** Android SDK 36, Kotlin, AndroidX AppCompat/Activity Result APIs, SlimAdapter 2.1.2, ADB/logcat device regression.

---

### Task 1: Restore wallpaper-backed preview

**Files:**
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/ui/activities/MainActivity.kt`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/ui/fragments/MainFragment.kt`
- Modify: `app/src/main/res/layout/fragment_app_main.xml`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/ui/fragments/SettingsFragment.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Steps:**
1. Remove the preview's dependency on `READ_EXTERNAL_STORAGE` permission state for Android 13+.
2. Set `FLAG_SHOW_WALLPAPER` on the main Activity window and make only the preview surface transparent when wallpaper preview is enabled.
3. Retain the existing solid color path when the wallpaper preview toggle is disabled.
4. Keep legacy bitmap loading only for pre-Android 14 devices where the API is supported, with exception-safe fallback.
5. Build and verify the preview on the connected Android 17 device.

### Task 2: Fix weather entry crashes

**Files:**
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/ui/activities/tabs/WeatherProviderActivity.kt`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/ui/activities/tabs/CustomLocationActivity.kt`

**Steps:**
1. Replace `register<Constants.WeatherProvider>` with `registerDefault` while preserving all existing row actions and labels.
2. Replace the two generic registrations in `CustomLocationActivity` with one default injector that branches on `String` versus `Address`.
3. Build and launch both Activities on Android 17, verifying no `IllegalArgumentException` from `SlimAdapter.register`.
4. Exercise text search and GPS entry paths without clearing app data.

### Task 3: Verification and handoff

**Steps:**
1. Run `git diff --check` and `./gradlew :app:assembleDebug`.
2. Install the debug APK with `adb install -r` so existing preferences remain intact.
3. Verify the main preview UI, weather provider screen, and custom location screen using ADB UI hierarchy and crash buffer.
4. Commit the implementation and report the APK path, hash, device evidence, and any remaining limitations.

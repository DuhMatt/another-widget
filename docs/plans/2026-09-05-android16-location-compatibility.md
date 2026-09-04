# Android 16 Location Compatibility Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Modernize Another Widget's weather-location flow so it works on Android 16+ without depending on the deprecated `lastLocation`-only foreground-service flow or a runtime “Allow all the time” dialog.

**Architecture:** Obtain a fresh location while the settings activity is visible, persist latitude and longitude, and let background weather refreshes use those saved coordinates. Replace the weather alarm/foreground-service path with WorkManager and keep background location optional rather than making it a prerequisite for weather.

**Tech Stack:** Kotlin 2.1.20, Android API 36, Android Gradle Plugin 8.10+, Gradle 8.11+, AndroidX Activity Result APIs, FusedLocationProviderClient `getCurrentLocation`, WorkManager, Room.

---

### Task 1: Modernize the Android build baseline

**Files:**
- Modify: `build.gradle`
- Modify: `app/build.gradle`
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `gradle.properties`

**Steps:**

1. Upgrade AGP/Gradle/Kotlin and set `compileSdk` and `targetSdk` to 36.
2. Remove JCenter, the obsolete explicit Build Tools version, and RenderScript build configuration.
3. Replace Realm's legacy Gradle transformer with Room while keeping the existing repository-facing behavior.
4. Upgrade WorkManager and AndroidX Activity dependencies needed by the new APIs.
5. Keep the original application ID and version identity for this development line; do not copy the fork's signing configuration or secrets.

### Task 2: Replace Realm event persistence with Room

**Files:**
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/models/Event.kt`
- Create: `app/src/main/java/com/tommasoberlose/anotherwidget/db/EventDao.kt`
- Create: `app/src/main/java/com/tommasoberlose/anotherwidget/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/db/EventRepository.kt`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/AWApplication.kt`

**Steps:**

1. Make `Event` a Room entity with `eventID` as the stable primary key and preserve all fields.
2. Add DAO queries equivalent to the current future-event and event-by-ID queries.
3. Preserve filtering, ordering, next-event state, and public repository methods.
4. Remove Realm initialization and dependencies after the replacement compiles.

### Task 3: Implement foreground current-location acquisition

**Files:**
- Create: `app/src/main/java/com/tommasoberlose/anotherwidget/location/LocationRepository.kt`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/ui/activities/tabs/CustomLocationActivity.kt`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/ui/fragments/tabs/WeatherFragment.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`

**Steps:**

1. Declare coarse and fine location together and request them through `ActivityResultContracts.RequestMultiplePermissions`.
2. Use `FusedLocationProviderClient.getCurrentLocation()` while `CustomLocationActivity` is visible.
3. Handle cancellation, disabled location, missing result, and provider errors without clearing a previously valid location.
4. Save coordinates only after receiving a valid result, then trigger an immediate weather refresh.
5. Explain that Android 11+ grants background location from the app settings page only; do not promise that the normal dialog can show “always allow”.

### Task 4: Remove the obsolete location foreground service path

**Files:**
- Delete: `app/src/main/java/com/tommasoberlose/anotherwidget/services/LocationService.kt`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/helpers/WeatherHelper.kt`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/receivers/WeatherReceiver.kt`
- Create or modify: `app/src/main/java/com/tommasoberlose/anotherwidget/services/WeatherWorker.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Steps:**

1. Make `WeatherHelper.updateWeather()` use saved coordinates directly and never start a location foreground service.
2. Schedule weather refresh with unique WorkManager work and network constraints.
3. Ensure manual selection enqueues immediate work and periodic refreshes use the configured interval.
4. Remove location-service notification and exact-alarm requirements that are no longer needed.
5. Only call the weather provider when coordinates are present; otherwise expose a clear location error.

### Task 5: Verify and package

**Files:**
- Test: all modified Kotlin/XML/Gradle files

**Steps:**

1. Run XML validation and Kotlin/Gradle compilation.
2. Inspect the merged manifest for API 36 permissions and exported components.
3. Build a debug APK and verify its package name, target SDK, and location permissions with APK tooling.
4. Install only after the user confirms the connected phone is ready; then verify permission flow, current location, weather refresh, and behavior after force-stopping the app.

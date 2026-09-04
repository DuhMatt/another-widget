# Android 17 Status Bar Scrim Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement the plan task-by-task.

**Goal:** Keep the main Activity status-bar area opaque while retaining the wallpaper-backed preview below it on Android versions that enforce edge-to-edge.

**Architecture:** Add a top-aligned color scrim to the Activity root and size it from the modern status-bar window inset. Keep the explicit status-bar color as a fallback for older Android versions, but do not rely on it for Android 15+ where the system can ignore it.

**Tech Stack:** Kotlin, AndroidX Core `WindowInsetsCompat`, Android XML view binding.

---

### Task 1: Add and size the status-bar scrim

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/ui/activities/MainActivity.kt`

**Steps:**

1. Add a top-aligned `View` with the application primary color above the transparent content.
2. Update its height from `WindowInsetsCompat.Type.statusBars()` insets.
3. Run `git diff --check`, build the debug APK, and install it with `adb install -r` as requested by the user.
4. Commit the source changes.

# Android 17 Back Navigation Fix Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make Android system back gestures return through Another Widget's existing nested settings navigation instead of finishing the main Activity.

**Architecture:** Register an enabled AndroidX `OnBackPressedCallback` on `MainActivity`, and move the existing two-level navigation logic into a private handler. The callback covers both modern back gestures and legacy back-button dispatch while preserving widget-configuration completion behavior. No device test is performed per user request.

**Tech Stack:** Kotlin, AndroidX Activity `OnBackPressedDispatcher`, AndroidX Navigation.

---

### Task 1: Route modern back dispatch through the existing navigation logic

**Files:**
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/ui/activities/MainActivity.kt`

**Step 1: Register the callback**

Add an enabled `OnBackPressedCallback` after the Activity content view is created.

**Step 2: Preserve current behavior**

Move the existing `onBackPressed()` body into a private handler and remove the deprecated override so the dispatcher is the single entry point.

**Step 3: Check the diff**

Run: `git diff --check`

Expected: no whitespace errors; no device test is run.

**Step 4: Commit**

```bash
git add app/src/main/java/com/tommasoberlose/anotherwidget/ui/activities/MainActivity.kt docs/plans/2026-09-05-android17-back-navigation-fix.md
git commit -m "fix: route modern back gestures through navigation"
```

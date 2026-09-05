# Next Alarm Display Window Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a configurable time window for the Next Clock Alarm glance provider.

**Architecture:** Store the selected window in Kotpref as minutes. Keep alarm-source validation and Shizuku-backed Xiaomi alarm resolution separate from the display-window filter, then apply the filter before formatting and scheduling widget refreshes. Add the selector to the existing `GlanceSettingsDialog` layout and reuse `BottomSheetMenu`.

**Tech Stack:** Kotlin, Android Views/Data Binding, Kotpref, existing `AlarmHelper`, existing Shizuku UserService integration.

---

### Task 1: Define the preference and localized labels

**Files:**
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/global/Constants.kt`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/global/Preferences.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`

Add six minute-valued choices and a 360-minute default. Add English and Simplified Chinese labels for the selector title, subtitle, menu header, and six choices.

### Task 2: Add the selector row to the existing glance settings layout

**Files:**
- Modify: `app/src/main/res/layout/glance_provider_settings_layout.xml`

Add a clickable row after the existing alarm source information row. The row is visible only for the Next Clock Alarm provider and follows the same title/subtitle spacing and selectable background as the existing notification timeout row.

### Task 3: Wire the selector into the dialog

**Files:**
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/components/GlanceSettingsDialog.kt`

Show the current localized window label, open a `BottomSheetMenu<Int>` with the six values, save the selected value, update the row, notify the parent list, and refresh the widget.

### Task 4: Apply the window after resolving the real alarm time

**Files:**
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/helpers/AlarmHelper.kt`
- Modify: `app/src/main/java/com/tommasoberlose/anotherwidget/ui/viewmodels/MainViewModel.kt`

Keep invalid-source detection separate from the range filter. Format alarms only when their remaining time is no greater than the selected window; for a valid alarm outside the window, schedule a refresh at `triggerTime - window` so it appears automatically when it enters the range. Preserve the wrong-source warning for actual source/read failures.

### Task 5: Build, install, and commit

Run the isolated Gradle Debug build, copy the APK to `app/build/outputs/apk/debug/app-debug.apk`, install it on serial `6c39baa8`, inspect the installed package metadata, then commit the source and plan changes. Do not perform functional UI testing on the device.

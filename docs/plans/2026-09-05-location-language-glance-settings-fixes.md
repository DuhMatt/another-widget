# Android 17 location, settings, language, and Glance fixes

## Scope

- Request foreground location first, then guide Android 11+ users to the system permission page for “Allow all the time”; request background location directly on Android 10.
- Restore the settings navigation shared-element transition from the toolbar action to the back action.
- Add a Simplified Chinese/English selector to the preferences screen using the existing translation icon and Android per-app locale support.
- Replace the Glance screen's generic SlimAdapter registration with the non-reflective registration path used by the already-fixed weather screens.

## Verification

- Build the debug APK.
- Push and install the resulting APK on the connected test device.
- Do not run device UI tests in this pass; manual verification remains with the user.

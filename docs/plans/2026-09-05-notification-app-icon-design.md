# Notification app icon display design

## Context

The notification listener was persisting `Notification.smallIcon.resId`. That value represents a notification/status-bar glyph, not the notifying application's launcher icon, and it is not reliable to resolve from the package on newer Android versions.

## Design

- Keep the existing preference field as a non-zero compatibility marker.
- Resolve the icon at render time from the notifying package's `ApplicationInfo.loadIcon()`.
- Use the same helper for both the standard and left-aligned widget layouts.
- Keep the notification text visible if the source package is no longer installed or its icon cannot be loaded.

## Acceptance criteria

- A notification row uses the sending application's icon rather than its white notification glyph.
- Existing stored notification data remains readable after upgrading.
- Icon lookup failures do not crash or hide the notification text.

# Plan: show the Geolocation city in weather settings

**Goal:** Show the city resolved from the current location after the Geolocation option.

**Architecture:** Keep the existing manual-location preference as the mode flag. Store the resolved city separately, update it from the location activity, expose it through `MainViewModel`, and render the combined label in `WeatherFragment`.

**Steps:**

1. Add the separate preference and localized label format.
2. Resolve a city from a successful current location with a safe fallback.
3. Clear stale city data for manual locations and wire the label to both preference values.
4. Compile the debug APK and install it on the connected device; do not run functional tests.

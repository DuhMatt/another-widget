# Geolocation city display design

## Context

The weather settings screen uses an empty `customLocationAdd` value to identify Geolocation. The location activity currently clears that value after a successful location lookup, so the settings row can only display the static Geolocation label.

## Design

- Add a separate `customLocationCity` preference so the existing manual-location and permission logic remains unchanged.
- Reverse-geocode the successful current location and keep only `locality`, with administrative-area fallbacks when the provider does not return a locality.
- Use Android's asynchronous `Geocoder` API on Android 13+ and the legacy API on older versions.
- Treat geocoding failure as non-fatal and keep the original Geolocation label.
- Clear the cached city whenever the user selects a manual address.

## Acceptance criteria

- Geolocation appears as `Geolocation · <city>` (or the localized Chinese equivalent) when a city is available.
- Manual addresses continue to display their full selected address.
- Location permission checks still distinguish manual locations from Geolocation.
- The project compiles without running functional device tests.

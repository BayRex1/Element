# Element Plugin SDK v2

Native DEX plugins can keep using API v1 or implement `ElementPluginEntryV2`.

## V2 capabilities

- Bottom navigation items with emoji or Base64 image icons and badges.
- Open any post or profile from a plugin.
- Read current account and app information.
- Read WebSocket connection state.
- Send authenticated requests through Element's existing encrypted socket transport.
- Subscribe to server events via `host.serverEvents`.
- Persistent per-plugin key/value storage.
- Change the live Element palette and restore it on unload.
- Toasts and external URL intents.
- Open the plugin manager.
- Existing visual-hall overrides remain supported.

## Server API

`serverRequest(type, action, payload)` is intentionally generic so a plugin can use new server endpoints without waiting for a new APK API release.

For common social operations use `ElementPluginServerApi` in `src/ElementPluginServerApi.kt`.

The plugin receives the same authenticated socket transport as the application. Do not store the session key yourself.

## Compatibility

API v1 remains unchanged. API v2 is opt-in through `ElementPluginEntryV2`.

The DEX is loaded with the host application's classloader, so compile plugins against the exact Element build they target.

See `example/ExamplePlugin.kt`.

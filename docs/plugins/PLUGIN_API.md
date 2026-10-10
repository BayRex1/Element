# Element Plugin API (initial host contract)

This document describes the first native host API layer in Element. It is an API foundation, not a promise that arbitrary `.plugin` files are already executable or installable in the app UI.

## Plugin lifecycle

Implement `ElementPlugin` and provide a unique `PluginManifest`:

- `onLoad(context)`: called once after registration; keep hook subscriptions and setup here.
- `onEnable()` / `onDisable()`: called when the user toggles the plugin.
- `onUnload()`: final cleanup; cancel subscriptions and restore any changed state.

## Hooks currently defined

- `AppStarted`
- `ThemeChanged(isDark)`
- `SessionChanged(accountId, isAuthorized)`
- `ScreenOpened(screenId)`
- `PostAction(action, postId)`
- `BeforePublishEvent`: allows a plugin to cancel or replace the outgoing post text before publishing.

## Host APIs

- `subscribe { event -> ... }`: listen for app events.
- `onBeforePublish { event -> ... }`: intercept an outgoing post.
- `emit(event)`: publish an event to other plugins.
- `overrideColor(key, argb)` / `colorOverride(key)`: store named color overrides. The UI must explicitly consume these overrides before they affect rendering.

## Example: add a Halloween suffix to a post

```kotlin
class HalloweenPlugin : ElementPlugin {
    override val manifest = PluginManifest(
        id = "halloween_theme",
        name = "Halloween Theme",
        version = "1.0.0",
        author = "@you",
        description = "Halloween accents for Element"
    )

    private var subscription: PluginSubscription? = null
    private var context: PluginContext? = null

    override fun onLoad(context: PluginContext) {
        this.context = context
        subscription = context.onBeforePublish { event ->
            if (!event.cancelled && event.replacementText.isNotBlank()) {
                event.replacementText += " 🎃"
            }
        }
    }

    override fun onDisable() {
        subscription?.cancel()
        subscription = null
    }

    override fun onUnload() {
        subscription?.cancel()
        subscription = null
        context?.overrideColor("accent", null)
    }
}
```

## Porting a Telegram plugin

A Telegram plugin that imports `org.telegram.ui.*`, `base_plugin`, `MethodHook`, or `hook_utils` cannot run unchanged in Element. Element uses Jetpack Compose and its own models. Port the behavior to Element events and color keys; do not call Telegram classes or rely on Telegram's internal method hooks.

## Security and compatibility

Plugins execute code with app-process privileges if a future script runtime is enabled. Only install plugins from sources you trust. Keep the public API narrow: don't expose session keys, authentication repositories, arbitrary network access, or raw Activity internals to plugins by default. Add capabilities deliberately and version the API before promising compatibility.

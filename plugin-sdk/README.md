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

## Python Script Plugins (API v3)

Element now supports native script plugins as plain `.plugin` source files. A user installs one file; no Kotlin, Android Studio, Gradle, DEX or Base64 is required.

Example:

    from element_plugin import BasePlugin

    __id__ = "hello"
    __name__ = "Hello Element"
    __description__ = "Example script plugin"
    __version__ = "1.0.0"
    __author__ = "Element"
    __icon__ = "post/144376"

    class Plugin(BasePlugin):
        def on_plugin_load(self):
            self.toast("Hello from Element!")
            self.ui.bottom_button(
                id="hello",
                title="Hello",
                icon="👋",
                on_click=lambda: self.toast("Button works!")
            )
            self.hook("post.opened", self.on_post)

        def on_post(self, event):
            self.toast("Opened post #%s" % event.get("post_id"))

Supported script APIs include:
- `BasePlugin`
- `ui.toast`, `ui.bottom_button`, `ui.open_post`, `ui.open_profile`, `ui.open_url`
- `posts.get/create/edit/delete/like/dislike/react/comment`
- `profiles.get/open`
- `server.request`, `server.on`
- `hook` / `unhook`
- `storage.get/set/remove/clear`
- `theme.set/reset`
- `user`, `app`, and `connection_state`
- `__icon__ = "post/<id>"` for using a post image as the plugin icon.

Legacy JSON + DEX plugins remain supported.

## Python Plugin UI API v4

Script plugins can create full Element-style screens from Python. UI is declarative and callbacks receive Python dictionaries.

Example:

    def open_test(self):
        self.ui.screen(
            id="test",
            title="Plugin Test",
            content=[
                self.ui.title("Element Plugin UI"),
                self.ui.text("Полноценный экран плагина"),
                self.ui.card(
                    title="Настройки",
                    text="Пример компонентов",
                    children=[
                        self.ui.button("Нажми меня", on_click=self.clicked),
                        self.ui.switch("Тестовый режим", value=True, on_change=self.changed),
                        self.ui.input("Имя", value="", placeholder="Введите имя", on_change=self.name_changed),
                    ],
                ),
            ],
        )

Available UI builders: text, title, button, soft_button, switch, checkbox, input, divider, spacer, card, column, row, screen, close.

Callback payloads: button {}, switch/checkbox {"value": bool}, input {"value": str}.

The screen uses Element's native visual palette and the bottom navigation is hidden while a plugin screen is open.

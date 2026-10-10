# NimarkoGram Python plugin compatibility

Element's plugin runtime can now load the common high-level parts of NimarkoGram-style
Python plugins:

- `.plugin` Python files using `from base_plugin import BasePlugin`
- common metadata fields (`__id__`, `__name__`, `__description__`, `__version__`, `__author__`, `__icon__`)
- persistent per-plugin settings through `get_setting`, `set_setting`, `export_settings`, and `import_settings`
- lifecycle callbacks (`on_app_event`) for start, pause, resume, and stop
- basic `ui.settings` model imports and a renderer for legacy `create_settings()` lists
- class lookup through `hook_utils.find_class`
- basic `android_utils.log`, `run_on_ui_thread`, and `is_on_ui_thread` helpers

## Important limits

This is a compatibility layer, not a full port of NimarkoGram's Android plugin engine.
Element does **not** yet ship its Xposed/Pine method-hook backend. Consequently,
`hook_method`, `hook_all_methods`, and `hook_all_constructors` do not install hooks.
They show a notice and return `None`. The current runtime also does not implement
Telegram-specific message/request/update interception, Android native custom settings
views, Telegram context menus, or the full NimarkoGram helper-module surface.

Plugins relying on those features need native Element-side implementations before
they can behave equivalently. The compatibility API deliberately avoids pretending
that unsupported hooks succeeded.

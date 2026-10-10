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

## Native method-hook backend

Element now includes Pine 0.3.0 and a Java adapter for Python callbacks. The API supports
`hook_method`, `hook_all_methods`, `hook_all_constructors`, method replacements, and
explicit unhooking. Hooks are installed in Element's own process; they do not affect other
apps. Plugin hooks are removed when their plugin unloads.

For safety, this backend is enabled only on arm64-v8a devices running Android API 26-35.
Pine 0.3.0 does not declare x86_64 or Android 16/API 36+ support. Unsupported devices
report the limitation instead of attempting native initialization. Priority values are
accepted for source compatibility but currently do not reorder Pine callbacks.

## Important limits

This is still not a complete port of NimarkoGram's Android plugin engine. Telegram-specific
send-message/request/update interception, resource hooks, Telegram context menus, native
custom Android settings views, and the full NimarkoGram helper-module surface are not
automatically provided by the generic method-hook backend. Those require separate,
Element-specific integrations.

import os
import traceback
from element_plugin import BasePlugin

_loaded = {}

def load_plugin(path, bridge):
    namespace = {
        "__file__": path,
        "__name__": "__element_plugin__",
        "__builtins__": __builtins__,
    }
    with open(path, "r", encoding="utf-8") as source:
        code = compile(source.read(), path, "exec")
    exec(code, namespace, namespace)

    candidates = []
    for value in namespace.values():
        if isinstance(value, type) and issubclass(value, BasePlugin) and value is not BasePlugin:
            candidates.append(value)
    if not candidates:
        raise RuntimeError("Плагин не содержит класс BasePlugin")
    if len(candidates) > 1:
        named = namespace.get("Plugin")
        plugin_class = named if isinstance(named, type) else candidates[0]
    else:
        plugin_class = candidates[0]

    instance = plugin_class(bridge)
    metadata = {
        "id": namespace.get("__id__", os.path.splitext(os.path.basename(path))[0]),
        "name": namespace.get("__name__", ""),
        "description": namespace.get("__description__", ""),
        "version": namespace.get("__version__", "1.0.0"),
        "author": namespace.get("__author__", "Unknown"),
        "icon": namespace.get("__icon__", "🧩"),
    }
    for key, value in metadata.items():
        if value not in (None, ""):
            setattr(instance, key, value)
    try:
        import plugin_settings
        plugin_settings.bind(instance)
    except Exception:
        pass
    instance._run_load()
    _loaded[id(instance)] = instance
    return instance

def unload_plugin(instance):
    if instance is None:
        return
    try:
        instance._run_unload()
    finally:
        _loaded.pop(id(instance), None)

def open_legacy_settings(instance):
    """Render create_settings() models through Element's declarative UI."""
    if instance is None:
        return False
    try:
        settings = instance.create_settings() or []
    except Exception as exc:
        instance.ui.toast("Не удалось создать настройки плагина: " + str(exc), True)
        return False

    nodes = []
    for item in settings:
        kind = str(getattr(item, "type", "") or "").lower()
        text = str(getattr(item, "text", "") or "")
        subtext = str(getattr(item, "subtext", "") or "")
        key = str(getattr(item, "key", "") or "")

        if kind == "header":
            nodes.append(instance.ui.title(text))
        elif kind == "divider":
            nodes.append(instance.ui.divider())
        elif kind == "switch":
            default = bool(getattr(item, "default", False))
            value = bool(instance.get_setting(key, default))
            def changed(event, _item=item, _key=key):
                value = bool(event.get("value", False))
                if _key:
                    instance.set_setting(_key, value)
                callback = getattr(_item, "on_change", None)
                if callback:
                    callback(value)
            nodes.append(instance.ui.switch(text, value=value, on_change=changed, secondary=subtext))
        elif kind == "selector":
            options = list(getattr(item, "items", []) or [])
            default = int(getattr(item, "default", 0) or 0)
            selected = int(instance.get_setting(key, default) or 0)
            selected = max(0, min(selected, max(0, len(options) - 1)))
            def changed(event, _item=item, _key=key):
                try:
                    value = int(event.get("value", 0))
                except Exception:
                    value = 0
                if _key:
                    instance.set_setting(_key, value)
                callback = getattr(_item, "on_change", None)
                if callback:
                    callback(value)
            nodes.append({
                "type": "selector", "text": text, "secondary": subtext,
                "value": str(selected), "options": [str(option) for option in options],
                "callback_id": instance.ui._callback(changed)
            })
        elif kind in ("input", "edit_text"):
            default = str(getattr(item, "default", "") or "")
            value = str(instance.get_setting(key, default) or "")
            def changed(event, _item=item, _key=key):
                value = str(event.get("value", ""))
                if _key:
                    instance.set_setting(_key, value)
                callback = getattr(_item, "on_change", None)
                if callback:
                    callback(value)
            label = text or str(getattr(item, "hint", "") or "")
            nodes.append(instance.ui.input(label, value=value, placeholder=subtext, on_change=changed))
        elif kind == "text":
            callback = getattr(item, "on_click", None)
            if callback:
                nodes.append(instance.ui.button(text, on_click=lambda event, _callback=callback: _callback(None)))
            else:
                nodes.append(instance.ui.text(text, secondary=subtext))
        elif kind == "custom":
            nodes.append(instance.ui.text(text or "Пользовательский элемент настроек не поддерживается", secondary=subtext))
        else:
            nodes.append(instance.ui.text(text or str(item)))

    if not nodes:
        nodes.append(instance.ui.text("У этого плагина нет дополнительных настроек."))
    instance.ui.screen(
        id="legacy_plugin_settings",
        title=str(getattr(instance, "name", "") or "Настройки плагина"),
        content=nodes,
    )
    return True

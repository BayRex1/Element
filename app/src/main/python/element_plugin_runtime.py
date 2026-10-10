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

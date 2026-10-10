"""Persistent settings compatibility backed by Element's per-plugin storage."""
import json

_bound_plugins = {}


def bind(plugin):
    plugin_id = getattr(plugin, "id", "")
    if plugin_id:
        _bound_plugins[str(plugin_id)] = plugin


def unbind(plugin_id):
    _bound_plugins.pop(str(plugin_id), None)


def get_setting(plugin_id, key, default=None):
    plugin = _bound_plugins.get(str(plugin_id))
    return default if plugin is None else plugin.get_setting(key, default)


def set_setting(plugin_id, key, value):
    plugin = _bound_plugins.get(str(plugin_id))
    if plugin is None:
        return False
    plugin.set_setting(key, value)
    return True


def get_all_settings(plugin_id):
    plugin = _bound_plugins.get(str(plugin_id))
    return {} if plugin is None else plugin.export_settings()


def set_all_settings(plugin_id, values):
    plugin = _bound_plugins.get(str(plugin_id))
    if plugin is None:
        return False
    plugin.import_settings(values)
    return True

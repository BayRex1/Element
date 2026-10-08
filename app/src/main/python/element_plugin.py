"""
Element Plugin Python API.
User-facing script plugins are plain .plugin files containing Python.
"""

class _Storage:
    def __init__(self, bridge):
        self._bridge = bridge
    def get(self, key, default=None):
        value = self._bridge.storage_get(str(key))
        return default if value is None else value
    def set(self, key, value):
        self._bridge.storage_put(str(key), str(value))
    def remove(self, key):
        self._bridge.storage_remove(str(key))
    def clear(self):
        self._bridge.storage_clear()


class _Theme:
    def __init__(self, bridge):
        self._bridge = bridge
    def set(self, **kwargs):
        # Pass JSON instead of a Python dict: Chaquopy does not reliably
        # convert arbitrary Python dicts to Kotlin Map parameters.
        import json
        self._bridge.set_theme_json(json.dumps(kwargs))
    def reset(self):
        self._bridge.reset_theme()


class _UI:
    def __init__(self, plugin):
        self._plugin = plugin
        self._counter = 0

    def toast(self, message, long=False):
        self._plugin._bridge.show_toast(str(message), bool(long))

    def open_post(self, post_id):
        return bool(self._plugin._bridge.open_post(int(post_id)))

    def open_profile(self, username):
        return bool(self._plugin._bridge.open_profile(str(username)))

    def open_url(self, url):
        return bool(self._plugin._bridge.open_url(str(url)))

    def bottom_button(self, id, title, icon="🧩", on_click=None, badge=None):
        return self._plugin.add_bottom_button(id, title, icon, on_click, badge)

    def _callback(self, callback):
        if callback is None:
            return None
        self._counter += 1
        callback_id = "ui_%s" % self._counter
        return self._plugin._bridge.register_ui_callback(callback_id, callback)

    def text(self, text, secondary="", size=None, bold=False):
        return {"type": "title" if bold else "text", "text": str(text), "secondary": str(secondary)}

    def title(self, text):
        return {"type": "title", "text": str(text)}

    def button(self, text, on_click=None, enabled=True):
        return {"type": "button", "text": str(text), "enabled": bool(enabled), "callback_id": self._callback(on_click)}

    def soft_button(self, text, on_click=None, enabled=True):
        return {"type": "soft_button", "text": str(text), "enabled": bool(enabled), "callback_id": self._callback(on_click)}

    def switch(self, text, value=False, on_change=None, secondary=""):
        return {"type": "switch", "text": str(text), "secondary": str(secondary), "checked": bool(value), "callback_id": self._callback(on_change)}

    def checkbox(self, text, value=False, on_change=None):
        return {"type": "checkbox", "text": str(text), "checked": bool(value), "callback_id": self._callback(on_change)}

    def input(self, label, value="", placeholder="", on_change=None):
        return {"type": "input", "text": str(label), "value": str(value), "secondary": str(placeholder), "callback_id": self._callback(on_change)}

    def divider(self):
        return {"type": "divider"}

    def spacer(self, height=12):
        return {"type": "spacer", "value": str(height)}

    def card(self, title="", text="", children=None):
        return {"type": "card", "text": str(title), "secondary": str(text), "children": list(children or [])}

    def column(self, children):
        return {"type": "column", "children": list(children or [])}

    def row(self, children):
        return {"type": "row", "children": list(children or [])}

    def screen(self, id, title, content):
        import json
        payload = {
            "id": str(id),
            "title": str(title),
            "nodes": list(content or []),
        }
        self._plugin._bridge.show_ui_screen_json(json.dumps(payload, ensure_ascii=False))

    def close(self):
        self._plugin._bridge.close_ui_screen()


class _Posts:
    def __init__(self, plugin):
        self._plugin = plugin
    def request(self, action, payload=None):
        import json
        return json.loads(self._plugin._bridge.server_request("social", str(action), json.dumps(payload or {}), 60000))
    def get(self, post_id):
        return self.request("load_post", {"post_id": int(post_id)})
    def comments(self, post_id):
        return self.request("comments/load", {"post_id": int(post_id)})
    def create(self, text):
        return self.request("posts/add", {"text": str(text)})
    def edit(self, post_id, text):
        return self.request("posts/edit", {"post_id": int(post_id), "text": str(text)})
    def delete(self, post_id):
        return self.request("posts/delete", {"post_id": int(post_id)})
    def like(self, post_id):
        return self.request("posts/like", {"post_id": int(post_id)})
    def dislike(self, post_id):
        return self.request("posts/dislike", {"post_id": int(post_id)})
    def react(self, post_id, reaction):
        return self.request("post/set_reaction", {"post_id": int(post_id), "reaction": str(reaction)})
    def comment(self, post_id, text):
        return self.request("comments/add", {"post_id": int(post_id), "text": str(text)})


class _Profiles:
    def __init__(self, plugin):
        self._plugin = plugin
    def get(self, username):
        import json
        return json.loads(self._plugin._bridge.server_request(
            "social", "profile/load", json.dumps({"username": str(username)}), 60000
        ))
    def open(self, username):
        return bool(self._plugin._bridge.open_profile(str(username)))


class _Server:
    def __init__(self, plugin):
        self._plugin = plugin
    def request(self, type, action, payload=None, timeout_ms=60000):
        import json
        return json.loads(self._plugin._bridge.server_request(str(type), str(action), json.dumps(payload or {}), int(timeout_ms)))
    def on(self, event, callback):
        return self._plugin.hook(event, callback)


class BasePlugin:
    def __init__(self, bridge):
        self._bridge = bridge
        self._hooks = {}
        self.storage = _Storage(bridge)
        self.theme = _Theme(bridge)
        self.ui = _UI(self)
        self.posts = _Posts(self)
        self.profiles = _Profiles(self)
        self.server = _Server(self)

    @property
    def user(self):
        import json
        return json.loads(self._bridge.current_user_json())

    @property
    def app(self):
        import json
        return json.loads(self._bridge.app_info_json())

    @property
    def connection_state(self):
        return self._bridge.connection_state()

    def toast(self, message, long=False):
        self.ui.toast(message, long)

    def add_bottom_button(self, id, title, icon="🧩", on_click=None, badge=None):
        callback = on_click if on_click is not None else (lambda: None)
        return self._bridge.register_bottom_button(str(id), str(title), str(icon), callback, badge)

    def remove_bottom_button(self, id):
        self._bridge.unregister_bottom_button(str(id))

    def hook(self, event, callback):
        self._hooks.setdefault(str(event), []).append(callback)
        self._bridge.register_hook(str(event), callback)
        return callback

    def unhook(self, event, callback):
        callbacks = self._hooks.get(str(event), [])
        if callback in callbacks:
            callbacks.remove(callback)
        self._bridge.unregister_hook(str(event), callback)

    def open_post(self, post_id):
        return self.ui.open_post(post_id)

    def open_profile(self, username):
        return self.ui.open_profile(username)

    def open_url(self, url):
        return self.ui.open_url(url)

    def on_plugin_load(self):
        pass

    def on_plugin_unload(self):
        pass

    def _run_load(self):
        self.on_plugin_load()

    def _run_unload(self):
        try:
            self.on_plugin_unload()
        finally:
            for event, callbacks in list(self._hooks.items()):
                for callback in list(callbacks):
                    self._bridge.unregister_hook(event, callback)
            self._hooks.clear()

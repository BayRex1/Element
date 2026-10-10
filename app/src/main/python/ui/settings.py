"""Import-compatible settings models for NimarkoGram-style Python plugins.

Element's current declarative plugin screen supports text, buttons, switches,
checkboxes, inputs, cards, rows, columns and dividers. These data models are
provided so legacy plugins can import their settings definitions; automatic
rendering of legacy create_settings() lists is not implemented yet.
"""
from dataclasses import dataclass, field
from typing import Any, Callable, List, Optional


@dataclass
class Switch:
    key: str
    text: str
    default: bool
    subtext: str = ""
    icon: str = ""
    on_change: Optional[Callable] = field(default=None, compare=False, repr=False)
    type: str = field(default="switch", init=False)


@dataclass
class Selector:
    key: str
    text: str
    default: int
    items: List[str]
    icon: str = ""
    on_change: Optional[Callable] = field(default=None, compare=False, repr=False)
    type: str = field(default="selector", init=False)


@dataclass
class Input:
    key: str
    text: str
    default: str = ""
    subtext: str = ""
    icon: str = ""
    on_change: Optional[Callable] = field(default=None, compare=False, repr=False)
    type: str = field(default="input", init=False)


@dataclass
class Text:
    text: str
    subtext: str = ""
    icon: str = ""
    accent: bool = False
    red: bool = False
    on_click: Optional[Callable] = field(default=None, compare=False, repr=False)
    type: str = field(default="text", init=False)


@dataclass
class Header:
    text: str
    type: str = field(default="header", init=False)


@dataclass
class Divider:
    text: str = ""
    type: str = field(default="divider", init=False)


@dataclass
class EditText:
    key: str
    hint: str
    default: str = ""
    multiline: bool = False
    max_length: int = 256
    mask: Optional[str] = None
    on_change: Optional[Callable] = field(default=None, compare=False, repr=False)
    type: str = field(default="edit_text", init=False)


@dataclass
class Custom:
    create_view: Optional[Callable] = field(default=None, compare=False, repr=False)
    bind_view: Optional[Callable] = field(default=None, compare=False, repr=False)
    view: Any = field(default=None, compare=False, repr=False)
    item: Any = None
    text: str = ""
    subtext: str = ""
    divider: bool = False
    on_click: Optional[Callable] = field(default=None, compare=False, repr=False)
    type: str = field(default="custom", init=False)


class SimpleSettingFactory:
    """Minimal import-compatible factory for legacy custom setting rows."""

    def __init__(self, custom_name=None, create_view_fn=None, bind_view_fn=None,
                 attached_view_handler=None, equals_handler=None,
                 content_equals_handler=None, is_clickable=True, is_shadow=False,
                 on_click_handler=None, on_long_click_handler=None, link_alias=None):
        self.custom_name = custom_name
        self.create_view_fn = create_view_fn
        self.bind_view_fn = bind_view_fn
        self.attached_view_handler = attached_view_handler
        self.equals_handler = equals_handler
        self.content_equals_handler = content_equals_handler
        self.is_clickable = is_clickable
        self.is_shadow = is_shadow
        self.on_click_handler = on_click_handler
        self.on_long_click_handler = on_long_click_handler
        self.link_alias = link_alias

    def create(self, *args, **kwargs):
        return self(*args, **kwargs)

    def __call__(self, *args, **kwargs):
        factory = self
        def create_view(context):
            if factory.create_view_fn is None:
                return None
            try:
                return factory.create_view_fn(context, *args, **kwargs)
            except TypeError:
                return factory.create_view_fn(context)
        def bind_view(view):
            if factory.bind_view_fn is None:
                return
            try:
                factory.bind_view_fn(view, *args, **kwargs)
            except TypeError:
                factory.bind_view_fn(view)
        row = Custom(
            create_view=create_view if factory.create_view_fn is not None else None,
            bind_view=bind_view if factory.bind_view_fn is not None else None,
            on_click=factory.on_click_handler if factory.is_clickable else None,
            text=factory.custom_name or "",
            divider=not factory.is_shadow,
        )
        row.custom_name = factory.custom_name
        row.attached_view_handler = factory.attached_view_handler
        row.equals_handler = factory.equals_handler
        row.content_equals_handler = factory.content_equals_handler
        row.is_clickable = factory.is_clickable
        row.is_shadow = factory.is_shadow
        row.link_alias = factory.link_alias
        return row

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

"""NimarkoGram/exteraGram Python plugin API compatibility for Element.

The high-level plugin lifecycle, storage and event API are adapted to Element.
Java method hooks are backed by Element's in-process Pine adapter on supported arm64
Android versions. Xposed resource hooks and unrelated Telegram internals are not included.
"""
from dataclasses import dataclass
from enum import Enum
from typing import Any, Callable, Optional

from element_plugin import BasePlugin


class HookStrategy(Enum):
    CANCEL = "CANCEL"
    MODIFY = "MODIFY"
    DEFAULT = "DEFAULT"
    MODIFY_FINAL = "MODIFY_FINAL"


@dataclass
class HookResult:
    strategy: HookStrategy = HookStrategy.DEFAULT
    request: Any = None
    response: Any = None
    update: Any = None
    updates: Any = None
    error: Any = None
    params: Any = None


class XposedHook:
    pass


class BaseHook(XposedHook):
    def before_hooked_method(self, param):
        return None

    def after_hooked_method(self, param):
        return None


class MethodHook(BaseHook):
    pass


class MethodReplacement(BaseHook):
    def replace_hooked_method(self, param):
        raise NotImplementedError


class AppEvent:
    # String constants match the event values passed by Element's runtime.
    START = "START"
    STOP = "STOP"
    PAUSE = "PAUSE"
    RESUME = "RESUME"


class MenuItemType(Enum):
    MESSAGE_CONTEXT_MENU = "message_context_menu"
    DRAWER_MENU = "drawer_menu"
    CHAT_ACTION_MENU = "chat_action_menu"
    PROFILE_ACTION_MENU = "profile_action_menu"


@dataclass
class MenuItemData:
    menu_type: MenuItemType
    text: str
    on_click: Callable
    item_id: Optional[str] = None
    icon: Optional[str] = None
    subtext: Optional[str] = None
    condition: Optional[str] = None
    priority: int = 0


class HookFilter:
    RESULT_IS_NULL = ("result_is_null",)
    RESULT_IS_TRUE = ("result_is_true",)
    RESULT_IS_FALSE = ("result_is_false",)
    RESULT_NOT_NULL = ("result_not_null",)

    @staticmethod
    def ResultIsInstanceOf(clazz):
        return ("result_is_instance_of", clazz)

    @staticmethod
    def ResultEqual(value):
        return ("result_equal", value)

    @staticmethod
    def ResultNotEqual(value):
        return ("result_not_equal", value)

    @staticmethod
    def ArgumentIsNull(index):
        return ("argument_is_null", index)

    @staticmethod
    def ArgumentIsTrue(index):
        return ("argument_is_true", index)

    @staticmethod
    def ArgumentIsFalse(index):
        return ("argument_is_false", index)

    @staticmethod
    def ArgumentNotNull(index):
        return ("argument_not_null", index)

    @staticmethod
    def ArgumentIsInstanceOf(index, clazz):
        return ("argument_is_instance_of", index, clazz)

    @staticmethod
    def ArgumentEqual(index, value):
        return ("argument_equal", index, value)

    @staticmethod
    def ArgumentNotEqual(index, value):
        return ("argument_not_equal", index, value)

    @staticmethod
    def Condition(condition, object=None):
        return ("condition", condition, object)

    @staticmethod
    def Or(*filters):
        return ("or",) + tuple(filters)


def hook_filters(*filters):
    def decorate(func):
        func.__hook_filters__ = list(filters)
        return func
    return decorate


__all__ = [
    "BasePlugin", "BaseHook", "XposedHook", "MethodHook", "MethodReplacement",
    "HookResult", "HookStrategy", "AppEvent", "MenuItemType", "MenuItemData",
    "HookFilter", "hook_filters",
]

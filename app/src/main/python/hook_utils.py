"""Small compatibility helpers for Python plugins running in Element.

This module resolves Java classes, but does not install hooks. Method-hook
installation requires a dedicated backend that Element does not currently ship.
"""


def find_class(name, class_loader=None):
    if not name:
        return None
    try:
        from java.lang import Class
        if class_loader is not None:
            return class_loader.loadClass(str(name))
        return Class.forName(str(name))
    except Exception:
        return None


def get_class(name, class_loader=None):
    return find_class(name, class_loader)


def findClass(name, class_loader=None):
    return find_class(name, class_loader)

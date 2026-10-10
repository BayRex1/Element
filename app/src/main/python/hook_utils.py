"""Small compatibility helpers for Python plugins running in Element.

This module resolves Java classes. Java method hooks are installed through the native
Pine backend on supported arm64 Android versions; unsupported devices report an error.
"""


def find_class(name, class_loader=None):
    if not name:
        return None
    try:
        from java.lang import Class, Thread
        loader = class_loader
        if loader is None:
            loader = Thread.currentThread().getContextClassLoader()
        if loader is not None:
            try:
                return loader.loadClass(str(name))
            except Exception:
                pass
        return Class.forName(str(name))
    except Exception:
        return None


def get_class(name, class_loader=None):
    return find_class(name, class_loader)


def findClass(name, class_loader=None):
    return find_class(name, class_loader)

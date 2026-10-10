"""Java class lookup helpers for Element plugin scripts.

Lookup tries the supplied loader, thread context loader, then the application
loader before falling back to Class.forName. A missing class returns None.
Callers must check the result before accessing reflection methods.
"""


def find_class(name, class_loader=None):
    if not name:
        return None
    try:
        from java.lang import Class, Thread
    except Exception:
        return None

    class_name = str(name).replace("/", ".")
    loaders = []
    if class_loader is not None:
        loaders.append(class_loader)
    try:
        context_loader = Thread.currentThread().getContextClassLoader()
        if context_loader is not None and context_loader not in loaders:
            loaders.append(context_loader)
    except Exception:
        pass
    try:
        from android.app import ActivityThread
        app = ActivityThread.currentApplication()
        if app is not None:
            app_loader = app.getClassLoader()
            if app_loader is not None and app_loader not in loaders:
                loaders.append(app_loader)
    except Exception:
        pass

    for loader in loaders:
        try:
            found = loader.loadClass(class_name)
            if found is not None:
                return found
        except Exception:
            continue

    try:
        return Class.forName(class_name)
    except Exception:
        return None


def get_class(name, class_loader=None):
    return find_class(name, class_loader)


def findClass(name, class_loader=None):
    return find_class(name, class_loader)

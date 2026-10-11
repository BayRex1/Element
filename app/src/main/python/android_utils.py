"""Small Android utility compatibility surface for Python plugins in Element."""


def log(data):
    try:
        print(str(data))
    except Exception:
        pass


def is_on_ui_thread():
    try:
        from android.os import Looper
        return Looper.myLooper() == Looper.getMainLooper()
    except Exception:
        return False


def run_on_ui_thread(func, delay=0):
    try:
        from android.os import Handler, Looper
        handler = Handler(Looper.getMainLooper())
        if int(delay or 0) > 0:
            handler.postDelayed(func, int(delay))
        else:
            handler.post(func)
        return True
    except Exception as exc:
        log("run_on_ui_thread unavailable: " + str(exc))
        return False

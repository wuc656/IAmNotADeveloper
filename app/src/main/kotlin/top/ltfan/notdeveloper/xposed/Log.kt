package top.ltfan.notdeveloper.xposed

import de.robv.android.xposed.XposedBridge

object Log {
    const val TAG = "PF"

    fun d(message: String, throwable: Throwable? = null) {
        log("D", message, throwable)
    }

    fun i(message: String, throwable: Throwable? = null) {
        log("I", message, throwable)
    }

    fun w(message: String, throwable: Throwable? = null) {
        log("W", message, throwable)
    }

    private fun log(level: String, message: String, throwable: Throwable? = null) {
        XposedBridge.log("[$level] $TAG: $message")
        if (throwable != null) {
            XposedBridge.log(throwable)
        }
    }
}

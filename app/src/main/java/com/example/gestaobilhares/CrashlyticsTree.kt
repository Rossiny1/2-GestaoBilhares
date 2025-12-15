package com.example.gestaobilhares

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * A Timber Tree that logs important events to Crashlytics.
 * Only logs WARN and ERROR.
 */
class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
            return
        }

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.log(message)

        if (t != null) {
            crashlytics.recordException(t)
        }
    }
}

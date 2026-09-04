package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashlyticsHelper {
    private fun instance(context: Context): FirebaseCrashlytics? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseCrashlytics.getInstance()
        } catch (_: IllegalStateException) {
            null
        }
    }

    fun configure(context: Context, enabled: Boolean) {
        instance(context)?.setCrashlyticsCollectionEnabled(enabled)
    }

    fun setCustomKey(context: Context, key: String, value: Int) {
        instance(context)?.setCustomKey(key, value)
    }

    fun recordException(context: Context, exception: Throwable) {
        instance(context)?.recordException(exception)
    }

    fun sendUnsentReports(context: Context) {
        instance(context)?.sendUnsentReports()
    }
}

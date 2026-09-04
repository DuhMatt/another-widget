package com.tommasoberlose.anotherwidget

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.CrashlyticsHelper
import net.danlew.android.joda.JodaTimeAndroid

class AWApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firebase crashlitycs
        CrashlyticsHelper.configure(this, !BuildConfig.DEBUG)

        // Preferences
        Kotpref.init(this)

        // Dark theme
        AppCompatDelegate.setDefaultNightMode(Preferences.darkThemePreference)

        calibrateVersions()
    }

    private fun calibrateVersions() {
        // 2.0 Tolerance
        if (Preferences.clockTextSize > 50f) {
            Preferences.clockTextSize = 32f
        }

        if (Preferences.textMainSize > 36f) {
            Preferences.textMainSize = 32f
        }

        if (Preferences.textSecondSize > 28f) {
            Preferences.textSecondSize = 24f
        }
    }
}

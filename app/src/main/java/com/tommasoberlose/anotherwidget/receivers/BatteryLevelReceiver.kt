package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.BatteryHelper
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget

class BatteryLevelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val wasCharging = Preferences.isCharging
        val wasBatteryLevelLow = Preferences.isBatteryLevelLow
        val previousBatteryLevel = Preferences.lastBatteryLevel

        when (intent.action) {
            Intent.ACTION_BATTERY_LOW,
            Intent.ACTION_BATTERY_OKAY,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BATTERY_CHANGED -> {
                // Read the actual current state instead of trusting the broadcast
                // action. Some vendor ROMs can delay or coalesce power broadcasts.
                BatteryHelper.updateBatteryInfo(context)
            }
            else -> return
        }

        val currentBatteryLevel = BatteryHelper.getBatteryLevel(context)
        Preferences.lastBatteryLevel = currentBatteryLevel

        if (wasCharging != Preferences.isCharging ||
            wasBatteryLevelLow != Preferences.isBatteryLevelLow ||
            previousBatteryLevel != currentBatteryLevel
        ) {
            MainWidget.updateWidget(context)
        }
    }

}

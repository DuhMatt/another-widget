package com.tommasoberlose.anotherwidget.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.text.format.DateFormat
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.utils.setExactIfAllowed
import java.text.SimpleDateFormat
import java.util.*

object AlarmHelper {
    fun getNextAlarm(context: Context): String {
        val triggerTime = getNextAlarmTime(context)
        val remaining = triggerTime?.minus(System.currentTimeMillis()) ?: 0L
        return if (triggerTime != null && remaining > 0L) {
            setTimeout(context, triggerTime)
            "%s %s".format(
                SimpleDateFormat("EEE", Locale.getDefault()).format(triggerTime),
                DateFormat.getTimeFormat(context).format(Date(triggerTime))
            )
        } else {
            cancelTimeout(context)
            ""
        }
    }

    fun isAlarmProbablyWrong(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager.nextAlarmClock == null) return false
        return getNextAlarmTime(context) == null
    }

    private fun getNextAlarmTime(context: Context): Long? {
        val alarm = getValidNextAlarm(context) ?: return null
        if (alarm.showIntent.creatorPackage == XIAOMI_DESKCLOCK_PACKAGE &&
            ShizukuAlarmHelper.isUsable(context)
        ) {
            // Xiaomi publishes ALARM_ARRIVING as nextAlarmClock. Shizuku lets
            // us read the real ALARM_ALERT entry from dumpsys alarm instead.
            return ShizukuAlarmHelper.getNextAlarmAlertTime(context)
        }
        return alarm.triggerTime
    }

    private fun getValidNextAlarm(context: Context): AlarmManager.AlarmClockInfo? {
        val alarm = (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).nextAlarmClock
            ?: return null
        if (alarm.triggerTime <= System.currentTimeMillis()) return null

        val creatorPackage = alarm.showIntent.creatorPackage ?: return null
        val packageManager = context.packageManager
        val clockPackages = listOf(
            AlarmClock.ACTION_SHOW_ALARMS,
            AlarmClock.ACTION_SET_ALARM
        ).flatMap { action ->
            packageManager.queryIntentActivities(
                Intent(action),
                PackageManager.MATCH_DEFAULT_ONLY
            )
        }.map { it.activityInfo.packageName }.toSet()

        return alarm.takeIf { creatorPackage in clockPackages }
    }

    private fun setTimeout(context: Context, trigger: Long) {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val intent = Intent(context, UpdatesReceiver::class.java).apply {
                action = Actions.ACTION_ALARM_UPDATE
            }
            cancel(getUpdatePendingIntent(context, intent))
            setExactIfAllowed(
                AlarmManager.RTC,
                trigger,
                getUpdatePendingIntent(context, intent)
            )
        }
    }

    private fun cancelTimeout(context: Context) {
        val intent = Intent(context, UpdatesReceiver::class.java).apply {
            action = Actions.ACTION_ALARM_UPDATE
        }
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(
            getUpdatePendingIntent(context, intent)
        )
    }

    private fun getUpdatePendingIntent(context: Context, intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            ALARM_UPDATE_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val ALARM_UPDATE_ID = 24953
    private const val XIAOMI_DESKCLOCK_PACKAGE = "com.android.deskclock"
}

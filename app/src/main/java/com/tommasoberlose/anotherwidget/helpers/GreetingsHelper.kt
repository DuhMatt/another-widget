package com.tommasoberlose.anotherwidget.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import java.util.*


object GreetingsHelper {
    private const val MORNING_TIME = 36
    private const val MORNING_TIME_END = 37
    private const val EVENING_TIME = 38
    private const val NIGHT_TIME = 39

    fun toggleGreetings(context: Context) {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val now = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.HOUR_OF_DAY, 0)
            }
            if (Preferences.showGreetings) {
                setRepeating(
                    AlarmManager.RTC,
                    now.apply {
                        set(Calendar.HOUR_OF_DAY, 5)
                    }.timeInMillis,
                    1000 * 60 * 60 * 24,
                    PendingIntent.getBroadcast(context,
                        MORNING_TIME,
                        Intent(context, UpdatesReceiver::class.java).apply {
                            action = Actions.ACTION_UPDATE_GREETINGS
                        },
                        PendingIntent.FLAG_IMMUTABLE)
                )

                setRepeating(
                    AlarmManager.RTC,
                    now.apply {
                        set(Calendar.HOUR_OF_DAY, 9)
                    }.timeInMillis,
                    1000 * 60 * 60 * 24,
                    PendingIntent.getBroadcast(context,
                        MORNING_TIME_END,
                        Intent(context, UpdatesReceiver::class.java).apply {
                            action = Actions.ACTION_UPDATE_GREETINGS
                        },
                        PendingIntent.FLAG_IMMUTABLE)
                )

                setRepeating(
                    AlarmManager.RTC,
                    now.apply {
                        set(Calendar.HOUR_OF_DAY, 19)
                    }.timeInMillis,
                    1000 * 60 * 60 * 24,
                    PendingIntent.getBroadcast(context,
                        EVENING_TIME,
                        Intent(context, UpdatesReceiver::class.java).apply {
                            action = Actions.ACTION_UPDATE_GREETINGS
                        },
                        PendingIntent.FLAG_IMMUTABLE)
                )

                setRepeating(
                    AlarmManager.RTC,
                    now.apply {
                        set(Calendar.HOUR_OF_DAY, 22)
                    }.timeInMillis,
                    1000 * 60 * 60 * 24,
                    PendingIntent.getBroadcast(context,
                        NIGHT_TIME,
                        Intent(context, UpdatesReceiver::class.java).apply {
                            action = Actions.ACTION_UPDATE_GREETINGS
                        },
                        PendingIntent.FLAG_IMMUTABLE)
                )
            } else {
                listOf(MORNING_TIME, MORNING_TIME_END, EVENING_TIME, NIGHT_TIME).forEach {
                    cancel(PendingIntent.getBroadcast(context, it, Intent(context,
                        UpdatesReceiver::class.java).apply {
                        action = Actions.ACTION_UPDATE_GREETINGS
                    }, PendingIntent.FLAG_IMMUTABLE))
                }
            }
        }
    }

    fun showGreetings(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour < 9 || hour >= 19
    }

    fun getRandomString(context: Context): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val period = when {
            hour in 5..8 -> 1
            hour in 19..21 -> 2
            hour >= 22 || hour < 5 -> 3
            else -> 0
        }
        if (period == 0) return ""

        val array = when (period) {
            1 -> context.resources.getStringArray(R.array.morning_greetings)
            2 -> context.resources.getStringArray(R.array.evening_greetings)
            else -> context.resources.getStringArray(R.array.night_greetings)
        }
        if (array.isEmpty()) return ""

        val locale = context.resources.configuration.locales[0].toLanguageTag()
        if (Preferences.greetingPeriod != period ||
            Preferences.greetingText.isBlank() ||
            Preferences.greetingLocale != locale
        ) {
            Preferences.greetingPeriod = period
            Preferences.greetingLocale = locale
            Preferences.greetingText = array[Random().nextInt(array.size)]
        }
        return Preferences.greetingText
    }
}

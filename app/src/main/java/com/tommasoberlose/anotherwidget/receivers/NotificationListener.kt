package com.tommasoberlose.anotherwidget.receivers

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.ActiveNotificationsHelper
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.setExactIfAllowed
import java.lang.Exception
import java.util.*


class NotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        MediaPlayerHelper.updatePlayingMediaInfo(this)
        clearStaleNotificationIfNeeded()
        MainWidget.updateWidget(this)
        super.onListenerConnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.notification?.extras?.let { bundle ->
            bundle.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)?.let {
                MediaPlayerHelper.updatePlayingMediaInfo(this)
            } ?: run {
                if (isAcceptedNotification(sbn)) {
                    Preferences.lastNotificationId = sbn.id
                    Preferences.lastNotificationTitle = bundle.getString(Notification.EXTRA_TITLE) ?: ""
                    // Keep a non-zero marker for the existing preference schema. The actual
                    // icon is loaded from the notifying application's ApplicationInfo when
                    // the widget is rendered; Notification.smallIcon is only a status-bar
                    // glyph and is not the application's icon.
                    Preferences.lastNotificationIcon = 1
                    Preferences.lastNotificationPackage = sbn.packageName
                    MainWidget.updateWidget(this)
                    setTimeout(this)
                } else if (isMiHomePlaceholder(sbn) &&
                        Preferences.lastNotificationPackage == sbn.packageName) {
                    // Do not leave an older empty Mi Home shell rendered after it is reposted.
                    ActiveNotificationsHelper.clearLastNotification(this)
                }
            }
        }

        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        MediaPlayerHelper.updatePlayingMediaInfo(this)

        sbn?.let {
            if (sbn.id == Preferences.lastNotificationId && sbn.packageName == Preferences.lastNotificationPackage) {
                ActiveNotificationsHelper.clearLastNotification(this)
            }
        }

        MainWidget.updateWidget(this)
        super.onNotificationRemoved(sbn)
    }

    private fun isAcceptedNotification(sbn: StatusBarNotification?): Boolean {
        if (sbn == null) return false

        val notification = sbn.notification
        val flags = notification.flags
        val isMiHomeForegroundService =
                sbn.packageName == "com.xiaomi.smarthome" &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        notification.channelId == "hide_foreground"
        return notification.extras.containsKey(Notification.EXTRA_TITLE) &&
                flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
                flags and Notification.FLAG_ONGOING_EVENT == 0 &&
                flags and Notification.FLAG_FOREGROUND_SERVICE == 0 &&
                flags and Notification.FLAG_NO_CLEAR == 0 &&
                !isMiHomeForegroundService &&
                !isMiHomePlaceholder(sbn) &&
                ActiveNotificationsHelper.isAppAccepted(sbn.packageName) &&
                !sbn.packageName.contains("com.android.systemui")
    }

    private fun isMiHomePlaceholder(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName != "com.xiaomi.smarthome") return false

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val appLabel = try {
            packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString().trim()
        } catch (ignored: Exception) {
            "米家"
        }

        val hasContent = listOf(
                Notification.EXTRA_TEXT,
                Notification.EXTRA_BIG_TEXT,
                Notification.EXTRA_SUB_TEXT,
                Notification.EXTRA_SUMMARY_TEXT
        ).any { key ->
            extras.getCharSequence(key)?.toString()?.trim()?.isNotEmpty() == true
        } || extras.containsKey(Notification.EXTRA_MESSAGES)

        return title != null && title == appLabel && !hasContent
    }

    private fun clearStaleNotificationIfNeeded() {
        if (Preferences.lastNotificationId == -1 || Preferences.lastNotificationPackage.isBlank()) return

        val activeNotification = try {
            getActiveNotifications().firstOrNull {
                it.id == Preferences.lastNotificationId &&
                        it.packageName == Preferences.lastNotificationPackage
            }
        } catch (ignored: Exception) {
            null
        }

        if (!isAcceptedNotification(activeNotification)) {
            ActiveNotificationsHelper.clearLastNotification(this)
        }
    }

    private fun setTimeout(context: Context) {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val intent = Intent(context, UpdatesReceiver::class.java).apply {
                action = Actions.ACTION_CLEAR_NOTIFICATION
            }
            cancel(PendingIntent.getBroadcast(context, 28943, intent, PendingIntent.FLAG_IMMUTABLE))
            val timeoutPref = Constants.GlanceNotificationTimer.fromInt(Preferences.hideNotificationAfter)
            if (timeoutPref != Constants.GlanceNotificationTimer.WHEN_DISMISSED) {
                setExactIfAllowed(
                    AlarmManager.RTC,
                    Calendar.getInstance().timeInMillis + when (timeoutPref) {
                        Constants.GlanceNotificationTimer.HALF_MINUTE -> 30 * 1000
                        Constants.GlanceNotificationTimer.ONE_MINUTE -> 60 * 1000
                        Constants.GlanceNotificationTimer.FIVE_MINUTES -> 5 * 60 * 1000
                        Constants.GlanceNotificationTimer.TEN_MINUTES -> 10 * 60 * 1000
                        Constants.GlanceNotificationTimer.FIFTEEN_MINUTES -> 15 * 60 * 1000
                        else -> 0
                    },
                    PendingIntent.getBroadcast(
                        context,
                        5,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }
    }
}

package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.helpers.CrashlyticsHelper
import java.lang.Exception

class CrashlyticsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Actions.ACTION_REPORT_CRASH) {
            val exception: Exception = intent.getSerializableExtra(EXCEPTION) as Exception
            CrashlyticsHelper.recordException(context, exception)
            CrashlyticsHelper.sendUnsentReports(context)
        }
    }


    companion object {
        private const val EXCEPTION = "EXCEPTION"

        fun sendCrash(context: Context, exception: Exception) {
            context.sendBroadcast(Intent(context, CrashlyticsReceiver::class.java).apply {
                action = Actions.ACTION_REPORT_CRASH
                putExtra(EXCEPTION, exception)
            })
        }
    }

}

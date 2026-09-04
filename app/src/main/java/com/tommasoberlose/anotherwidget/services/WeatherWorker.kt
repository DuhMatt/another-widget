package com.tommasoberlose.anotherwidget.services

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.chibatching.kotpref.Kotpref
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.network.WeatherNetworkApi
import java.util.concurrent.TimeUnit

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Kotpref.init(applicationContext)
        if (!Preferences.showWeather) return Result.success()

        return try {
            WeatherNetworkApi(applicationContext).updateWeather()
            Result.success()
        } catch (exception: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val IMMEDIATE_WORK = "weather-update-now"
        private const val PERIODIC_WORK = "weather-update-periodic"

        private fun constraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueue(context: Context, replace: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<WeatherWorker>()
                .setConstraints(constraints())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK,
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun schedule(context: Context) {
            Kotpref.init(context)
            if (!Preferences.showWeather) {
                cancel(context)
                return
            }

            val intervalMinutes = when (Preferences.weatherRefreshPeriod) {
                0 -> 30L
                1 -> 60L
                2 -> 180L
                3 -> 360L
                4 -> 720L
                5 -> 1440L
                else -> 60L
            }
            val request = PeriodicWorkRequestBuilder<WeatherWorker>(intervalMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK)
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        }
    }
}

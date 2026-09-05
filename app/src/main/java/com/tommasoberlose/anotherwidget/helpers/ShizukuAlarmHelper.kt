package com.tommasoberlose.anotherwidget.helpers

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.ServiceConnection
import android.os.IBinder
import com.tommasoberlose.anotherwidget.BuildConfig
import com.tommasoberlose.anotherwidget.shizuku.IPrivilegedAlarmService
import com.tommasoberlose.anotherwidget.shizuku.PrivilegedAlarmService
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import rikka.shizuku.Shizuku
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Coordinates Shizuku permission, UserService binding, and cached alarm reads. */
object ShizukuAlarmHelper {
    private const val REQUEST_CODE = 1401
    private const val SERVICE_VERSION = 1
    private const val SERVICE_TAG = "another-widget-alarm-alert-v1"

    private val lock = Any()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    @Volatile
    private var service: IPrivilegedAlarmService? = null
    @Volatile
    private var cachedAlarmTime: Long? = null
    @Volatile
    private var hasCachedResult = false
    @Volatile
    private var refreshInFlight = false
    @Volatile
    private var applicationContext: Context? = null
    private var serviceConnection: ServiceConnection? = null

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
            applicationContext?.let { context ->
                ensureBound(context)
                MainWidget.updateWidget(context)
            }
        }
    }

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (_: Throwable) {
            // Shizuku is optional. The normal AlarmManager path remains available.
        }
    }

    fun requestPermission(activity: Activity) {
        try {
            if (!Shizuku.pingBinder()) return
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ensureBound(activity.applicationContext)
                return
            }
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Throwable) {
            // Sui/Shizuku may be stopped or unavailable; the caller will use fallback logic.
        }
    }

    fun isUsable(context: Context): Boolean {
        return try {
            applicationContext = context.applicationContext
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Returns the last privileged result without blocking the UI. The first
     * call starts the UserService; the result is pushed into the widget when
     * the asynchronous read completes.
     */
    fun getNextAlarmAlertTime(context: Context): Long? {
        if (!isUsable(context)) {
            clearState()
            return null
        }

        ensureBound(context.applicationContext)
        synchronized(lock) {
            if (!hasCachedResult) return null
            return cachedAlarmTime
        }
    }

    fun invalidate(context: Context) {
        if (!isUsable(context)) {
            clearState()
            return
        }
        synchronized(lock) {
            hasCachedResult = false
            cachedAlarmTime = null
        }
        ensureBound(context.applicationContext)
    }

    private fun ensureBound(context: Context) {
        applicationContext = context.applicationContext
        if (!isUsableWithoutRecursion()) return

        synchronized(lock) {
            if (service != null || serviceConnection != null) {
                requestRefreshLocked(context.applicationContext)
                return
            }

            val args = Shizuku.UserServiceArgs(
                ComponentName(context, PrivilegedAlarmService::class.java)
            )
                .daemon(false)
                .processNameSuffix("alarm")
                .debuggable(BuildConfig.DEBUG)
                .version(SERVICE_VERSION)
                .tag(SERVICE_TAG)

            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    synchronized(lock) {
                        service = binder?.let { IPrivilegedAlarmService.Stub.asInterface(it) }
                        serviceConnection = this
                        requestRefreshLocked(applicationContext ?: context.applicationContext)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    synchronized(lock) {
                        service = null
                        serviceConnection = null
                        refreshInFlight = false
                    }
                }
            }

            serviceConnection = connection
            try {
                Shizuku.bindUserService(args, connection)
            } catch (_: Throwable) {
                serviceConnection = null
                service = null
            }
        }
    }

    private fun requestRefreshLocked(context: Context) {
        if (service == null || refreshInFlight) return
        refreshInFlight = true
        val remoteService = service ?: run {
            refreshInFlight = false
            return
        }

        executor.execute {
            val result = try {
                remoteService.getNextAlarmAlertTime().takeIf { it > 0L }
            } catch (_: Throwable) {
                null
            }

            synchronized(lock) {
                cachedAlarmTime = result
                hasCachedResult = true
                refreshInFlight = false
            }
            MainWidget.updateWidget(context)
        }
    }

    private fun isUsableWithoutRecursion(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    private fun clearState() {
        synchronized(lock) {
            service = null
            serviceConnection = null
            cachedAlarmTime = null
            hasCachedResult = false
            refreshInFlight = false
        }
    }
}

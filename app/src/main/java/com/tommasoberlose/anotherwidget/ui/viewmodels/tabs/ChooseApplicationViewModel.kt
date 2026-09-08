package com.tommasoberlose.anotherwidget.ui.viewmodels.tabs

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChooseApplicationViewModel(application: Application) : AndroidViewModel(application) {

    val pm: PackageManager by lazy { application.packageManager }
    val appList: MutableLiveData<List<ResolveInfo>> = MutableLiveData()
    val searchInput: MutableLiveData<String> = MutableLiveData("")
    val showSystemApps: MutableLiveData<Boolean> = MutableLiveData(false)

    private var allAppList: List<ResolveInfo> = emptyList()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            // Query system launchers explicitly as Android versions and OEM package
            // managers may omit them from the default query result.
            val app = (application.packageManager.queryIntentActivities(mainIntent, 0) +
                application.packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_SYSTEM_ONLY))
                .distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name}" }
            allAppList = app.sortedWith(Comparator { app1: ResolveInfo, app2: ResolveInfo ->
                app1.loadLabel(pm).toString().compareTo(app2.loadLabel(pm).toString())
            })
            withContext(Dispatchers.Main) {
                publishVisibleApps()
            }
        }
    }

    fun setShowSystemApps(show: Boolean) {
        showSystemApps.value = show
        publishVisibleApps()
    }

    private fun publishVisibleApps() {
        val visibleApps = if (showSystemApps.value == true) {
            allAppList
        } else {
            allAppList.filterNot(::isSystemApp)
        }
        appList.value = visibleApps
    }

    private fun isSystemApp(app: ResolveInfo): Boolean {
        val flags = app.activityInfo.applicationInfo.flags
        return flags and (android.content.pm.ApplicationInfo.FLAG_SYSTEM or
            android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }
}

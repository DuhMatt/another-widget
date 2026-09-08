package com.tommasoberlose.anotherwidget.ui.activities

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.ActivityMainBinding
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.ui.activities.tabs.WeatherProviderActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import com.tommasoberlose.anotherwidget.utils.toPixel

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var mAppWidgetId: Int = -1
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private val mainNavController: NavController? by lazy {
        Navigation.findNavController(
            this,
            R.id.content_fragment
        )
    }
    private val settingsNavController: NavController? by lazy {
        Navigation.findNavController(
            this,
            R.id.settings_fragment
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        binding = ActivityMainBinding.inflate(layoutInflater)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // The wallpaper preview requires a transparent window, so draw both system
        // bar backgrounds in the content layer for an exact color match on Android 15+.
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = !isDarkTheme()
            isAppearanceLightNavigationBars = !isDarkTheme()
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            binding.statusBarScrim.layoutParams = binding.statusBarScrim.layoutParams.apply {
                height = systemBarInsets.top
            }
            binding.navigationBarScrim.layoutParams = binding.navigationBarScrim.layoutParams.apply {
                height = maxOf(systemBarInsets.bottom, gestureInsets.bottom, 24.toPixel(this@MainActivity))
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
        controlExtras(intent)
    }

    private fun handleBackNavigation() {
        if (mainNavController?.currentDestination?.id == R.id.appMainFragment) {
            if (settingsNavController?.navigateUp() == true) {
                viewModel.fragmentScrollY.value = 0
                return
            }
        } else if (mainNavController?.navigateUp() == true) {
            return
        }

        if (mAppWidgetId > 0) {
            addNewWidget()
        } else {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) {
            controlExtras(intent)
        }
    }

    private fun controlExtras(intent: Intent) {
        val extras = intent.extras
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)

            if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                binding.actionAddWidget.visibility = View.VISIBLE
                binding.actionAddWidget.setOnClickListener {
                    addNewWidget()
                }
            }


            if (extras.containsKey(Actions.ACTION_EXTRA_OPEN_WEATHER_PROVIDER)) {
                startActivityForResult(Intent(this, WeatherProviderActivity::class.java), RequestCode.WEATHER_PROVIDER_REQUEST_CODE.code)
            }
        }
    }

    private fun addNewWidget() {
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    override fun onResume() {
        super.onResume()

        if (Preferences.showEvents && !checkGrantedPermission(Manifest.permission.READ_CALENDAR)) {
            Preferences.showEvents = false
        }
    }

    override fun onStart() {
        Preferences.preferences.registerOnSharedPreferenceChangeListener(this)
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        Preferences.preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        MainWidget.updateWidget(this)
    }
}

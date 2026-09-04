package com.tommasoberlose.anotherwidget.helpers

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.tommasoberlose.anotherwidget.R

object LanguageHelper {
    const val ENGLISH = "en"
    const val SIMPLIFIED_CHINESE = "zh-CN"

    private const val PREFERENCES_NAME = "app_language_preferences"
    private const val LANGUAGE_KEY = "language_tag"

    /** Applies the selected locale on pre-Android 13 devices before resources are created. */
    fun wrap(context: Context): Context {
        val languageTag = getStoredLanguageTag(context) ?: return context
        val locale = LocaleList.forLanguageTags(languageTag).get(0) ?: return context
        val configuration = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        return context.createConfigurationContext(configuration)
    }

    fun getLanguageTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val applicationLocales = context
                .getSystemService(LocaleManager::class.java)
                .applicationLocales
            if (!applicationLocales.isEmpty) {
                return normalizeLanguageTag(applicationLocales.toLanguageTags())
            }
        }

        getStoredLanguageTag(context)?.let { return it }

        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return if (locale?.language.equals("zh", ignoreCase = true)) {
            SIMPLIFIED_CHINESE
        } else {
            ENGLISH
        }
    }

    fun getDisplayName(context: Context, languageTag: String): String {
        return context.getString(
            if (normalizeLanguageTag(languageTag) == SIMPLIFIED_CHINESE) {
                R.string.settings_language_simplified_chinese
            } else {
                R.string.settings_language_english
            }
        )
    }

    fun setLanguage(context: Context, languageTag: String) {
        val normalizedLanguageTag = normalizeLanguageTag(languageTag)
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, normalizedLanguageTag)
            .apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(normalizedLanguageTag)
        } else {
            (context as? Activity)?.recreate()
        }
    }

    private fun getStoredLanguageTag(context: Context): String? {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(LANGUAGE_KEY, null)
            ?.let(::normalizeLanguageTag)
    }

    private fun normalizeLanguageTag(languageTag: String): String {
        return if (languageTag.startsWith("zh", ignoreCase = true)) {
            SIMPLIFIED_CHINESE
        } else {
            ENGLISH
        }
    }
}

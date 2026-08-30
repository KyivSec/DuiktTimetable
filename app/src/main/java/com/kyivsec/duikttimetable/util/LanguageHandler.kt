package com.kyivsec.duikttimetable.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    UKRAINIAN("uk"),
    ENGLISH("en"),
}

object LanguageHandler {
    val availableLanguages: List<AppLanguage> = AppLanguage.entries

    fun selectedLanguage(context: Context): AppLanguage {
        val stored = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(LANGUAGE_KEY, AppLanguage.SYSTEM.name)
        return AppLanguage.entries.firstOrNull { it.name == stored } ?: AppLanguage.SYSTEM
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit().putString(LANGUAGE_KEY, language.name).apply()
    }

    fun setLanguageAndRecreate(context: Context, language: AppLanguage) {
        setLanguage(context, language)
        generateSequence(context as Context?) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()
            ?.recreate()
    }

    fun wrapContext(context: Context): Context {
        val language = selectedLanguage(context)
        val locale = language.tag?.let(Locale::forLanguageTag) ?: supportedSystemLocale(context)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    private fun supportedSystemLocale(context: Context): Locale {
        val systemLocale = context.resources.configuration.locales[0]
        return when (systemLocale.language) {
            "en" -> Locale.forLanguageTag("en")
            "uk" -> Locale.forLanguageTag("uk")
            else -> Locale.forLanguageTag("uk")
        }
    }

    private const val PREFERENCES_NAME = "language_preferences"
    private const val LANGUAGE_KEY = "language"
}

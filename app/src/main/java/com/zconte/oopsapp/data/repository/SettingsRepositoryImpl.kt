package com.zconte.oopsapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zconte.oopsapp.domain.model.ThemeMode
import com.zconte.oopsapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val stored = prefs[THEME_MODE_KEY]
        stored?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode.name }
    }
}
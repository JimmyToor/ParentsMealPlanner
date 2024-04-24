package com.jimmy.parentsmealplanner.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.jimmy.parentsmealplanner.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {
    private companion object {
        val APP_THEME = intPreferencesKey("is_dark_theme")
        const val TAG = "UserPreferencesRepo"
    }

    override val appTheme: Flow<AppTheme> = dataStore.data
        .catch { exception ->
            Log.e(TAG, "Error reading preferences.", exception)
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[APP_THEME]?.let { AppTheme.fromOrdinal(it) } ?: AppTheme.MODE_AUTO
        }

    override suspend fun saveDarkThemePreference(appTheme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[APP_THEME] = appTheme.ordinal
        }
    }
}

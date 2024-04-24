package com.jimmy.parentsmealplanner.data

import com.jimmy.parentsmealplanner.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val appTheme: Flow<AppTheme>
    suspend fun saveDarkThemePreference(appTheme: AppTheme)
}
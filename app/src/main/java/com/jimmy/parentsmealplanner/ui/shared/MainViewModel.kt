package com.jimmy.parentsmealplanner.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jimmy.parentsmealplanner.data.UserPreferencesRepository
import com.jimmy.parentsmealplanner.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This class represents the main ViewModel for the application, which is responsible for managing
 * user preferences and any app-wide state.
 *
 * @property userPreferencesRepository The repository used to get and save user preferences.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel()
{
    val uiState: StateFlow<PlanningAppUiState> =
        userPreferencesRepository.appTheme.map { appTheme ->
            PlanningAppUiState(appTheme = appTheme)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlanningAppUiState()
        )

    fun changeTheme(appTheme: AppTheme) {
        viewModelScope.launch {
            userPreferencesRepository.saveDarkThemePreference(appTheme)
        }
    }
}

data class PlanningAppUiState(
    val appTheme: AppTheme = AppTheme.MODE_AUTO,
)
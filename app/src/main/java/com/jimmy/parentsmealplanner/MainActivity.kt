package com.jimmy.parentsmealplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.jimmy.parentsmealplanner.ui.nav.MealPlanningNavHost
import com.jimmy.parentsmealplanner.ui.shared.MainViewModel
import com.jimmy.parentsmealplanner.ui.theme.AppTheme
import com.jimmy.parentsmealplanner.ui.theme.LocalDarkTheme
import com.jimmy.parentsmealplanner.ui.theme.ParentsMealPlannerTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val activityViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState = activityViewModel.uiState.collectAsStateWithLifecycle().value
            val darkTheme = when(uiState.appTheme) {
                AppTheme.MODE_LIGHT -> false
                AppTheme.MODE_DARK -> true
                AppTheme.MODE_AUTO -> isSystemInDarkTheme()
            }

            CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
                ParentsMealPlannerTheme(
                    darkTheme = darkTheme,
                    dynamicColor = false,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                        tonalElevation = 5.dp,
                    ) {
                        MealPlanningApp()
                    }
                }
            }
        }
    }
}

@Composable
fun MealPlanningApp(navController: NavHostController = rememberNavController()) {
    MealPlanningNavHost(navController = navController)
}
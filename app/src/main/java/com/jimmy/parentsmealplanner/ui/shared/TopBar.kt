package com.jimmy.parentsmealplanner.ui.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.jimmy.parentsmealplanner.R
import com.jimmy.parentsmealplanner.ui.theme.AppTheme
import com.jimmy.parentsmealplanner.ui.theme.LocalDarkTheme

/**
 * App bar to display title and conditionally display the back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview()
fun TopBar(
    modifier: Modifier = Modifier,
    title: String = "Parents Meal Planner",
    canNavigateBack: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigateUp: () -> Unit = {},
    onThemeToggle: (AppTheme) -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back_button),
                    )
                }
            }
        },
        actions = {
            ThemeSwitch(onThemeToggle = onThemeToggle)
        }
    )
}

/**
 * Switch composable to toggle between dark and light theme
 */
@Composable
@Preview()
fun ThemeSwitch(
    modifier: Modifier = Modifier,
    onThemeToggle: (AppTheme) -> Unit = {},
) {
    Switch(
        checked = LocalDarkTheme.current,
        onCheckedChange = {
            if (it) {
                onThemeToggle(AppTheme.MODE_DARK)
            } else {
                onThemeToggle(AppTheme.MODE_LIGHT)
            }
        },
        thumbContent = {
            if (LocalDarkTheme.current) {
                Icon(
                    imageVector =
                        ImageVector.vectorResource(id = R.drawable.baseline_dark_mode_24),
                    contentDescription = null,
                )
            } else {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.baseline_light_mode_24),
                    contentDescription = null,
                )
            }
        },
    )
}
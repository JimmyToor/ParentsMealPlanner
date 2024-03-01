package com.jimmy.parentsmealplanner.ui.meal

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jimmy.parentsmealplanner.R
import com.jimmy.parentsmealplanner.ui.nav.NavigationDestination

object MealDetailDest : NavigationDestination {
    override val route = "meal_detail"
    override val titleRes = R.string.meal_detail
}

@Composable
@Preview
fun MealDetail() {

}

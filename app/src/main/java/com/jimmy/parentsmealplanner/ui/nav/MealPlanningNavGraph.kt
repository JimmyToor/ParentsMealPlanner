package com.jimmy.parentsmealplanner.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jimmy.parentsmealplanner.ui.meal.MealDetail
import com.jimmy.parentsmealplanner.ui.meal.MealDetailDest
import com.jimmy.parentsmealplanner.ui.meal.MealPlanner
import com.jimmy.parentsmealplanner.ui.meal.MealPlanningDest
import com.jimmy.parentsmealplanner.ui.shared.Occasion

/**
 * Provides Navigation graph for the application.
 */
@Composable
fun MealPlanningNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = MealPlanningDest.route,
        modifier = modifier,
    ) {
        composable(MealPlanningDest.route) {
            MealPlanner(
                navigateToMealDetail = {
                    mealId, date, occasion -> navController.navigate(
                    "${MealDetailDest.route}/${date.toEpochDays()}/${occasion}/$mealId"
                    )
                }
            )
        }
        composable(
            route = MealDetailDest.routeWithArgs,
            arguments = listOf(
                navArgument(MealDetailDest.MEAL_ID_ARG) { type = NavType.IntType },
                navArgument(MealDetailDest.DATE_ARG) { type = NavType.IntType },
                navArgument(MealDetailDest.OCCASION_ARG) {
                    type = NavType.EnumType(Occasion::class.java)
                },
            )
        ) {
            MealDetail(
                navigateBack = { navController.popBackStack() },
                onNavigateUp = { navController.navigateUp() },
            )
        }
    }
}
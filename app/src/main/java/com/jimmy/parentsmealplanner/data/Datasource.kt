package com.jimmy.parentsmealplanner.data

import com.jimmy.parentsmealplanner.model.Dish
import com.jimmy.parentsmealplanner.model.DishInMeal
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealWithDishes
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.Rating
import kotlinx.datetime.LocalDate

/**
 * [Datasource] provides a pre-made list of Meals, Dishes, and DishInMeals.
 */
class Datasource {
    companion object {
        private val meals: List<Meal> =
            listOf(
                Meal(1, Occasion.BREAKFAST, Rating.LEARNING, "meal1", LocalDate(2021, 1, 1)),
                Meal(2, Occasion.LUNCH, Rating.LIKEIT, "meal2", LocalDate(2021, 1, 2)),
                Meal(3, Occasion.DINNER, Rating.LOVEIT, "meal3", LocalDate(2021, 1, 3)),
                Meal(4, Occasion.SNACK, Rating.LOVEIT, "meal4", LocalDate(2021, 1, 4)),
                Meal(5, Occasion.BREAKFAST, Rating.LOVEIT, "meal5", LocalDate(2021, 1, 5)),
                Meal(6, Occasion.LUNCH, Rating.LEARNING, "meal6", LocalDate(2021, 1, 6)),
                Meal(7, Occasion.DINNER, Rating.LIKEIT, "meal7", LocalDate(2021, 1, 7)),
                Meal(8, Occasion.SNACK, Rating.LOVEIT, "meal8", LocalDate(2021, 1, 8)),
                Meal(9, Occasion.BREAKFAST, Rating.LOVEIT, "meal9", LocalDate(2021, 1, 9)),
                Meal(10, Occasion.LUNCH, Rating.LOVEIT, "meal10", LocalDate(2021, 1, 10)),
            )

        private val dishes: List<Dish> =
            listOf(
                Dish(1, "dish1", Rating.LEARNING),
                Dish(2, "dish2", Rating.LIKEIT),
                Dish(3, "dish3", Rating.LOVEIT),
                Dish(4, "dish4", Rating.LOVEIT),
                Dish(5, "dish5", Rating.LOVEIT),
                Dish(6, "dish6", Rating.LEARNING),
                Dish(7, "dish7", Rating.LIKEIT),
                Dish(8, "dish8", Rating.LOVEIT),
                Dish(9, "dish9", Rating.LOVEIT),
                Dish(10, "dish10", Rating.LOVEIT),
            )

        private val dishesInMeals: List<DishInMeal> =
            listOf(
                DishInMeal(1, 1),
                DishInMeal(1, 2),
                DishInMeal(1, 3),
                DishInMeal(2, 2),
                DishInMeal(2, 3),
                DishInMeal(3, 3),
                DishInMeal(3, 4),
                DishInMeal(4, 4),
                DishInMeal(4, 5),
                DishInMeal(5, 5),
                DishInMeal(5, 6),
                DishInMeal(6, 6),
                DishInMeal(6, 7),
                DishInMeal(7, 7),
                DishInMeal(7, 8),
                DishInMeal(8, 8),
                DishInMeal(8, 9),
                DishInMeal(9, 9),
                DishInMeal(9, 10),
                DishInMeal(10, 10),
                DishInMeal(10, 1),
            )

        private val mealsWithDishes: List<MealWithDishes> =
            meals.map { meal ->
                val dishesForMeal =
                    dishesInMeals
                        .filter { it.mealId == meal.id }
                        .mapNotNull { dishInMeal -> dishes.find { it.id == dishInMeal.dishId } }
                MealWithDishes(meal, dishesForMeal)
            }

        fun loadMeals(): List<Meal> {
            return meals
        }

        fun loadDishes(): List<Dish> {
            return dishes
        }

        fun loadDishInMeals(): List<DishInMeal> {
            return dishesInMeals
        }

        fun loadMealsWithDishes(): List<MealWithDishes> {
            return mealsWithDishes
        }
    }
}
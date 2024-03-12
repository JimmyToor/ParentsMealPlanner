package com.jimmy.parentsmealplanner.ui.shared

import com.jimmy.parentsmealplanner.model.Dish
import com.jimmy.parentsmealplanner.model.DishInMeal
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealWithDishes
import kotlinx.datetime.LocalDate

data class MealDetails(
    val id: Int = 0,
    val occasion: Occasion = Occasion.BREAKFAST,
    val rating: Rating = Rating.LIKEIT,
    val name: String = "",
    val date: LocalDate = LocalDate(1, 1, 1),
    val dishes: List<DishDetails>? = listOf(),
)

data class DishDetails(
    val id: Int = 0,
    val name: String = "",
    val rating: Rating = Rating.LIKEIT,
)

/**
 * Converts a [MealDetails] to a [Meal].
 */
fun MealDetails.toMeal(): Meal =
    Meal(
        id = id,
        occasion = occasion,
        name = name,
        rating = rating,
        date = date,
    )

/**
 * Converts a [Meal] to a [MealDetails].
 */
fun Meal.toMealDetails(): MealDetails =
    MealDetails(
        id = id,
        occasion = occasion,
        name = name,
        rating = rating,
        date = date,
    )

/**
 * Converts a [MealWithDishes] to a [MealDetails].
 */
fun MealDetails.toMealWithDishes(): MealWithDishes =
    MealWithDishes(
        meal = this.toMeal(),
        dishes = dishes?.map { it.toDish() } ?: listOf(),
    )

/**
 * Converts a [MealDetails] to a [MealWithDishes].
 */
fun MealWithDishes.toMealDetails(): MealDetails =
    MealDetails(
        id = meal.id,
        occasion = meal.occasion,
        name = meal.name,
        rating = meal.rating,
        date = meal.date,
        dishes = dishes.map { it.toDishDetails() },
    )

/**
 * Converts a [DishDetails] to a [Dish].
 */
fun DishDetails.toDish(): Dish =
    Dish(
        id = id,
        name = name,
        rating = rating,
    )

/**
 * Converts a [Dish] to a [DishDetails].
 */
fun Dish.toDishDetails(): DishDetails =
    DishDetails(
        id = id,
        name = name,
        rating = rating,
    )

/**
 * Converts a [Dish] to a [DishInMeal].
 */
fun Dish.toDishInMeal(mealId: Int): DishInMeal =
    DishInMeal(mealId = mealId, dishId = id)

/**
 * Converts a [MealWithDishes] to a [Dish].
 */
fun MealWithDishes.toDishesInMeal(): List<DishInMeal> =
    dishes.map {
            dish -> DishInMeal(mealId = meal.id, dishId = dish.id)
    }

enum class Occasion {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
}

enum class Rating {
    LEARNING,
    LIKEIT,
    LOVEIT,
}
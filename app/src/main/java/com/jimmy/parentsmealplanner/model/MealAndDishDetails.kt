package com.jimmy.parentsmealplanner.model

import kotlinx.datetime.LocalDate

data class MealDetails(
    val id: Int = 0,
    val occasion: Occasion = Occasion.BREAKFAST,
    val rating: Rating = Rating.ONE,
    val name: String = "",
    val date: LocalDate = LocalDate(1, 1, 1),
    val dishes: List<DishDetails>? = listOf(DishDetails()),
)

data class DishDetails(
    val id: Int = 0,
    val name: String = "",
    val rating: Rating = Rating.ONE,
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

enum class Occasion {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
}

enum class Rating {
    ONE,
    TWO,
    THREE,
    FOUR,
    FIVE,
}
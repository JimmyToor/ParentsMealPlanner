package com.jimmy.parentsmealplanner.ui.shared

import com.jimmy.parentsmealplanner.model.Dish
import com.jimmy.parentsmealplanner.model.DishInMeal
import com.jimmy.parentsmealplanner.model.InstanceDetails
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealWithDishes
import com.jimmy.parentsmealplanner.model.MealWithDishesAndAllInstances
import com.jimmy.parentsmealplanner.model.MealWithDishesInstance
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class MealDetails(
    val mealId: Long = 0,
    val rating: Rating = Rating.LIKEIT,
    val name: String = "",
    val dishes: List<DishDetails> = listOf(),
    val imgSrc: String? = "",
)

data class MealInstanceDetails(
    val mealInstanceId: Long = 0,
    val mealDetails: MealDetails = MealDetails(),
    val occasion: Occasion = Occasion.BREAKFAST,
    val date: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val userId: Long = 0,
)

data class DishDetails(
    val dishId: Long = 0,
    val name: String = "",
    val rating: Rating = Rating.LIKEIT,
)

enum class Occasion {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    ;

    companion object {
        fun fromInt(value: Int) = entries.find { it.ordinal == value }
    }
}

enum class Rating {
    LEARNING,
    LIKEIT,
    LOVEIT,
}

/**
 * Converts a [MealInstanceDetails] to an [InstanceDetails].
 */
fun MealInstanceDetails.toInstanceDetails(): InstanceDetails =
    InstanceDetails(
        mealInstanceId = mealInstanceId,
        date = date,
        occasion = occasion,
        userId = userId,
    )

/**
 * Converts a [MealDetails] to a [Meal].
 */
fun MealDetails.toMeal(): Meal =
    Meal(
        mealId = mealId,
        name = name,
        rating = rating,
        imgSrc = imgSrc,
    )

/**
 * Converts a [Meal] to a [MealDetails].
 */
fun Meal.toMealDetails(): MealDetails =
    MealDetails(
        mealId = mealId,
        name = name,
        rating = rating,
        imgSrc = imgSrc,
    )

/**
 * Converts a [MealWithDishes] to a [MealDetails].
 */
fun MealDetails.toMealWithDishes(removeEmptyDishes: Boolean = true): MealWithDishes =
    MealWithDishes(
        meal = this.toMeal(),
        dishes = when (removeEmptyDishes) {
            true -> dishes.filterNot { it.name.isBlank() }
            false -> dishes
        }.map { it.toDish()},
    )

/**
 * Converts a [MealWithDishesAndAllInstances] to a list of [MealDetails] for each instance.
 */
fun MealWithDishesAndAllInstances.toMealInstanceDetails(): List<MealInstanceDetails> =
    mealInstanceDetails.map { mealInstanceDetails ->
        MealInstanceDetails(
            mealInstanceId = mealInstanceDetails.mealInstanceId,
            mealDetails = MealDetails(
                mealId = meal.mealId,
                name = meal.name,
                rating = meal.rating,
                dishes = dishes.map { it.toDishDetails() },
                imgSrc = meal.imgSrc,
            ),
            occasion = mealInstanceDetails.occasion,
            date = mealInstanceDetails.date,
            userId = mealInstanceDetails.userId,
        )
    }

/**
 * Converts a [MealWithDishesAndInstance] to a [MealInstanceDetails]
 */
fun MealWithDishesInstance.toMealInstanceDetails(): MealInstanceDetails =
    MealInstanceDetails(
        mealInstanceId = instanceDetails.mealInstanceId,
        mealDetails = mealWithDishes.toMealDetails(),
        occasion = instanceDetails.occasion,
        date = instanceDetails.date,
        userId = instanceDetails.userId,
    )

/**
 * Converts a [MealWithDishes] to [MealDetails].
 */
fun MealWithDishes.toMealDetails(): MealDetails =
    MealDetails(
        mealId = meal.mealId,
        name = meal.name,
        rating = meal.rating,
        dishes = dishes.map { it.toDishDetails() },
        imgSrc = meal.imgSrc,
    )

/**
 * Converts a [MealWithDishes] to a [MealInstanceDetails]
 */
fun MealWithDishes.toMealInstanceDetails(
    date: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    occasion: Occasion = Occasion.BREAKFAST,
    userId: Long = 0,
    mealId: Long = 0,
): MealInstanceDetails =
    MealInstanceDetails(
        mealInstanceId = mealId,
        mealDetails = this.toMealDetails(),
        occasion = occasion,
        date = date,
        userId = userId,
    )

/**
 * Converts a [DishDetails] to a [Dish].
 */
fun DishDetails.toDish(): Dish =
    Dish(
        dishId = dishId,
        name = name,
        rating = rating,
    )

/**
 * Converts a [DishDetails] to a [DishInMeal].
 */
fun DishDetails.toDishInMeal(mealId: Long): DishInMeal =
    DishInMeal(
        mealId = mealId,
        dishId = dishId,
    )

/**
 * Converts a [Dish] to a [DishDetails].
 */
fun Dish.toDishDetails(): DishDetails =
    DishDetails(
        dishId = dishId,
        name = name,
        rating = rating,
    )
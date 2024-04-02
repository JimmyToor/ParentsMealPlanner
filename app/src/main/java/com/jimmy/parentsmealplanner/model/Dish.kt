package com.jimmy.parentsmealplanner.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.jimmy.parentsmealplanner.ui.shared.Rating

@Entity(tableName = "dishes")
data class Dish(
    @PrimaryKey(autoGenerate = true)
    val dishId: Long = 0,
    val rating: Rating = Rating.LIKEIT,
    val name: String = "New Dish",
)

data class DishWithMeals(
    @Embedded
    val dish: Dish,
    @Relation(
        parentColumn = "dishId",
        entity = Meal::class,
        entityColumn = "mealId",
        associateBy = Junction(
            DishInMeal::class,
            parentColumn = "dishId",
            entityColumn = "mealId"
        )
    )
    val meals: List<Meal>,
)


/**
 * Converts a [Dish] to a [DishInMeal].
 */
fun Dish.toDishInMeal(mealId: Long): DishInMeal =
    DishInMeal(mealId = mealId, dishId = dishId)

/**
 * Converts a list of [Dish]s to a list of [DishInMeal].
 */
fun List<Dish>.toDishesInMeal(mealId: Long): List<DishInMeal> =
    map { it.toDishInMeal(mealId) }


/**
 * Converts a [DishInMeal] to a [Dish].
 */
fun DishInMeal.toDish(): Dish =
    Dish(dishId = dishId, name = "", rating = Rating.LIKEIT)

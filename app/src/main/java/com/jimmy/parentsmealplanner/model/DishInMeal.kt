package com.jimmy.parentsmealplanner.model

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "dishes_in_meals",
    primaryKeys = ["mealId", "dishId"],
    indices = [
        androidx.room.Index("mealId"),
        androidx.room.Index("dishId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["mealId"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Dish::class,
            parentColumns = ["dishId"],
            childColumns = ["dishId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DishInMeal(
    val mealId: Long,
    val dishId: Long,
)
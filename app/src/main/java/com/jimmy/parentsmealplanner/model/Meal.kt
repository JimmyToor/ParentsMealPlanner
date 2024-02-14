package com.jimmy.parentsmealplanner.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    // occasion enum
    val occasion: Int,
    // rating enum
    val rating: Int,
    val name: String? = null,
    val date: Int,
)

data class MealWithDishes(
    @Embedded
    val meal: Meal,
    @Relation(
        parentColumn = "id",
        entity = Dish::class,
        entityColumn = "id",
        associateBy = Junction(
            DishesInMeal::class,
            parentColumn = "mealId",
            entityColumn = "dishId"
        )
    )
    val dishes: List<Dish>
)
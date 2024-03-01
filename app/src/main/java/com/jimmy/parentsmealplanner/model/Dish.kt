package com.jimmy.parentsmealplanner.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "dishes")
data class Dish(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val rating: Rating = Rating.THREE,
)

data class DishWithMeals(
    @Embedded
    val dish: Dish,
    @Relation(
        parentColumn = "id",
        entity = Meal::class,
        entityColumn = "id",
        associateBy = Junction(
            DishInMeal::class,
            parentColumn = "dishId",
            entityColumn = "mealId"
        )
    )
    val meals: List<Meal>
)
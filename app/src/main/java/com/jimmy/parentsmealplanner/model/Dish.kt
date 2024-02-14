package com.jimmy.parentsmealplanner.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.SerialName

@Entity(tableName = "dishes")
data class Dish(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    @SerialName(value = "img_src")
    val imgSrc: String,
    // rating enum
    val rating: Int,
)

data class DishWithMeals(
    @Embedded
    val dish: Dish,
    @Relation(
        parentColumn = "id",
        entity = Meal::class,
        entityColumn = "id",
        associateBy = Junction(
            DishesInMeal::class,
            parentColumn = "dishId",
            entityColumn = "mealId"
        )
    )
    val meals: List<Meal>
)
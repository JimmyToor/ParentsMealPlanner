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
    val id: Int = 0,
    val name: String = "New Dish",
    val rating: Rating = Rating.LIKEIT,
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
package com.jimmy.parentsmealplanner.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.datetime.LocalDate

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val occasion: Occasion = Occasion.BREAKFAST,
    val rating: Rating = Rating.THREE,
    val name: String = "",
    val date: LocalDate = LocalDate(0, 0, 0),
)

data class MealWithDishes(
    @Embedded
    val meal: Meal,
    @Relation(
        parentColumn = "id",
        entity = Dish::class,
        entityColumn = "id",
        associateBy =
            Junction(
                DishInMeal::class,
                parentColumn = "mealId",
                entityColumn = "dishId",
            ),
    )
    val dishes: List<Dish>,
)
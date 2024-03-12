package com.jimmy.parentsmealplanner.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.Rating
import kotlinx.datetime.LocalDate

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val occasion: Occasion = Occasion.BREAKFAST,
    val rating: Rating = Rating.LIKEIT,
    val name: String = "New Meal",
    val date: LocalDate = LocalDate(1, 1, 1),
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
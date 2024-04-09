package com.jimmy.parentsmealplanner.model

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.Rating
import kotlinx.datetime.LocalDate

/**
 * Meal entity and relevant POJOs for grouping related data together
 */
@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val mealId: Long = 0,
    val rating: Rating = Rating.LIKEIT,
    val name: String = "New Meal",
    val imgSrc: String? = null,
)

data class MealWithDishesInstance(
    @Embedded
    var instanceDetails: InstanceDetails,
    @Embedded
    var mealWithDishes: MealWithDishes,
)

data class MealWithDishes(
    @Embedded
    var meal: Meal,
    @Relation(
        parentColumn = "mealId",
        entity = Dish::class,
        entityColumn = "dishId",
        associateBy = Junction(
                DishInMeal::class,
                parentColumn = "mealId",
                entityColumn = "dishId",
            ),
    )
    var dishes: List<Dish>
)

data class MealWithDishesAndAllInstances(
    @Embedded
    var meal: Meal,
    @Relation(
        parentColumn = "mealId",
        entity = Dish::class,
        entityColumn = "dishId",
        associateBy =
            Junction(
                DishInMeal::class,
                parentColumn = "mealId",
                entityColumn = "dishId",
            ),
    )
    var dishes: List<Dish>,
    @Relation(
        parentColumn = "mealId",
        entity = MealInstance::class,
        entityColumn = "mealId",
    )
    var mealInstanceDetails: List<InstanceDetails>
)

/**
 * Converts a [MealWithDishes] to a list of [DishInMeal].
 */
fun MealWithDishes.toDishesInMeal(): List<DishInMeal> =
    dishes.map {
            dish -> DishInMeal(mealId = meal.mealId, dishId = dish.dishId)
    }

/**
 * Converts an [MealWithDishesInstance] to a list of [DishInMeal].
 */
fun MealWithDishesInstance.toDishesInMeal(): List<DishInMeal> =
    mealWithDishes.dishes.map {
            dish -> DishInMeal(mealId = mealWithDishes.meal.mealId, dishId = dish.dishId)
    }

/**
 * Converts a [MealWithDishesInstance] to a [MealWithDishes]
 */
fun MealWithDishesInstance.toMealWithDishes(): MealWithDishes =
    MealWithDishes(meal = mealWithDishes.meal, dishes = mealWithDishes.dishes)

/**
 * Converts a [MealWithDishesAndInstance] to a [MealInstance]
 */
fun MealWithDishesInstance.toMealInstance(): MealInstance =
    MealInstance(
        mealInstanceId = instanceDetails.mealInstanceId,
        mealId = mealWithDishes.meal.mealId,
        date = instanceDetails.date,
        occasion = instanceDetails.occasion,
        userId = instanceDetails.userId
    )

@DatabaseView(
    "SELECT meals.mealId AS mealId, meals.rating, meals.name, meals.imgSrc AS imgSrc, " +
        " meal_instances.mealInstanceId AS mealInstanceId, meal_instances.userId AS userId, " +
        "meal_instances.date, meal_instances.occasion " +
        "FROM meals INNER JOIN meal_instances ON meals.mealId = meal_instances.mealId"
)
data class ViewMealsAndInstances(
    val mealId: Long,
    val rating: Rating,
    val name: String,
    val imgSrc: String?,
    val mealInstanceId: Long,
    val userId: Long,
    val date: LocalDate,
    val occasion: Occasion,
)

@DatabaseView(
    "SELECT meals.mealId AS mealId, meals.rating AS rating, " +
    "meals.name AS name,  meals.imgSrc AS imgSrc " +
    "FROM meals"
)
data class ViewMeals(
    val mealId: Long,
    val rating: Rating,
    val name: String,
    val imgSrc: String?,
)
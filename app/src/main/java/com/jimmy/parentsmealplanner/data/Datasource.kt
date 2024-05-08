package com.jimmy.parentsmealplanner.data

import com.jimmy.parentsmealplanner.model.Dish
import com.jimmy.parentsmealplanner.model.DishInMeal
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealInstance
import com.jimmy.parentsmealplanner.model.MealWithDishes
import com.jimmy.parentsmealplanner.model.MealWithDishesAndAllInstances
import com.jimmy.parentsmealplanner.model.User
import com.jimmy.parentsmealplanner.model.toInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.Rating
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * [Datasource] provides a pre-made list of Meals, Dishes, and DishInMeals.
 */
class Datasource {
    companion object {
        private val users: List<User> =
            listOf(
                User(userId = 1, name = "user1"),
                User(userId = 2, name = "user2"),
                User(userId = 3, name = "user3"),
            )
                

        private val meals: List<Meal> =
            listOf(
                Meal(mealId = 1, rating = Rating.LEARNING, name = "meal1"),
                Meal(mealId = 2, rating = Rating.LIKEIT, name = "meal2"),
                Meal(mealId = 3, rating = Rating.LOVEIT, name = "meal3"),
                Meal(mealId = 4, rating = Rating.LOVEIT, name = "meal4"),
                Meal(mealId = 5, rating = Rating.LOVEIT, name = "meal5"),
                Meal(mealId = 6, rating = Rating.LEARNING, name = "meal6"),
                Meal(mealId = 7, rating = Rating.LIKEIT, name = "meal7"),
                Meal(mealId = 8, rating = Rating.LOVEIT, name = "meal8"),
                Meal(mealId = 9, rating = Rating.LOVEIT, name = "meal9"),
                Meal(mealId = 10, rating = Rating.LOVEIT, name = "meal10"),
            )

        private val dishes: List<Dish> =
            listOf(
                Dish(dishId = 1, name = "dish1", rating = Rating.LEARNING),
                Dish(dishId = 2, name = "dish2", rating = Rating.LIKEIT),
                Dish(dishId = 3, name = "dish3", rating = Rating.LOVEIT),
                Dish(dishId = 4, name = "dish4", rating = Rating.LOVEIT),
                Dish(dishId = 5, name = "dish5", rating = Rating.LOVEIT),
                Dish(dishId = 6, name = "dish6", rating = Rating.LEARNING),
                Dish(dishId = 7, name = "dish7", rating = Rating.LIKEIT),
                Dish(dishId = 8, name = "dish8", rating = Rating.LOVEIT),
                Dish(dishId = 9, name = "dish9", rating = Rating.LOVEIT),
                Dish(dishId = 10, name =  "dish10", rating = Rating.LOVEIT),
            )

        private val dishesInMeals: List<DishInMeal> =
            listOf(
                DishInMeal(1, 1),
                DishInMeal(1, 2),
                DishInMeal(1, 3),
                DishInMeal(2, 2),
                DishInMeal(2, 3),
                DishInMeal(3, 3),
                DishInMeal(3, 4),
                DishInMeal(4, 4),
                DishInMeal(4, 5),
                DishInMeal(5, 5),
                DishInMeal(5, 6),
                DishInMeal(6, 6),
                DishInMeal(6, 7),
                DishInMeal(7, 7),
                DishInMeal(7, 8),
                DishInMeal(8, 8),
                DishInMeal(8, 9),
                DishInMeal(9, 9),
                DishInMeal(9, 10),
                DishInMeal(10, 10),
                DishInMeal(10, 1),
            )

        private val mealsWithDishes: List<MealWithDishes> =
            meals.map { meal ->
                val dishesForMeal =
                    dishesInMeals
                        .filter { it.mealId == meal.mealId }
                        .mapNotNull { dishInMeal -> dishes.find { it.dishId == dishInMeal.dishId } }
                MealWithDishes(meal, dishesForMeal)
            }

        private val mealInstances: List<MealInstance> =
            meals.flatMap { meal ->
                val mealInstancesForMeal =
                    (1..Random.nextInt(1, 3)).map {
                        MealInstance(
                            mealInstanceId = Random.nextLong(1, 101),
                            mealId = meal.mealId,
                            date = Clock.System.now().toLocalDateTime(
                                TimeZone.currentSystemDefault()
                            ).date.plus(DatePeriod(days = Random.nextInt(0, 7))),
                            occasion =
                                Occasion.fromInt(Random.nextInt(0..5))
                                    ?: Occasion.BREAKFAST,
                            userId = Random.nextLong(1,4),
                        )
                    }
                mealInstancesForMeal
            }

        private val mealsWithDishesAndAllInstances: List<MealWithDishesAndAllInstances> =
            mealsWithDishes.map { mealWithDishes ->
                MealWithDishesAndAllInstances(
                    meal = mealWithDishes.meal,
                    dishes = mealWithDishes.dishes,
                    mealInstanceDetails =
                        mealInstances.filter { mealInstance ->
                            mealInstance.mealId == mealWithDishes.meal.mealId
                        }.map {
                            it.toInstanceDetails()
                        },
                )
            }

        fun loadUsers(): List<User> {
            return users
        }

        fun loadMeals(): List<Meal> {
            return meals
        }

        fun loadDishes(): List<Dish> {
            return dishes
        }

        fun loadDishInMeals(): List<DishInMeal> {
            return dishesInMeals
        }

        fun loadMealsWithDishes(): List<MealWithDishes> {
            return mealsWithDishes
        }

        fun loadMealInstances(): List<MealInstance> {
            return mealInstances
        }

        fun loadMealsWithDishesAndInstances(): List<MealWithDishesAndAllInstances> {
            return mealsWithDishesAndAllInstances
        }
    }
}
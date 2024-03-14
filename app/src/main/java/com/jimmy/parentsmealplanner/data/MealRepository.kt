package com.jimmy.parentsmealplanner.data

import com.jimmy.parentsmealplanner.model.Dish
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealWithDishes
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface MealRepository {
    fun getMealsInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate): Flow<List<Meal>>

    fun getMealStream(id: Int): Flow<Meal?>

    suspend fun getMealWithDishes(id: Int): MealWithDishes?

    fun getMealsWithDishesInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<MealWithDishes>>

    fun getMealWithDishesStream(id: Int): Flow<MealWithDishes?>

    suspend fun insertMeal(meal: Meal)

    suspend fun insertMealWithDishes(mealWithDishes: MealWithDishes)

    suspend fun upsertMealWithDishes(mealWithDishes: MealWithDishes)

    suspend fun deleteMeal(meal: Meal)

    suspend fun updateMeal(meal: Meal)

    fun searchForMeals(searchTerm: String): Flow<List<Meal>>

    suspend fun deleteDishesFromMeal(mealId: Int, dishes: List<Dish>)
}

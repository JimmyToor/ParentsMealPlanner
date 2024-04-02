package com.jimmy.parentsmealplanner.data

import com.jimmy.parentsmealplanner.model.Dish
import com.jimmy.parentsmealplanner.model.Instance
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealInstance
import com.jimmy.parentsmealplanner.model.MealWithDishes
import com.jimmy.parentsmealplanner.model.MealWithDishesAndAllInstances
import com.jimmy.parentsmealplanner.ui.shared.UserDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface MealRepository {
    fun getMealsInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate): Flow<List<Meal>>

    fun getMealStream(id: Long): Flow<Meal?>

    suspend fun getMealWithDishes(id: Long): MealWithDishes?

    suspend fun getMealWithDishesAndInstance(instanceId: Long): Instance?

    fun getInstanceInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<Instance>>

    fun getMealsWithDishesAndInstancesInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<MealWithDishesAndAllInstances>>

    fun getMealWithDishesStream(id: Long): Flow<MealWithDishes?>

    suspend fun insertMeal(meal: Meal): Long

    suspend fun insertMealWithDishes(mealWithDishes: MealWithDishes): MealWithDishes

    suspend fun updateMealWithDishes(mealWithDishes: MealWithDishes): MealWithDishes

    suspend fun upsertInstance(instance: Instance): Instance

    suspend fun insertMealInstance(mealInstance: MealInstance): Long

    suspend fun updateMealInstance(mealInstance: MealInstance)

    suspend fun upsertMealInstance(mealInstance: MealInstance): Long

    suspend fun deleteMealInstance(instanceId: Long)

    suspend fun deleteMeal(meal: Meal)

    suspend fun updateMeal(meal: Meal)

    suspend fun deleteDishesInNoMeal(dishes: List<Dish>)

    fun searchForMeal(searchTerm: String): Flow<List<Meal>>

    fun searchForDish(searchTerm: String): Flow<List<Dish>>

    suspend fun deleteDishesFromMeal(mealId: Long, dishes: List<Dish>)

    suspend fun getUserDetails(id: Long): UserDetails?

    suspend fun insertUser(user: UserDetails): Long

    suspend fun updateUserName(id: Long, name: String)
}

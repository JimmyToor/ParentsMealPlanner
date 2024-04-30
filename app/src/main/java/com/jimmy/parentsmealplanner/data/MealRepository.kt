package com.jimmy.parentsmealplanner.data

import com.jimmy.parentsmealplanner.model.Dish
import com.jimmy.parentsmealplanner.model.DishInMeal
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealInstance
import com.jimmy.parentsmealplanner.model.MealWithDishes
import com.jimmy.parentsmealplanner.model.MealWithDishesAndAllInstances
import com.jimmy.parentsmealplanner.model.MealWithDishesInstance
import com.jimmy.parentsmealplanner.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface MealRepository {
    fun getMealsInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate): Flow<List<Meal>>

    fun getMealStream(id: Long): Flow<Meal?>

    suspend fun getMealWithDishes(id: Long): MealWithDishes?

    suspend fun getMealWithDishesAndInstance(instanceId: Long): MealWithDishesInstance?

    fun getInstanceInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<MealWithDishesInstance>>

    fun getMealsWithDishesAndInstancesInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<MealWithDishesAndAllInstances>>

    fun getMealWithDishesStream(id: Long): Flow<MealWithDishes?>

    suspend fun insertMeal(meal: Meal): Long

    suspend fun updateMeal(meal: Meal): Boolean

    suspend fun deleteMeal(meal: Meal)

    suspend fun updateDish(dish: Dish): Boolean

    suspend fun upsertMealWithDishes(mealWithDishes: MealWithDishes): MealWithDishes

    suspend fun upsertMealWithDishesInstance(mealWithDishesInstance: MealWithDishesInstance)
        : MealWithDishesInstance

    suspend fun insertMealInstance(mealInstance: MealInstance): Long

    suspend fun updateMealInstance(mealInstance: MealInstance)

    suspend fun upsertMealInstance(mealInstance: MealInstance): Long

    suspend fun deleteMealInstance(instanceId: Long)

    suspend fun deleteDishesInNoMeal(dishes: List<Dish>)

    fun searchForMeal(searchTerm: String): Flow<List<Meal>>

    fun searchForMealWithDishes(searchTerm: String): Flow<List<MealWithDishes>>

    fun searchForDish(searchTerm: String): Flow<List<Dish>>

    suspend fun deleteDishesFromMeal(dishesInMeal: List<DishInMeal>)

    suspend fun getUser(id: Long): User?

    suspend fun getAllUsers(): List<User>

    suspend fun insertUser(user: User): Long

    suspend fun updateUser(user: User)

    suspend fun deleteUser(user: User)
    fun getAllUsersStream(): Flow<List<User>>
}

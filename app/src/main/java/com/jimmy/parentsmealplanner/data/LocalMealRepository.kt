package com.jimmy.parentsmealplanner.data

import com.jimmy.parentsmealplanner.model.Dish
import com.jimmy.parentsmealplanner.model.DishDao
import com.jimmy.parentsmealplanner.model.DishInMeal
import com.jimmy.parentsmealplanner.model.DishInMealDao
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealDao
import com.jimmy.parentsmealplanner.model.MealInstance
import com.jimmy.parentsmealplanner.model.MealInstanceDao
import com.jimmy.parentsmealplanner.model.MealWithDishes
import com.jimmy.parentsmealplanner.model.MealWithDishesAndAllInstances
import com.jimmy.parentsmealplanner.model.MealWithDishesInstance
import com.jimmy.parentsmealplanner.model.User
import com.jimmy.parentsmealplanner.model.UserDao
import com.jimmy.parentsmealplanner.model.toDish
import com.jimmy.parentsmealplanner.model.toDishesInMeal
import com.jimmy.parentsmealplanner.model.toMealInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class LocalMealRepository @Inject constructor(
    private val mealDao: MealDao,
    private val dishDao: DishDao,
    private val dishInMealDao: DishInMealDao,
    private val mealInstanceDao: MealInstanceDao,
    private val userDao: UserDao,
) : MealRepository {
    override fun getMealsInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<Meal>> = mealDao.getMealsInDateRangeStream(dateStart, dateEnd)

    override fun getMealStream(id: Long): Flow<Meal?> = mealDao.getMealStream(id)

    override fun getInstanceInDateRangeStream(
        dateStart: LocalDate,
        dateEnd: LocalDate
    ): Flow<List<MealWithDishesInstance>> =
        mealDao.getInstancesInDateRangeStream(dateStart, dateEnd)

    override fun getMealsWithDishesAndInstancesInDateRangeStream(
        dateStart: LocalDate,
        dateEnd: LocalDate
    ): Flow<List<MealWithDishesAndAllInstances>> =
        mealDao.getMealsWithDishesAndAllInstancesInDateRangeStream(dateStart, dateEnd)

    override suspend fun getMealWithDishes(id: Long): MealWithDishes? =
        mealDao.getMealWithDishes(id)

    override suspend fun getMealWithDishesAndInstance(instanceId: Long):
        MealWithDishesInstance? = mealDao.getMealWithDishesAndInstance(instanceId)

    override fun getMealWithDishesStream(id: Long): Flow<MealWithDishes> =
        mealDao.getMealWithDishesStream(id)

    override suspend fun insertMeal(meal: Meal): Long = mealDao.insert(meal)

    /**
     * Upsert a mealWithDishes and return the mealWithDishes with updated IDs.
     * @param mealWithDishes The mealWithDishes to upsert
     * @return The updated mealWithDishes
     */
    override suspend fun upsertMealWithDishes(mealWithDishes: MealWithDishes): MealWithDishes {
        var mealId = mealDao.upsert(mealWithDishes.meal)
        val dishIds = dishDao.upsertAll(mealWithDishes.dishes)

        if (mealId == -1L) {
            mealId = mealWithDishes.meal.mealId
        }

        // Update the mealId in the meal and associated dishes
        return mealWithDishes.copy(
            meal = mealWithDishes.meal.copy(mealId = mealId),
            dishes = dishIds.mapIndexed { index, newId ->
                mealWithDishes.dishes[index].copy(
                    dishId = if (newId != -1L) newId else mealWithDishes.dishes[index].dishId
                )
            }
        ).also { dishInMealDao.upsertAll(it.toDishesInMeal()) }
    }

    /**
     * Upsert a mealWithDishesInstance and return the mealWithDishesInstance with updated IDs.
     * @param mealWithDishesInstance The mealWithDishesInstance to upsert
     * @return The updated mealWithDishesInstance
     */
    override suspend fun upsertMealWithDishesInstance(
        mealWithDishesInstance: MealWithDishesInstance): MealWithDishesInstance {
        mealWithDishesInstance.mealWithDishes =
            upsertMealWithDishes(mealWithDishesInstance.mealWithDishes)

        val newInstanceId = upsertMealInstance(mealWithDishesInstance.toMealInstance())
        mealWithDishesInstance.instanceDetails.mealInstanceId = newInstanceId

        return mealWithDishesInstance
    }

    override suspend fun insertMealInstance(mealInstance: MealInstance): Long =
        mealInstanceDao.upsert(mealInstance)

    override suspend fun updateMealInstance(mealInstance: MealInstance) =
        mealInstanceDao.update(mealInstance)

    override suspend fun deleteMealInstance(instanceId: Long) =
        mealInstanceDao.deleteById(instanceId)

    override suspend fun upsertMealInstance(mealInstance: MealInstance): Long =
        mealInstanceDao.upsert(mealInstance)

    override suspend fun deleteDishesFromMeal(dishesInMeal: List<DishInMeal>) {
        dishInMealDao.deleteAll(dishesInMeal)
        deleteDishesInNoMeal(dishesInMeal.map { it.toDish() })
    }

    override suspend fun deleteDishesInNoMeal(dishes: List<Dish>)
    {
        val dishesFound = dishInMealDao.getDishesInMeals(
            dishes.map { it.dishId }
        ).map { it.toDish() }.toSet()

        val dishesToDelete = dishes.minus(dishesFound)

        dishDao.deleteAll(dishesToDelete)
    }

    override fun searchForMeal(searchTerm: String): Flow<List<Meal>> {
        return when (searchTerm.isBlank()) {
            true -> flowOf(emptyList())
            false -> mealDao.searchForMealStream(searchTerm)
        }
    }

    override fun searchForMealWithDishes(searchTerm: String): Flow<List<MealWithDishes>> {
        return when (searchTerm.isBlank()) {
            true -> flowOf(emptyList())
            false -> mealDao.searchForMealWithDishesStream(searchTerm)
        }
    }

    override fun searchForDish(searchTerm: String): Flow<List<Dish>> {
        return when (searchTerm.isBlank()) {
            true -> flowOf(emptyList())
            false -> dishDao.searchForDish(searchTerm)
        }
    }

    override suspend fun deleteMeal(meal: Meal) = mealDao.delete(meal)

    override suspend fun getMealByName(name: String): Meal? = mealDao.getMealByName(name)

    override suspend fun updateMeal(meal: Meal) = mealDao.update(meal)

    override suspend fun updateDish(dish: Dish) = dishDao.update(dish)

    override suspend fun getDishByName(name: String): Dish? {
        return dishDao.getDishByName(name)
    }

    override suspend fun getUser(id: Long): User? = userDao.getUser(id)

    override suspend fun getAllUsers(): List<User> = userDao.getAllUsers()


    override suspend fun insertUser(user: User): Long = userDao.insert(user)

    override suspend fun updateUser(user: User) =
        userDao.update(user)

    override suspend fun deleteUser(user: User) = userDao.delete(user)

    override fun getAllUsersStream(): Flow<List<User>> {
        return userDao.getAllUsersStream()
    }
}
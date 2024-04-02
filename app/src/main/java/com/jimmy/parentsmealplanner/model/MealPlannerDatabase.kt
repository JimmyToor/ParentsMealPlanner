package com.jimmy.parentsmealplanner.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Dish::class,
        Meal::class,
        DishInMeal::class,
        MealInstance::class,
        User::class,
    ],
    views = [
        ViewMealsAndInstances::class,
        ViewMeals::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MealPlannerDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    abstract fun dishDao(): DishDao

    abstract fun dishInMealDao(): DishInMealDao

    abstract fun mealInstanceDao(): MealInstanceDao

    abstract fun plannerUserDao(): UserDao

    companion object {
        @Volatile
        private var instance: MealPlannerDatabase? = null

        fun getDatabase(context: Context): MealPlannerDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    MealPlannerDatabase::class.java,
                    "meal_planner_database",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
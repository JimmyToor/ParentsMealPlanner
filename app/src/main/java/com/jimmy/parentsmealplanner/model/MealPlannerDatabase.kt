package com.jimmy.parentsmealplanner.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Dish::class, Meal::class],
    version = 1,
    exportSchema = false,
)
abstract class MealPlannerDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao

    abstract fun mealDao(): MealDao

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
package com.jimmy.parentsmealplanner.hiltmodule

import android.content.Context
import com.jimmy.parentsmealplanner.model.DishDao
import com.jimmy.parentsmealplanner.model.DishInMealDao
import com.jimmy.parentsmealplanner.model.MealDao
import com.jimmy.parentsmealplanner.model.MealPlannerDatabase
import com.jimmy.parentsmealplanner.ui.shared.MealDetails
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MealPlannerDatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): MealPlannerDatabase {
        return MealPlannerDatabase.getDatabase(appContext)
    }

    @Provides
    fun provideMealDao(appDatabase: MealPlannerDatabase): MealDao {
        return appDatabase.mealDao()
    }

    @Provides
    fun provideDishDao(appDatabase: MealPlannerDatabase): DishDao {
        return appDatabase.dishDao()
    }

    @Provides
    fun provideDishInMealDao(appDatabase: MealPlannerDatabase): DishInMealDao {
        return appDatabase.dishInMealDao()
    }

    @Provides
    fun provideMealDetails(): MealDetails {
        return MealDetails()
    }
}
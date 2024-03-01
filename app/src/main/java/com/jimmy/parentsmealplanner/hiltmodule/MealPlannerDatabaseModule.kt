package com.jimmy.parentsmealplanner.hiltmodule

import android.content.Context
import com.jimmy.parentsmealplanner.model.MealDao
import com.jimmy.parentsmealplanner.model.MealPlannerDatabase
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
}
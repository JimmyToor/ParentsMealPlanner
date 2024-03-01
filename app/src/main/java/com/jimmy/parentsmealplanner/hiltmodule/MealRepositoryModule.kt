package com.jimmy.parentsmealplanner.hiltmodule

import com.jimmy.parentsmealplanner.data.LocalMealRepository
import com.jimmy.parentsmealplanner.data.MealRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class MealRepositoryModule {
     @Binds
     abstract fun bindLocalMealRepository(
         localMealRepository: LocalMealRepository
     ): MealRepository
}
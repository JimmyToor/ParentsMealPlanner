package com.jimmy.parentsmealplanner.hiltmodule

import com.jimmy.parentsmealplanner.data.LocalUserPreferencesRepository
import com.jimmy.parentsmealplanner.data.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class UserPreferencesModule {
    @Binds
    abstract fun bindLocalUserPreferencesRepository(
        localUserPreferencesRepository: LocalUserPreferencesRepository
    ): UserPreferencesRepository
}
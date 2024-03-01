package com.jimmy.parentsmealplanner

import AppContainer
import AppDataContainer
import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ParentsMealPlannerApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}
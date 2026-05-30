package com.example.speak2easy

import android.app.Application
import com.example.speak2easy.di.AppContainer

/**
 * Application entry point. Hosts the manual DI container ([AppContainer]),
 * which wires up preferences now and networking/repositories in later phases.
 */
class Speak2EasyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

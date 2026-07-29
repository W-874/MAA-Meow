package com.aliothmoon.maameow

import android.app.Application
import com.aliothmoon.maameow.koin.appModule
import com.aliothmoon.maameow.koin.floatingWindowModule
import com.aliothmoon.maameow.koin.useCaseModule
import com.aliothmoon.maameow.koin.viewModelModule
import com.aliothmoon.maameow.utils.CrashHandler
import com.aliothmoon.maameow.utils.PerformanceTrace
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MaaApplication : Application() {

    private val crashHandler: CrashHandler by inject()
    private val initializationCoordinator: AppInitializationCoordinator by inject()
    override fun onCreate() {
        super.onCreate()
        PerformanceTrace.section("MaaApplication.onCreate") {
            val app = this
            startKoin {
                androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
                androidContext(app)
                modules(appModule, useCaseModule, viewModelModule, floatingWindowModule)
            }
            crashHandler.init(this)
        }
    }

    suspend fun ensureUiBootstrapReady() = initializationCoordinator.ensureMinimalReady()

    fun startDeferredServices() {
        initializationCoordinator.startAfterFirstInteractive()
    }
}

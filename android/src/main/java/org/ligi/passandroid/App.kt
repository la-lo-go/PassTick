package org.ligi.passandroid

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.ligi.passandroid.model.AndroidFileSystemPassStore
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.createPassMoshi
import org.ligi.passandroid.platform.AndroidPlatformActions
import org.ligi.passandroid.platform.PlatformActions
import org.ligi.passandroid.repository.DataStoreSettingsRepository
import org.ligi.passandroid.repository.FilePassRepository
import org.ligi.passandroid.repository.PassRepository
import org.ligi.passandroid.repository.SettingsRepository
import org.ligi.passandroid.ui.state.MainViewModel

open class App : Application() {

    private val moshi = createPassMoshi()

    open fun createKoin(): Module {

        return module {
            single { AndroidFileSystemPassStore(this@App, moshi) as PassStore }
            single<Tracker> { LocalTracker() }
            single<PassRepository> { FilePassRepository(this@App, get(), get()) }
            single<SettingsRepository> { DataStoreSettingsRepository(this@App) }
            single<PlatformActions> { AndroidPlatformActions(this@App) }
            viewModel { MainViewModel(get(), get()) }
        }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@App)
            modules(createKoin())
        }

        AndroidThreeTen.init(this)
    }
}

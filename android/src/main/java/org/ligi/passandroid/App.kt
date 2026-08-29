package org.ligi.passandroid

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.jakewharton.threetenabp.AndroidThreeTen
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.ligi.passandroid.model.AndroidFileSystemPassStore
import org.ligi.passandroid.model.AndroidSettings
import org.ligi.passandroid.model.PassStore
import org.ligi.passandroid.model.Settings
import org.ligi.passandroid.model.createPassMoshi
import org.ligi.passandroid.scan.events.PassScanEventChannelProvider
import org.ligi.tracedroid.TraceDroid

open class App : Application() {

    private val moshi = createPassMoshi()

    private val settings by lazy { AndroidSettings(this) }

    open fun createKoin(): Module {

        return module {
            single { AndroidFileSystemPassStore(this@App, get(), moshi) as PassStore }
            single { settings as Settings }
            single<Tracker> { LocalTracker() }
            single { PassScanEventChannelProvider() }
        }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@App)
            modules(createKoin())
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        AndroidThreeTen.init(this)
        initTraceDroid()

        AppCompatDelegate.setDefaultNightMode(settings.getNightMode())
    }


    private fun initTraceDroid() {
        TraceDroid.init(this)
    }

}

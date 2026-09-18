package org.ligi.passandroid

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
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
import org.ligi.passandroid.repository.FilePinnedStore
import org.ligi.passandroid.repository.FilePassRepository
import org.ligi.passandroid.repository.PassRepository
import org.ligi.passandroid.repository.SettingsRepository
import org.ligi.passandroid.ui.state.MainViewModel
import org.ligi.passandroid.ui.state.StringResolver
import org.ligi.passandroid.reminder.AndroidReminderScheduler
import org.ligi.passandroid.reminder.ReminderScheduler
import org.ligi.passandroid.widget.PassWidgetSnapshotPublisher
import org.ligi.passandroid.widget.WidgetSnapshotPublisher
import java.io.File

open class App : Application() {

    private val moshi = createPassMoshi()
    private var currentActivity: Activity? = null

    open fun createKoin(): Module {

        return module {
            single { AndroidFileSystemPassStore(this@App, moshi) as PassStore }
            single<Tracker> { LocalTracker() }
            single<org.ligi.passandroid.repository.FileFavoriteStore> {
                FilePinnedStore(
                    java.io.File(filesDir, "pass-pinned.json"),
                    java.io.File(filesDir, "pass-favorites.json"),
                )
            }
            single<PassRepository> { FilePassRepository(this@App, get(), get(), favoriteStore = get()) }
            single<SettingsRepository> { DataStoreSettingsRepository(this@App) }
            single<PlatformActions> { AndroidPlatformActions(this@App, activityProvider = { currentActivity }) }
            single<ReminderScheduler> { AndroidReminderScheduler(this@App) }
            single<WidgetSnapshotPublisher> { PassWidgetSnapshotPublisher(this@App) }
            single<StringResolver> {
                val context = get<Context>()
                object : StringResolver {
                    override fun resolve(id: Int, vararg args: Any): String = context.getString(id, *args)
                }
            }
            viewModel { MainViewModel(get(), get(), get(), get(), get(), get()) }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Shared pass and image files are transient; remove leftovers from a previous session.
        File(cacheDir, "share").deleteRecursively()
        File(cacheDir, "import").deleteRecursively()
        File(cacheDir, "capture").deleteRecursively()

        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }
            override fun onActivityPaused(activity: Activity) {
                if (currentActivity === activity) currentActivity = null
            }
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })

        startKoin {
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@App)
            modules(createKoin())
        }

        AndroidThreeTen.init(this)
    }
}

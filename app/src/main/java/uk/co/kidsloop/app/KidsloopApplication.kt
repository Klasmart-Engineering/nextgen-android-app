package uk.co.kidsloop.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import uk.co.kidsloop.BuildConfig

@HiltAndroidApp
class KidsloopApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}

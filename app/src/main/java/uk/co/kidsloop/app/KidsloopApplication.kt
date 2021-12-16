package uk.co.kidsloop.app

import android.app.Application
import uk.co.kidsloop.app.di.app.AppComponent
import uk.co.kidsloop.app.di.app.AppModule
import uk.co.kidsloop.app.di.app.DaggerAppComponent

class KidsloopApplication : Application() {

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.builder().appModule(AppModule(this)).build()
    }

    override fun onCreate() {
        super.onCreate()
    }
}
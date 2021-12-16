package uk.co.kidsloop.app.di.app

import dagger.Component
import uk.co.kidsloop.app.di.activity.ActivityComponent

@AppScope
@Component(modules = [AppModule::class])
interface AppComponent {
    fun newActivityComponentBuilder(): ActivityComponent.Builder
}
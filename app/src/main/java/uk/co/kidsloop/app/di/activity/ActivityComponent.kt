package uk.co.kidsloop.app.di.activity

import androidx.appcompat.app.AppCompatActivity
import dagger.BindsInstance
import dagger.Subcomponent
import uk.co.kidsloop.app.di.presentation.PresentationComponent
import uk.co.kidsloop.app.di.presentation.PresentationModule

@ActivityScope
@Subcomponent
interface ActivityComponent {

    fun newPresentationComponent(fragmentModule: PresentationModule): PresentationComponent

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun activity(activity: AppCompatActivity): Builder
        fun build(): ActivityComponent
    }
}
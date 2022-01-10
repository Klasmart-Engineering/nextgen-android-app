package uk.co.kidsloop.app.di.presentation

import dagger.Subcomponent
import uk.co.kidsloop.app.features.splash.SplashFragment
import uk.co.kidsloop.app.features.videostream.LiveVideoStreamFragment
import uk.co.kidsloop.features.videostream.PreviewFragment

@PresentationScope
@Subcomponent(modules = [PresentationModule::class, ViewModelModule::class])
interface PresentationComponent {
    fun inject(fragment:SplashFragment)
    fun inject(fragment:LiveVideoStreamFragment)
    fun inject(fragment: PreviewFragment)
}
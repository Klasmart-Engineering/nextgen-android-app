package uk.co.kidsloop.app.di.presentation

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import uk.co.kidsloop.features.videostream.LiveVideoStreamViewModel
import uk.co.kidsloop.features.videostream.PreviewViewModel

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(LiveVideoStreamViewModel::class)
    abstract fun locationViewModel(liveVideoStreamViewModel: LiveVideoStreamViewModel): ViewModel
    @Binds
    @IntoMap
    @ViewModelKey(PreviewViewModel::class)
    abstract fun locationPreviewViewModel(previewViewModel: PreviewViewModel): ViewModel
}
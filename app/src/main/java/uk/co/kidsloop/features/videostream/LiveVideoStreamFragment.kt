package uk.co.kidsloop.app.features.videostream

import android.os.Bundle
import androidx.fragment.app.viewModels
import uk.co.kidsloop.R
import uk.co.kidsloop.app.BaseFragment
import uk.co.kidsloop.app.viewmodel.ViewModelFactory
import uk.co.kidsloop.features.videostream.LiveVideoStreamViewModel
import javax.inject.Inject

class LiveVideoStreamFragment: BaseFragment(R.layout.live_videostream_fragment) {

    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: LiveVideoStreamViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
    }
}
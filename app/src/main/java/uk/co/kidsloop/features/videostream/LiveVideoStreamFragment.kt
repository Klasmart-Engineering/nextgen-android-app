package uk.co.kidsloop.features.videostream

import android.content.pm.ActivityInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.FragmentLiveVideostreamBinding

class LiveVideoStreamFragment : BaseFragment(R.layout.fragment_live_videostream) {

    private lateinit var binding: FragmentLiveVideostreamBinding
    private val viewModel by viewModels<LiveVideoStreamViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_live_videostream, container, false)
        return binding.root
    }

    companion object {
        val TAG = LiveVideoStreamFragment::class.qualifiedName
    }
}

package uk.co.kidsloop.features.liveclass

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.liveswitch.AudioStream
import fm.liveswitch.VideoStream
import javax.inject.Inject

@HiltViewModel
class LiveClassViewModel @Inject constructor(private val openSfuUpstreamConnectionUseCase: OpenSfuUpstreamConnectionUseCase) : ViewModel() {

    fun openSfuUpstreamConnection(audioStream: AudioStream?, videoStream: VideoStream?) {
        val sfuUpstreamConnection = openSfuUpstreamConnectionUseCase.openSfuUpstreamConnection(audioStream, videoStream)
    }
}
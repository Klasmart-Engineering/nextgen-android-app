package uk.co.kidsloop.features.liveclass

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.liveswitch.AudioStream
import fm.liveswitch.Channel
import fm.liveswitch.Future
import fm.liveswitch.IAction1
import fm.liveswitch.Promise
import fm.liveswitch.SfuUpstreamConnection
import fm.liveswitch.VideoStream
import javax.inject.Inject

@HiltViewModel
class LiveClassViewModel @Inject constructor(
    private val joinLiveClassUseCase: JoinLiveClassUseCase,
    private val openSfuUpstreamConnectionUseCase: OpenSfuUpstreamConnectionUseCase,
    private val liveClassManager: LiveClassManager
) : ViewModel() {

    private var _classroomStateLiveData = MutableLiveData<LiveClassState>()
    val classroomStateLiveData: LiveData<LiveClassState> get() = _classroomStateLiveData

    sealed class LiveClassState {
        object Loading : LiveClassState()
        data class RegistrationSuccessful(val channel: Channel) : LiveClassState()
        data class FailedToJoiningLiveClass(val message: String?) : LiveClassState()
    }

    fun joinLiveClass() {
        _classroomStateLiveData.value = LiveClassState.Loading
        joinLiveClassUseCase.joinAsync().then { channels ->
            _classroomStateLiveData.postValue(LiveClassState.RegistrationSuccessful(channels[0]))
        }.fail(IAction1 { exception ->
            _classroomStateLiveData.postValue(LiveClassState.FailedToJoiningLiveClass(exception.message))
        })
    }

    fun openSfuUpstreamConnection(audioStream: AudioStream?, videoStream: VideoStream?): SfuUpstreamConnection {
        val upstreamConnection =  openSfuUpstreamConnectionUseCase.openSfuUpstreamConnection(audioStream, videoStream)
        upstreamConnection.open()
        liveClassManager.setUpstreamConnection(upstreamConnection)
        return upstreamConnection
    }

    fun toggleLocalAudio(): Future<Any> {
        liveClassManager.getUpstreamConnection()?.let { upstreamConnection ->
            val config = upstreamConnection.config
            config.localAudioMuted = !config.localAudioMuted
            return upstreamConnection.update(config)
        }
        return Promise.resolveNow()
    }

    fun toggleLocalVideo(): Future<Any> {
        liveClassManager.getUpstreamConnection()?.let { upstreamConnection ->
            val config = upstreamConnection.config
            config.localVideoMuted = !config.localVideoMuted
            return upstreamConnection.update(config)
        }
        return Promise.resolveNow()
    }
}
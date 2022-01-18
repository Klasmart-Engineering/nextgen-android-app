package uk.co.kidsloop.features.liveclass

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.liveswitch.AudioStream
import fm.liveswitch.Channel
import fm.liveswitch.IAction1
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
        object LocalMediaTurnedOn : LiveClassState()
        object LocalMediaTurnedOff : LiveClassState()
        data class RegistrationSuccessful(val channel: Channel) : LiveClassState()
        data class FailedToJoiningLiveClass(val message: String?) : LiveClassState()
        object UnregisterSuccessful : LiveClassState()
    }

    fun joinLiveClass() {
        _classroomStateLiveData.value = LiveClassState.Loading
        joinLiveClassUseCase.joinAsync().then { channels ->
            _classroomStateLiveData.postValue(LiveClassState.RegistrationSuccessful(channels[0]))
        }.fail(IAction1 { exception ->
            _classroomStateLiveData.postValue(LiveClassState.FailedToJoiningLiveClass(exception.message))
        })
    }

    fun openSfuUpstreamConnection(audioStream: AudioStream?, videoStream: VideoStream?, isAudioTurnedOn:Boolean, isVideoTurnedOn:Boolean): SfuUpstreamConnection? {
        val upstreamConnection = openSfuUpstreamConnectionUseCase.openSfuUpstreamConnection(audioStream, videoStream)
        upstreamConnection?.let {
            liveClassManager.setUpstreamConnection(it)
        }
        val config = upstreamConnection?.config
        config?.localVideoMuted = !isVideoTurnedOn
        config?.localAudioMuted = !isAudioTurnedOn
        upstreamConnection?.update(config)
        return upstreamConnection
    }

    fun toggleLocalAudio() {
        liveClassManager.getUpstreamConnection()?.let { upstreamConnection ->
            val config = upstreamConnection.config
            config.localAudioMuted = !config.localAudioMuted
            upstreamConnection.update(config)
        }
    }

    fun toggleLocalVideo() {
        liveClassManager.getUpstreamConnection()?.let { upstreamConnection ->
            val config = upstreamConnection.config
            config.localVideoMuted = !config.localVideoMuted
            upstreamConnection.update(config)
            _classroomStateLiveData.value = if (config.localVideoMuted) LiveClassState.LocalMediaTurnedOff else LiveClassState.LocalMediaTurnedOn
        }
    }

    fun leaveLiveClass() {
        val client = liveClassManager.getClient()
        if (client != null) {
            client.unregister().then(IAction1 {
                _classroomStateLiveData.value = LiveClassState.UnregisterSuccessful
                liveClassManager.cleanConnection()
            }).fail(IAction1 { exception ->

            })
        }
    }
}
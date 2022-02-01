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

    private var _classroomStateLiveData = MutableLiveData<LiveClassUiState>()
    val classroomStateLiveData: LiveData<LiveClassUiState> get() = _classroomStateLiveData

    sealed class LiveClassUiState {
        object Loading : LiveClassUiState()
        object LocalMediaTurnedOn : LiveClassUiState()
        object LocalMediaTurnedOff : LiveClassUiState()
        data class RegistrationSuccessful(val channel: Channel) : LiveClassUiState()
        data class FailedToJoiningLiveClass(val message: String?) : LiveClassUiState()
        object UnregisterSuccessful : LiveClassUiState()
        object UnregisterFailed : LiveClassUiState()
    }

    fun joinLiveClass() {
        _classroomStateLiveData.value = LiveClassUiState.Loading
        joinLiveClassUseCase.joinAsync().then { channels ->
            _classroomStateLiveData.postValue(LiveClassUiState.RegistrationSuccessful(channels[0]))
        }.fail(IAction1 { exception ->
            _classroomStateLiveData.postValue(LiveClassUiState.FailedToJoiningLiveClass(exception.message))
        })
    }

    fun openSfuUpstreamConnection(
        audioStream: AudioStream?,
        videoStream: VideoStream?,
        isAudioTurnedOn: Boolean,
        isVideoTurnedOn: Boolean
    ): SfuUpstreamConnection? {
        //        liveClassManager.setDataChannels()
        //        liveClassManager.setDataChannelsListeners()

        val upstreamConnection =
            openSfuUpstreamConnectionUseCase.openSfuUpstreamConnection(audioStream, videoStream)
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
            _classroomStateLiveData.value = if (config.localVideoMuted) LiveClassUiState.LocalMediaTurnedOff else LiveClassUiState.LocalMediaTurnedOn
        }
    }

    fun leaveLiveClass() {
        val client = liveClassManager.getClient()
        client?.unregister()?.then {
            liveClassManager.cleanConnection()
            _classroomStateLiveData.postValue(LiveClassUiState.UnregisterSuccessful)
        }?.fail(IAction1 { exception ->
            liveClassManager.cleanConnection()
            _classroomStateLiveData.postValue(LiveClassUiState.UnregisterFailed)
        })
    }

    override fun onCleared() {
        super.onCleared()
        liveClassManager.cleanConnection()
    }
}
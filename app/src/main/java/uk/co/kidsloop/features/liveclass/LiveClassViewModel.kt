package uk.co.kidsloop.features.liveclass

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.liveswitch.AudioStream
import fm.liveswitch.Channel
import fm.liveswitch.ConnectionInfo
import fm.liveswitch.IAction1
import fm.liveswitch.SfuDownstreamConnection
import fm.liveswitch.SfuUpstreamConnection
import fm.liveswitch.VideoStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.co.kidsloop.data.enums.DataChannelActionsType
import uk.co.kidsloop.data.enums.SharedPrefsWrapper
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import uk.co.kidsloop.features.liveclass.usecases.JoinLiveClassUseCase
import uk.co.kidsloop.features.liveclass.usecases.OpenSfuDownstreamConnection
import uk.co.kidsloop.features.liveclass.usecases.OpenSfuUpstreamConnectionUseCase
import uk.co.kidsloop.features.liveclass.usecases.SendDataChannelEventUseCase
import uk.co.kidsloop.liveswitch.Config

@HiltViewModel
class LiveClassViewModel @Inject constructor(
    private val joinLiveClassUseCase: JoinLiveClassUseCase,
    private val openSfuUpstreamConnectionUseCase: OpenSfuUpstreamConnectionUseCase,
    private val openSfuDownstreamConnection: OpenSfuDownstreamConnection,
    private val sendDataChannelEventUseCase: SendDataChannelEventUseCase,
    private val liveClassManager: LiveClassManager
) : ViewModel() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    private var _classroomStateLiveData = MutableLiveData<LiveClassUiState>()
    val classroomStateLiveData: LiveData<LiveClassUiState> get() = _classroomStateLiveData

    sealed class LiveClassUiState {
        data class RegistrationSuccessful(val channel: Channel) : LiveClassUiState()
        data class FailedToJoiningLiveClass(val message: String?) : LiveClassUiState()
        object LiveClassStarted : LiveClassUiState()
        object LiveClassRestarted : LiveClassUiState()
        object UnregisterSuccessful : LiveClassUiState()
        object UnregisterFailed : LiveClassUiState()
    }

    fun joinLiveClass() {
        joinLiveClassUseCase.joinAsync().then { channels ->
            _classroomStateLiveData.postValue(LiveClassUiState.RegistrationSuccessful(channels[0]))
        }.fail(
            IAction1 { exception ->
                _classroomStateLiveData.postValue(
                    LiveClassUiState.FailedToJoiningLiveClass(
                        exception.message
                    )
                )
            }
        )
    }

    fun openSfuUpstreamConnection(
        audioStream: AudioStream?,
        videoStream: VideoStream?
    ): SfuUpstreamConnection? {
        val upstreamConnection =
            openSfuUpstreamConnectionUseCase.openSfuUpstreamConnection(audioStream, videoStream)
        return upstreamConnection
    }

    fun toggleLocalAudio() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                liveClassManager.getUpstreamConnection()?.let { upstreamConnection ->
                    val config = upstreamConnection.config
                    config.localAudioMuted = !config.localAudioMuted
                    upstreamConnection.update(config)
                }
            }
        }
    }

    fun toggleLocalVideo() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                liveClassManager.getUpstreamConnection()?.let { upstreamConnection ->
                    val config = upstreamConnection.config
                    config.localVideoMuted = !config.localVideoMuted
                    upstreamConnection.update(config)
                }
            }
        }
    }

    fun updateUpstreamConnection(isVideoOn: Boolean, isAudioOn: Boolean) {
//        viewModelScope.launch {
//            withContext(Dispatchers.IO) {
//                liveClassManager.getUpstreamConnection()?.let { upstreamConnection ->
//                    val config = upstreamConnection.config
//                    config.localVideoMuted = !isVideoOn
//                    config.localAudioMuted = !isAudioOn
//                    upstreamConnection.update(config)
//                }
//            }
//        }
    }

    fun turnOffVideoForStudents() {
        viewModelScope.launch {
            sendDataChannelEventUseCase.sendDataChannelEvent(DataChannelActionsType.DISABLE_VIDEO)
        }
    }

    fun turnOffVideo() {
        liveClassManager.getUpstreamConnection()?.let { upstreamConnection ->
            val config = upstreamConnection.config
            config.localVideoMuted = true
            upstreamConnection.update(config)
        }
    }

    fun turnOffAudio() {
        liveClassManager.getUpstreamConnection()?.let { upstreamConnection ->
            val config = upstreamConnection.config
            config.localAudioMuted = true
            upstreamConnection.update(config)
        }
    }

    fun enableVideoForStudents() {
        viewModelScope.launch {
            sendDataChannelEventUseCase.sendDataChannelEvent(DataChannelActionsType.ENABLE_VIDEO)
        }
    }

    fun disableMicForStudents() {
        viewModelScope.launch {
            sendDataChannelEventUseCase.sendDataChannelEvent(DataChannelActionsType.DISABLE_AUDIO)
        }
    }

    fun enableMicForStudents() {
        viewModelScope.launch {
            sendDataChannelEventUseCase.sendDataChannelEvent(DataChannelActionsType.ENABLE_AUDIO)
        }
    }

    fun showHandRaised() {
        viewModelScope.launch {
            sendDataChannelEventUseCase.sendDataChannelEvent(DataChannelActionsType.RAISE_HAND)
        }
    }

    fun showHandLowered() {
        viewModelScope.launch {
            sendDataChannelEventUseCase.sendDataChannelEvent(DataChannelActionsType.LOWER_HAND)
        }
    }

    fun leaveLiveClass() {
        val client = liveClassManager.getClient()
        client?.unregister()?.then {
            liveClassManager.cleanConnection()
            _classroomStateLiveData.postValue(LiveClassUiState.UnregisterSuccessful)
        }?.fail(
            IAction1 { exception ->
                liveClassManager.cleanConnection()
                _classroomStateLiveData.postValue(LiveClassUiState.UnregisterFailed)
            }
        )
    }

    fun openSfuDownstreamConnection(
        remoteConnectionInfo: ConnectionInfo,
        remoteMedia: SFURemoteMedia,
        shouldTurnOnCam: Boolean,
        shouldUnMuteMic: Boolean
    ): SfuDownstreamConnection? {
        val downStreamConnection =
            openSfuDownstreamConnection.openSfuDownstreamConnection(remoteConnectionInfo, remoteMedia)
        if (remoteConnectionInfo.clientRoles[0] == Config.TEACHER_ROLE) {
            if (liveClassManager.getState() == LiveClassState.TEACHER_DISCONNECTED) {
                liveClassManager.setState(LiveClassState.LIVE_CLASS_RESTARTED)
                _classroomStateLiveData.postValue(LiveClassUiState.LiveClassRestarted)
            } else {
                liveClassManager.setState(LiveClassState.LIVE_CLASS_STARTED)
                _classroomStateLiveData.postValue(LiveClassUiState.LiveClassStarted)
                updateUpstreamConnection(shouldTurnOnCam, shouldUnMuteMic)
            }
        }
        return downStreamConnection
    }

    override fun onCleared() {
        super.onCleared()
        liveClassManager.cleanConnection()
    }
}

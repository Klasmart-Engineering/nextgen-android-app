package uk.co.kidsloop.features.liveclass

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import fm.liveswitch.*
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.LiveClassFragmentBinding
import uk.co.kidsloop.app.UiThreadPoster
import uk.co.kidsloop.app.utils.emptyString
import uk.co.kidsloop.app.utils.gone
import uk.co.kidsloop.app.utils.visible
import uk.co.kidsloop.data.enums.DataChannelActions
import uk.co.kidsloop.features.liveclass.localmedia.CameraLocalMedia
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia
import uk.co.kidsloop.features.liveclass.localmedia.LocalMedia
import uk.co.kidsloop.liveswitch.DataChannelActionsHandler
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import uk.co.kidsloop.liveswitch.Config.STUDENT_ROLE
import uk.co.kidsloop.liveswitch.Config.TEACHER_ROLE
import javax.inject.Inject

@AndroidEntryPoint
class LiveClassFragment : BaseFragment(R.layout.live_class_fragment), DataChannelActionsHandler {

    companion object {

        val TAG = LiveClassFragment::class.qualifiedName
        const val IS_CAMERA_TURNED_ON = "isCameraTurnedOn"
        const val IS_MICROPHONE_TURNED_ON = "isMicrophoneTurnedOn"
    }

    @Inject
    lateinit var liveClassManager: LiveClassManager

    @Inject
    lateinit var uiThreadPoster: UiThreadPoster

    private val binding by viewBinding(LiveClassFragmentBinding::bind)
    private var localMedia: LocalMedia<View>? = null

    private val viewModel by viewModels<LiveClassViewModel>()

    private lateinit var studentsFeedAdapter: StudentsFeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localMedia = CameraLocalMedia(requireContext(), false, false, AecContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toggleCameraBtn.isChecked =
            !requireArguments().getBoolean(IS_CAMERA_TURNED_ON, true)
        binding.toggleMicrophoneBtn.isChecked =
            !requireArguments().getBoolean(IS_MICROPHONE_TURNED_ON, true)
        if (!requireArguments().getBoolean(IS_CAMERA_TURNED_ON)) {
            binding.localMediaContainer.showCameraTurnedOff()
        }
        if (!requireArguments().getBoolean(IS_MICROPHONE_TURNED_ON)) {
            binding.localMediaContainer.showMicMuted()
        }
        startLocalMedia()

        studentsFeedAdapter = StudentsFeedAdapter()
        binding.studentFeedsRecyclerview.apply {
            setHasFixedSize(true)
            adapter = studentsFeedAdapter
            layoutManager = LinearLayoutManager(context)
        }

        when (viewModel.sharedPrefsWrapper.getRole()) {
            TEACHER_ROLE -> {
                setUiForTeacher()
            }
            STUDENT_ROLE -> {
                setUiForStudent()
            }
        }

        observe()
        setControls()

        liveClassManager.dataChannelActionsHandler = this
    }

    private fun setUiForTeacher() {
        binding.raiseHandBtn.gone()
    }

    private fun setUiForStudent() {
        binding.raiseHandBtn.visible()
        binding.raiseHandBtn.isActivated = false
    }

    private fun setControls() {
        binding.toggleMicrophoneBtn.setOnClickListener {
            if (binding.toggleMicrophoneBtn.isChecked) {
                binding.localMediaContainer.showMicMuted()
            } else {
                binding.localMediaContainer.showMicTurnedOn()
            }
            viewModel.toggleLocalAudio()
        }

        binding.toggleCameraBtn.setOnClickListener {
            if (binding.toggleCameraBtn.isChecked) {
                binding.localMediaContainer.showCameraTurnedOff()
            } else {
                binding.localMediaContainer.showCameraTurnedOn()
            }
            viewModel.toggleLocalVideo()
        }

        binding.exitClassBtn.setOnClickListener {
            viewModel.leaveLiveClass()
        }

        binding.exitMenu.setOnClickListener {
            binding.liveClassOverlay.visibility = View.GONE
        }

        binding.moreBtn.setOnClickListener {
            binding.liveClassOverlay.visibility = View.VISIBLE
        }

        binding.raiseHandBtn.setOnClickListener {
            if (liveClassManager.getUpstreamConnection()?.state == ConnectionState.Connected) {
                binding.raiseHandBtn.isSelected = binding.raiseHandBtn.isSelected.not()
                val id = liveClassManager.getUpstreamConnection()?.clientId ?: emptyString()

                when (binding.raiseHandBtn.isSelected) {
                    true -> {
                        liveClassManager.sendDataString(DataChannelActions.RAISE_HAND.type + ":" + id)
                        binding.localMediaContainer.showHandRaised()
                    }
                    false -> {
                        liveClassManager.sendDataString(DataChannelActions.LOWER_HAND.type + ":" + id)
                        binding.localMediaContainer.hideRaiseHand()
                    }
                }
            }
        }
    }

    private fun observe() = with(viewModel) {
        viewModel.classroomStateLiveData.observe(viewLifecycleOwner, Observer
        {
            when (it) {
                is LiveClassViewModel.LiveClassUiState.Loading -> showLoading()
                is LiveClassViewModel.LiveClassUiState.RegistrationSuccessful -> onClientRegistered(it.channel)
                is LiveClassViewModel.LiveClassUiState.FailedToJoiningLiveClass -> handleFailures()
                //                is LiveClassViewModel.LiveClassUiState.LocalMediaTurnedOn -> turnOnLocalMedia()
                //                is LiveClassViewModel.LiveClassUiState.LocalMediaTurnedOff -> turnOffLocalMedia()
                is LiveClassViewModel.LiveClassUiState.UnregisterSuccessful -> stopLocalMedia()
                is LiveClassViewModel.LiveClassUiState.UnregisterFailed -> stopLocalMedia()
            }
        })
    }

    private fun openSfuDownstreamConnection(
        remoteConnectionInfo: ConnectionInfo,
        channel: Channel
    ): SfuDownstreamConnection {
        // Create remote media.
        val remoteMedia = SFURemoteMedia(
            requireContext(),
            disableAudio = false,
            disableVideo = false,
            aecContext = AecContext()
        )

        // Create audio and video streams from remote media.
        val audioStream: AudioStream? =
            if (remoteConnectionInfo.hasAudio) AudioStream(remoteMedia) else null
        val videoStream: VideoStream? =
            if (remoteConnectionInfo.hasVideo) VideoStream(remoteMedia) else null

        // Create a SFU downstream connection with remote audio and video and data streams.
        val connection: SfuDownstreamConnection =
            channel.createSfuDownstreamConnection(
                remoteConnectionInfo,
                audioStream,
                videoStream,
                liveClassManager.getNewDownstreamDataStream()
            )

        if (remoteConnectionInfo.clientRoles[0] == STUDENT_ROLE) {
            // Store the downstream connection.
            liveClassManager.saveDownStreamConnections(
                remoteConnectionInfo.clientId ?: emptyString(),
                connection
            )
        }

        // Adding remote view to UI.
        when (remoteConnectionInfo.clientRoles[0]) {
            TEACHER_ROLE -> {
                uiThreadPoster.post {
                    binding.teacherVideoFeed.tag = remoteConnectionInfo.clientId ?: emptyString()
                    binding.teacherVideoFeed.addView(remoteMedia.view, 1)
                }
            }

            STUDENT_ROLE -> uiThreadPoster.post {
                studentsFeedAdapter.addVideoFeed(remoteConnectionInfo.clientId, remoteMedia.view)
            }
        }

        connection.addOnStateChange { conn: ManagedConnection ->
            if (conn.state == ConnectionState.Closing || conn.state == ConnectionState.Failing) {
                val clientId = remoteConnectionInfo.clientId ?: emptyString()

                uiThreadPoster.post {
                    if (view != null) {
                        if (binding.teacherVideoFeed.tag == clientId) {
                            binding.teacherVideoFeed.removeViewAt(1)
                        } else {
                            studentsFeedAdapter.removeVideoFeed(clientId)
                        }
                    }
                }
                remoteMedia.destroy()
                liveClassManager.removeDownStreamConnection(clientId)
            } else if (conn.state == ConnectionState.Failed) {
                // Reconnect if the connection failed.
                openSfuDownstreamConnection(remoteConnectionInfo, channel)
            }
        }
        connection.open()
        return connection
    }

    private fun getAudioStream(localMedia: LocalMedia<View>?): AudioStream? {
        return if (localMedia?.audioTrack != null) AudioStream(localMedia.audioTrack) else null
    }

    private fun getVideoStream(localMedia: LocalMedia<View>?): VideoStream? {
        return if (localMedia?.videoTrack != null) VideoStream(localMedia.videoTrack) else null
    }

    private fun showLoading() {
    }

    private fun handleFailures() {
    }

    private fun onClientRegistered(channel: Channel) {
        openSfuUpstreamConnection()
        // Check for existing remote upstream connections and open a downstream connection for
        // each of them.
        for (connectionInfo in channel.remoteUpstreamConnectionInfos) {
            openSfuDownstreamConnection(connectionInfo, channel)
        }

        channel.addOnRemoteUpstreamConnectionOpen { connectionInfo ->
            openSfuDownstreamConnection(connectionInfo, channel)
        }
    }

    private fun stopLocalMedia() {
        localMedia?.stop()?.then { _ ->
            localMedia?.destroy()
            localMedia = null
            //TODO This is added for testing purpouse and it will be removed later on
            requireActivity().finish()
        }
    }

    private fun startLocalMedia() {
        if (liveClassManager.getState() == LiveClassState.IDLE) {
            localMedia?.start()?.then({
                                          uiThreadPoster.post {
                                              binding.localMediaContainer.addLocalMediaView(localMedia?.view)
                                              viewModel.joinLiveClass()
                                          }
                                      }, { exception -> })
        } else {
            binding.localMediaContainer.addLocalMediaView(localMedia?.view)
        }
    }

    private fun openSfuUpstreamConnection() {
        val upstreamConnection = viewModel.openSfuUpstreamConnection(
            getAudioStream(localMedia),
            getVideoStream(localMedia),
            requireArguments().getBoolean(IS_MICROPHONE_TURNED_ON, true),
            requireArguments().getBoolean(IS_CAMERA_TURNED_ON, true)
        )

        upstreamConnection?.addOnStateChange { connection ->
            when (connection.state) {
                ConnectionState.Initializing -> {
                    onConnectionInitializing()
                }
                ConnectionState.Connected -> {
                    onConnectedSuccessfully()
                }
                ConnectionState.Failed -> {
                    // Reconnect if the connection failed.
                    openSfuUpstreamConnection()
                }
            }
        }

        upstreamConnection?.open()
        liveClassManager.setState(LiveClassState.JOINED)
    }

    private fun onConnectionInitializing() {
        binding.raiseHandBtn.isActivated = false
    }

    private fun onConnectedSuccessfully() {
        binding.raiseHandBtn.isActivated = true
    }

    override fun onRaiseHand(clientId: String) {
        uiThreadPoster.post {
            studentsFeedAdapter.onHandRaised(clientId)
        }
    }

    override fun onLowerHand(clientId: String) {
        uiThreadPoster.post {
            studentsFeedAdapter.onHandLowered(clientId)
        }
    }
}

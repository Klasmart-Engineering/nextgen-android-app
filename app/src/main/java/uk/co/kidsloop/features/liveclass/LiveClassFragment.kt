package uk.co.kidsloop.features.liveclass

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import fm.liveswitch.AudioStream
import fm.liveswitch.Channel
import fm.liveswitch.ConnectionInfo
import fm.liveswitch.ConnectionState
import fm.liveswitch.ManagedConnection
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.LiveClassFragmentBinding
import fm.liveswitch.SfuDownstreamConnection
import fm.liveswitch.VideoStream
import uk.co.kidsloop.app.UiThreadPoster
import uk.co.kidsloop.app.utils.shortToast
import uk.co.kidsloop.data.enums.DataChannelActions
import uk.co.kidsloop.features.liveclass.localmedia.CameraLocalMedia
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia
import uk.co.kidsloop.features.liveclass.localmedia.LocalMedia
import uk.co.kidsloop.liveswitch.DataChannelActionsHandler
import uk.co.kidsloop.features.liveclass.state.LiveClassState
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

        binding.raiseHandBtn.isActivated = false

        viewModel.classroomStateLiveData.observe(viewLifecycleOwner, Observer
        {
            when (it) {
                is LiveClassViewModel.LiveClassUiState.Loading -> showLoading()
                is LiveClassViewModel.LiveClassUiState.RegistrationSuccessful -> {
                    uiThreadPoster.post { onClientRegistered(it.channel) }
                }
                is LiveClassViewModel.LiveClassUiState.FailedToJoiningLiveClass -> handleFailures()
                is LiveClassViewModel.LiveClassUiState.LocalMediaTurnedOn -> turnOnLocalMedia()
                is LiveClassViewModel.LiveClassUiState.LocalMediaTurnedOff -> turnOffLocalMedia()
                is LiveClassViewModel.LiveClassUiState.UnregisterSuccessful -> stopLocalMedia()
                is LiveClassViewModel.LiveClassUiState.UnregisterFailed -> stopLocalMedia()
            }
        })

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
                when (binding.raiseHandBtn.isSelected) {
                    true -> liveClassManager.sendDataString(DataChannelActions.RAISE_HAND.type)
                    false -> liveClassManager.sendDataString(DataChannelActions.LOWER_HAND.type)
                }
            }
        }

        liveClassManager.dataChannelActionsHandler = this
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
        // Adding remote view to UI.
        if (remoteConnectionInfo.clientRoles[0] == TEACHER_ROLE) {
            requireActivity().runOnUiThread {
                binding.teacherVideoFeed.tag = remoteMedia.id
                binding.teacherVideoFeed.addView(remoteMedia.view)
            }
        } else {
            val numberOfDownstreamConnection =
                liveClassManager.getNumberOfActiveDownStreamConnections()
            requireActivity().runOnUiThread {
                if (numberOfDownstreamConnection == 0) {
                    binding.firstStudentVideoFeed.tag = remoteMedia.id
                    binding.firstStudentVideoFeed.visibility = View.VISIBLE
                    binding.firstStudentVideoFeed.addView(remoteMedia.view)
                } else if (numberOfDownstreamConnection == 1) {
                    binding.secondStudentVideoFeed.tag = remoteMedia.id
                    binding.secondStudentVideoFeed.visibility = View.VISIBLE
                    binding.secondStudentVideoFeed.addView(remoteMedia.view)
                } else if (numberOfDownstreamConnection == 2) {
                    binding.thirdStudentVideoFeed.tag = remoteMedia.id
                    binding.thirdStudentVideoFeed.visibility = View.VISIBLE
                    binding.thirdStudentVideoFeed.addView(remoteMedia.view)
                }
            }
        }

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
                liveClassManager.getDownstreamDataStream()
            )

        // Store the downstream connection.
        liveClassManager.saveDownStreamConnections(remoteMedia.id, connection)
        connection.addOnStateChange { conn: ManagedConnection ->
            if (conn.state == ConnectionState.Closing || conn.state == ConnectionState.Failing) {
                requireActivity().runOnUiThread {
                    if (view != null) {
                        val remoteId = remoteMedia.id
                        if (binding.firstStudentVideoFeed.tag == remoteId) {
                            binding.firstStudentVideoFeed.removeView(remoteMedia.view)
                            binding.firstStudentVideoFeed.visibility = View.GONE
                        } else if (binding.secondStudentVideoFeed.tag == remoteId) {
                            binding.secondStudentVideoFeed.removeView(remoteMedia.view)
                            binding.secondStudentVideoFeed.visibility = View.GONE
                        } else if (binding.thirdStudentVideoFeed.tag == remoteId) {
                            binding.thirdStudentVideoFeed.removeView(remoteMedia.view)
                            binding.thirdStudentVideoFeed.visibility = View.GONE
                        } else if (binding.teacherVideoFeed.tag == remoteId) {
                            binding.teacherVideoFeed.removeView(remoteMedia.view)
                        }
                    }
                }
                remoteMedia.destroy()
                liveClassManager.removeDownStreamConnection(remoteMedia.id)
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

    private fun turnOnLocalMedia() {
        //here we should add functionality in order to turn on camera for yourself
    }

    private fun turnOffLocalMedia() {
        //here we should add functionality in order to turn off local media
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
                ConnectionState.Initializing -> { onConnectionInitializing() }
                ConnectionState.Connected -> { onConnectedSuccessfully() }
                ConnectionState.Failed -> {
                    // Reconnect if the connection failed.
                    openSfuUpstreamConnection()
                }
                else -> {}
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

    override fun onRaiseHand() {
        uiThreadPoster.post {
            shortToast("Hand raised")
        }
    }

    override fun onLowerHand() {
        uiThreadPoster.post {
            shortToast("Hand lowered")
        }
    }
}

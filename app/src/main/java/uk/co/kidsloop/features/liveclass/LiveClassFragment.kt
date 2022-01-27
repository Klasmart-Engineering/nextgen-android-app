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
import fm.liveswitch.IAction1
import fm.liveswitch.ManagedConnection
import uk.co.kidsloop.R
import uk.co.kidsloop.app.BaseFragment
import uk.co.kidsloop.databinding.LiveClassFragmentBinding
import fm.liveswitch.SfuDownstreamConnection
import fm.liveswitch.VideoStream
import uk.co.kidsloop.app.UiThreadPoster
import uk.co.kidsloop.features.liveclass.localmedia.CameraLocalMedia
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia
import uk.co.kidsloop.features.liveclass.localmedia.LocalMedia
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import uk.co.kidsloop.liveswitch.Config.TEACHER_ROLE
import javax.inject.Inject

@AndroidEntryPoint
class LiveClassFragment : BaseFragment(R.layout.live_class_fragment) {

    companion object {
        const val IS_CAMERA_TURNED_ON = "isCameraTurnedOn"
        const val IS_MICROPHONE_TURNED_ON = "isMicrophoneTurnedOn"
    }

    @Inject
    lateinit var liveClassManager: LiveClassManager

    @Inject
    lateinit var uiThreadPoster: UiThreadPoster

    private val binding by viewBinding(LiveClassFragmentBinding::bind)
    private var localMedia: LocalMedia<View>? = null

    private var isCameraTurnedOn: Boolean = true
    private var isMicrophoneTurnedOn: Boolean = true

    private val viewModel: LiveClassViewModel by viewModels<LiveClassViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = requireArguments()
        isCameraTurnedOn = arguments.getBoolean(IS_CAMERA_TURNED_ON)
        isMicrophoneTurnedOn = arguments.getBoolean(IS_MICROPHONE_TURNED_ON)
        localMedia = CameraLocalMedia(requireContext(), false, false, AecContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toggleCameraBtn.isChecked = isCameraTurnedOn.not()
        binding.toggleMicrophoneBtn.isChecked = isMicrophoneTurnedOn.not()
        startLocalMedia()

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
            viewModel.toggleLocalAudio()
        }
        binding.toggleCameraBtn.setOnClickListener {
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
            binding.teacherVideoFeed.tag = remoteMedia.id
            binding.teacherVideoFeed.addView(remoteMedia.view)
        } else {
            val numberOfDownstreamConnection = liveClassManager.getNumberOfActiveDownStreamConnections()
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

        // Create audio and video streams from remote media.
        val audioStream: AudioStream? =
            if (remoteConnectionInfo.hasAudio) AudioStream(remoteMedia) else null
        val videoStream: VideoStream? =
            if (remoteConnectionInfo.hasVideo) VideoStream(remoteMedia) else null

        // Create a SFU downstream connection with remote audio and video and data streams.
        val connection: SfuDownstreamConnection =
            channel.createSfuDownstreamConnection(remoteConnectionInfo, audioStream, videoStream)

        // Store the downstream connection.
        liveClassManager.saveDownStreamConnections(remoteMedia.id, connection)
        connection.addOnStateChange { conn: ManagedConnection ->
            if (conn.state == ConnectionState.Closing || conn.state == ConnectionState.Failing) {
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

    private fun hideLoading() {
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
        localMedia?.stop()?.then(IAction1 { result ->

            localMedia?.destroy()
            localMedia = null
            //TODO This is added for testing purpouse and it will be removed later on
            requireActivity().finish()
        })
    }

    private fun startLocalMedia() {
        if (liveClassManager.getState() == LiveClassState.IDLE) {
            localMedia?.start()?.then({
                                          requireActivity().runOnUiThread {
                                           //   binding.localVideoFeed.addView(localMedia?.view)
                                              viewModel.joinLiveClass()
                                          }
                                      }, { exception -> })
        } else {
           // binding.localVideoFeed.addView(localMedia?.view)
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
            isMicrophoneTurnedOn,
            isCameraTurnedOn
        )
        upstreamConnection?.open()
        liveClassManager.setState(LiveClassState.JOINED)
    }
}

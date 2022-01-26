package uk.co.kidsloop.features.liveclass

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import uk.co.kidsloop.databinding.LiveClassFragmentBinding
import fm.liveswitch.SfuDownstreamConnection
import fm.liveswitch.VideoStream
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.features.liveclass.localmedia.CameraLocalMedia
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia
import uk.co.kidsloop.features.liveclass.localmedia.LocalMedia
import javax.inject.Inject

@AndroidEntryPoint
class LiveClassFragment : BaseFragment(R.layout.live_class_fragment) {

    companion object {

        const val IS_CAMERA_TURNED_ON = "isCameraTurnedOn"
        const val IS_MICROPHONE_TURNED_ON = "isMicrophoneTurnedOn"
    }

    @Inject
    lateinit var appContext: Context

    @Inject
    lateinit var liveClassManager: LiveClassManager

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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toggleCameraBtn.isChecked = isCameraTurnedOn.not()
        binding.toggleMicrophoneBtn.isChecked = isMicrophoneTurnedOn.not()

        localMedia = CameraLocalMedia(appContext, false, false, AecContext())
        startLocalMedia()

        viewModel.classroomStateLiveData.observe(viewLifecycleOwner, Observer
        {
            when (it) {
                is LiveClassViewModel.LiveClassState.Loading -> showLoading()
                is LiveClassViewModel.LiveClassState.RegistrationSuccessful -> onClientRegistered(
                    it.channel
                )
                is LiveClassViewModel.LiveClassState.FailedToJoiningLiveClass -> handleFailures()
                is LiveClassViewModel.LiveClassState.LocalMediaTurnedOn -> turnOnLocalMedia()
                is LiveClassViewModel.LiveClassState.LocalMediaTurnedOff -> turnOffLocalMedia()
                is LiveClassViewModel.LiveClassState.UnregisterSuccessful -> stopLocalMedia()
                is LiveClassViewModel.LiveClassState.UnregisterFailed -> stopLocalMedia()
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
        Log.d("LiveClassFragment", "openSfuDownstreamConnection")

        val remoteMedia = SFURemoteMedia(
            requireContext(),
            disableAudio = false,
            disableVideo = false,
            aecContext = AecContext()
        )
        // Adding remote view to UI.
        if (remoteConnectionInfo.clientRoles[0] == "teacher") {
            requireActivity().runOnUiThread {
                binding.teacherVideoFeed.addView(remoteMedia.view)
            }
        } else {
            val numberOfDownstreamConnection = liveClassManager.getNumberOfActiveDownStreamConnections()
            requireActivity().runOnUiThread {
                if (numberOfDownstreamConnection == 0) {
                    binding.firstStudentVideoFeed.addView(remoteMedia.view)
                } else if (numberOfDownstreamConnection == 1) {
                    binding.secondStudentVideoFeed.addView(remoteMedia.view)
                } else if (numberOfDownstreamConnection == 2) {
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
            channel.createSfuDownstreamConnection(remoteConnectionInfo, audioStream, videoStream)

        // Store the downstream connection.
        liveClassManager.saveDownStreamConnections(remoteMedia.id, connection)
        connection.addOnStateChange { conn: ManagedConnection ->
            if (conn.state == ConnectionState.Closing || conn.state == ConnectionState.Failing) {

                // Removing remote view from UI.
                //layoutManager?.removeRemoteView(remoteMedia.id)
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
        localMedia?.start()?.then({
                                      requireActivity().runOnUiThread {
                                          binding.localVideoFeed.addView(localMedia?.view)
                                          viewModel.joinLiveClass()
                                      }
                                  }, { exception -> })
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
    }
}

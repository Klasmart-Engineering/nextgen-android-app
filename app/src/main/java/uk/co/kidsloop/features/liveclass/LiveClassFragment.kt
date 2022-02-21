package uk.co.kidsloop.features.liveclass

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.* // ktlint-disable no-wildcard-imports
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import fm.liveswitch.* // ktlint-disable no-wildcard-imports
import uk.co.kidsloop.R
import uk.co.kidsloop.app.UiThreadPoster
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.app.utils.* // ktlint-disable no-wildcard-imports
import uk.co.kidsloop.databinding.LiveClassFragmentBinding
import uk.co.kidsloop.features.liveclass.feeds.FeedsAdapter
import uk.co.kidsloop.features.liveclass.localmedia.CameraLocalMedia
import uk.co.kidsloop.features.liveclass.localmedia.LocalMedia
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import uk.co.kidsloop.liveswitch.Config.STUDENT_ROLE
import uk.co.kidsloop.liveswitch.Config.TEACHER_ROLE
import uk.co.kidsloop.liveswitch.DataChannelActionsHandler
import javax.inject.Inject

@AndroidEntryPoint
class LiveClassFragment :
    BaseFragment(R.layout.live_class_fragment),
    DataChannelActionsHandler,
    DisplayManager.DisplayListener {

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
    private lateinit var window: Window
    private var localMedia: LocalMedia<View>? = null
    private lateinit var displayManager: DisplayManager
    private lateinit var display: Display
    private var initialDisplayOrientation: Int = 1
    private lateinit var toastView: View

    private val viewModel by viewModels<LiveClassViewModel>()

    private lateinit var studentsFeedAdapter: FeedsAdapter
    private var notificationToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isTeacher = when (viewModel.sharedPrefsWrapper.getRole()) {
            TEACHER_ROLE -> true
            STUDENT_ROLE -> false
            else -> false
        }

        localMedia = CameraLocalMedia(
            requireContext(),
            disableAudio = false,
            disableVideo = false,
            aecContext = AecContext(),
            enableSimulcast = true,
            isTeacher
        )

        displayManager =
            requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(this, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display = activity?.display!!
        } else {
            @Suppress("DEPRECATION")
            display = activity?.windowManager?.defaultDisplay!!
        }
        initialDisplayOrientation = displayManager.getDisplay(display.displayId).rotation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toastView = layoutInflater.inflate(R.layout.custom_toast_layout, null)
        window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.toggleCameraBtn.isActivated = true
        binding.toggleCameraBtn.isChecked =
            !requireArguments().getBoolean(IS_CAMERA_TURNED_ON, true)
        binding.toggleMicrophoneBtn.isActivated = true
        binding.toggleMicrophoneBtn.isChecked =
            !requireArguments().getBoolean(IS_MICROPHONE_TURNED_ON, true)
        if (!requireArguments().getBoolean(IS_CAMERA_TURNED_ON)) {
            binding.localMediaFeed.showCameraTurnedOff()
        }
        if (!requireArguments().getBoolean(IS_MICROPHONE_TURNED_ON)) {
            binding.localMediaFeed.showMicMuted()
        }
        studentsFeedAdapter = FeedsAdapter()
        startLocalMedia()

        binding.studentFeedsRecyclerview.apply {
            adapter = studentsFeedAdapter
            layoutManager = object : LinearLayoutManager(context) {

                override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean {
                    val height = height / 3
                    val width = height * 4 / 3
                    lp?.height = height - resources.getDimensionPixelSize(R.dimen.space_8)
                    lp?.width = width
                    return true
                }

                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
        }

        when (viewModel.sharedPrefsWrapper.getRole()) {
            TEACHER_ROLE -> setUiForTeacher()
            STUDENT_ROLE -> setUiForStudent()
        }

        setControls()

        viewModel.classroomStateLiveData.observe(
            viewLifecycleOwner,
            Observer
            {
                when (it) {
                    is LiveClassViewModel.LiveClassUiState.Loading -> showLoading()
                    is LiveClassViewModel.LiveClassUiState.RegistrationSuccessful -> onClientRegistered(
                        it.channel
                    )
                    is LiveClassViewModel.LiveClassUiState.FailedToJoiningLiveClass -> handleFailures()
                    is LiveClassViewModel.LiveClassUiState.UnregisterSuccessful -> stopLocalMedia()
                    is LiveClassViewModel.LiveClassUiState.UnregisterFailed -> stopLocalMedia()
                }
            }
        )

        liveClassManager.dataChannelActionsHandler = this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        displayManager.unregisterDisplayListener(this)
    }

    private fun setUiForTeacher() {
        binding.raiseHandBtn.gone()
        binding.toggleStudentsVideo.visible()
        binding.toggleStudentsAudio.visible()
    }

    private fun setUiForStudent() {
        binding.raiseHandBtn.visible()
        binding.raiseHandBtn.isActivated = false
    }

    private fun setControls() {
        binding.toggleMicrophoneBtn.setOnClickListener {
            if (binding.toggleMicrophoneBtn.isActivated) {
                if (binding.toggleMicrophoneBtn.isChecked) {
                    binding.localMediaFeed.showMicMuted()
                } else {
                    binding.localMediaFeed.showMicTurnedOn()
                }
                viewModel.toggleLocalAudio()
            } else {
                showCustomToast(getString(R.string.teacher_turned_off_all_students_mic), true, false)
            }
        }

        binding.toggleCameraBtn.setOnClickListener {
            if (binding.toggleCameraBtn.isActivated) {
                if (binding.toggleCameraBtn.isChecked) {
                    binding.localMediaFeed.showCameraTurnedOff()
                } else {
                    binding.localMediaFeed.showCameraTurnedOn()
                }
                viewModel.toggleLocalVideo()
            } else {
                showCustomToast(getString(R.string.teacher_turned_off_all_students_camera), false, true)
            }
        }

        binding.confirmExitClassBtn.setOnClickListener {
            viewModel.leaveLiveClass()
            Navigation.findNavController(requireView())
                .navigate(LiveClassFragmentDirections.liveclassToLogin())
        }

        binding.backBtn.setOnClickListener {
            binding.leaveLiveClassOverlay.gone()
        }

        binding.exitClassBtn.setOnClickListener {
            binding.liveClassOverlay.gone()
            binding.leaveLiveClassOverlay.visible()
        }

        binding.exitMenu.setOnClickListener {
            binding.liveClassOverlay.gone()
        }

        binding.moreBtn.setOnClickListener {
            binding.liveClassOverlay.visible()
        }

        binding.toggleStudentsVideo.setOnClickListener { view ->
            if (binding.toggleStudentsVideo.isChecked) {
                viewModel.turnOffVideoForStudents()
            } else {
                viewModel.enableVideoForStudents()
            }
        }

        binding.raiseHandBtn.setOnClickListener {
            binding.raiseHandBtn.isSelected = binding.raiseHandBtn.isSelected.not()

            if (binding.raiseHandBtn.isSelected) {
                viewModel.showHandRaised()
                binding.localMediaFeed.showHandRaised()
            } else {
                viewModel.showHandLowered()
                binding.localMediaFeed.hideRaiseHand()
            }
        }

        binding.toggleStudentsAudio.setOnClickListener {
            if (binding.toggleStudentsAudio.isChecked) {
                viewModel.disableMicForStudents()
            } else {
                viewModel.enableMicForStudents()
            }
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

        (remoteConnectionInfo.clientId ?: emptyString()).let { clientId ->
            liveClassManager.saveDownStreamConnection(clientId, connection)
            when (remoteConnectionInfo.clientRoles[0]) {
                STUDENT_ROLE -> liveClassManager.saveDownStreamConnectionRole(
                    clientId,
                    STUDENT_ROLE
                )
                TEACHER_ROLE -> liveClassManager.saveDownStreamConnectionRole(
                    clientId,
                    TEACHER_ROLE
                )
            }
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
                liveClassManager.removeDownStreamConnectionRole(clientId)
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
            uiThreadPoster.post {
                requireActivity().finish()
            }
        }
    }

    private fun startLocalMedia() {
        localMedia?.start()?.then({
            uiThreadPoster.post {
                binding.localMediaFeed.addLocalMediaView(localMedia?.view)
                viewModel.joinLiveClass()
            }
        }, { exception -> })
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
    }

    private fun onConnectionInitializing() {
        binding.raiseHandBtn.isActivated = false
    }

    private fun onConnectedSuccessfully() {
        binding.raiseHandBtn.isActivated = true
    }

    override fun onRaiseHand(clientId: String?) {
        uiThreadPoster.post {
            studentsFeedAdapter.onHandRaised(clientId)
        }
    }

    override fun onLowerHand(clientId: String?) {
        uiThreadPoster.post {
            studentsFeedAdapter.onHandLowered(clientId)
        }
    }

    override fun onVideoEnabled() {
        binding.toggleCameraBtn.isActivated = true
        binding.toggleCameraBtn.isChecked = true
    }

    override fun onVideoDisabled(state: LiveClassState) {
        viewModel.turnOffVideo()
        uiThreadPoster.post {
            binding.localMediaFeed.showCameraTurnedOff()
            binding.toggleCameraBtn.isActivated = false

            if (state == LiveClassState.CAM_DISABLED_BY_TEACHER) {
                showCustomToast(getString(R.string.teacher_turned_off_all_students_camera), false, true)
            } else {
                notificationToast?.cancel()
                showCustomToast(getString(R.string.teacher_turned_off_all_students_cam_and_mic), true, true)
            }
        }
    }

    private fun showCustomToast(message: String, isMicDisabled: Boolean, isCamDisabled: Boolean) {
        notificationToast?.cancel()
        notificationToast = Toast(requireActivity())
        if (binding.liveClassOverlay.isVisible) {
            notificationToast?.setGravity(Gravity.TOP or Gravity.FILL, 0, 0)
            toastView.findViewById<TextView>(R.id.start_space).visibility = View.VISIBLE
            toastView.findViewById<TextView>(R.id.end_space).visibility = View.VISIBLE
        } else {
            notificationToast?.setGravity(Gravity.START or Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, 40)
            toastView.findViewById<TextView>(R.id.start_space).visibility = View.GONE
            toastView.findViewById<TextView>(R.id.end_space).visibility = View.GONE
        }
        toastView.findViewById<TextView>(R.id.status_textview).text = message
        toastView.findViewById<ImageView>(R.id.mic_muted_imageView).isVisible = isMicDisabled
        toastView.findViewById<ImageView>(R.id.cam_muted_imageView).isVisible = isCamDisabled
        notificationToast?.view = toastView
        notificationToast?.duration = Toast.LENGTH_LONG
        notificationToast?.show()
    }

    override fun onDisplayAdded(displayId: Int) {}

    override fun onDisplayRemoved(displayId: Int) {}

    override fun onDisplayChanged(displayId: Int) {
        if (initialDisplayOrientation == Surface.ROTATION_90) {
            if (display.rotation == Surface.ROTATION_90) {
                binding.localMediaFeed.updateLocalMediaViewOrientationDefault()
            }
            if (display.rotation == Surface.ROTATION_270) {
                binding.localMediaFeed.updateLocalMediaViewOrientationReverse()
            }
        } else {
            if (display.rotation == Surface.ROTATION_90) {
                binding.localMediaFeed.updateLocalMediaViewOrientationReverse()
            }
            if (display.rotation == Surface.ROTATION_270) {
                binding.localMediaFeed.updateLocalMediaViewOrientationDefault()
            }
        }
    }

    override fun onEnableMic() {
        uiThreadPoster.post {
            binding.localMediaFeed.showMicMuted()
            binding.toggleMicrophoneBtn.isActivated = true
            binding.toggleMicrophoneBtn.isChecked = true
        }
    }

    override fun onDisableMic(state: LiveClassState) {
        viewModel.turnOffAudio()
        uiThreadPoster.post {
            binding.localMediaFeed.showMicDisabledMuted()
            binding.toggleMicrophoneBtn.isActivated = false
            if (state == LiveClassState.MIC_DISABLED_BY_TEACHER) {
                showCustomToast(getString(R.string.teacher_turned_off_all_students_mic), true, false)
            } else {
                notificationToast?.cancel()
                showCustomToast(getString(R.string.teacher_turned_off_all_students_cam_and_mic), true, true)
            }
        }
    }
}

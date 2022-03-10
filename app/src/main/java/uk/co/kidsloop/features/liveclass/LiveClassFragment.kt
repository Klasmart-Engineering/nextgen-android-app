package uk.co.kidsloop.features.liveclass

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.* // ktlint-disable no-wildcard-imports
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
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
import uk.co.kidsloop.liveswitch.Config
import uk.co.kidsloop.liveswitch.Config.ASSISTANT_TEACHER_ROLE
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

    @Inject
    lateinit var dialogsManager: DialogsManager

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
    private var isMainTeacher: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isMainTeacher = when (viewModel.sharedPrefsWrapper.getRole()) {
            TEACHER_ROLE -> true
            else -> false
        }

        localMedia = CameraLocalMedia(
            requireContext(),
            disableAudio = false,
            disableVideo = false,
            aecContext = AecContext(),
            enableSimulcast = true,
            isMainTeacher
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

        studentsFeedAdapter = FeedsAdapter()
        startLocalMedia()

        binding.studentFeedsRecyclerview.apply {
            adapter = studentsFeedAdapter
            layoutManager = object : LinearLayoutManager(context) {

                override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean {
                    val newHeight = height / 3
                    val newWidth = newHeight * 4 / 3
                    lp?.height = newHeight - resources.getDimensionPixelSize(R.dimen.space_8)
                    lp?.width = newWidth - resources.getDimensionPixelSize(R.dimen.space_4)
                    return true
                }

                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
        }

        if (isMainTeacher) {
            setUiForTeacher()
        } else {
            showLoading()
            setupWaitingState()
        }

        setControls()

        viewModel.classroomStateLiveData.observe(
            viewLifecycleOwner,
            Observer
            {
                when (it) {
                    is LiveClassViewModel.LiveClassUiState.RegistrationSuccessful -> onClientRegistered(
                        it.channel
                    )
                    is LiveClassViewModel.LiveClassUiState.FailedToJoiningLiveClass -> hideLoading()
                    is LiveClassViewModel.LiveClassUiState.UnregisterSuccessful -> stopLocalMedia()
                    is LiveClassViewModel.LiveClassUiState.UnregisterFailed -> stopLocalMedia()
                    is LiveClassViewModel.LiveClassUiState.LiveClassStarted -> onLiveClassStarted()
                    is LiveClassViewModel.LiveClassUiState.LiveClassRestarted -> onLiveClassRestarted()
                    is LiveClassViewModel.LiveClassUiState.LiveClassEnded -> onLiveClassEnded()
                }
            }
        )

        liveClassManager.dataChannelActionsHandler = this

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    viewModel.leaveLiveClass()
                }
            }
        )
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
        binding.teacherVideoFeedOverlay.visible()
        binding.liveClassGroup.visible()
        binding.raiseHandBtn.gone()
        binding.waitingStateTextview.visibility = View.GONE
        binding.blackboardImageView.visibility = View.GONE

        binding.toggleStudentsVideo.visible()
        binding.toggleStudentsAudio.visible()

        binding.toggleCameraBtn.isActivated = true
        binding.toggleMicrophoneBtn.isActivated = true
    }

    private fun setupWaitingState() {
        localMedia?.videoMuted = true
        localMedia?.audioMuted = true
        binding.raiseHandBtn.isEnabled = false

        binding.toggleCameraBtn.isActivated = false
        binding.toggleCameraBtn.isChecked = false

        binding.toggleMicrophoneBtn.isActivated = false
        binding.toggleMicrophoneBtn.isChecked = false

        binding.localMediaFeed.showCameraTurnedOff()
        binding.localMediaFeed.showMicDisabledMuted()
        binding.waitingStateTextview.visible()
        binding.blackboardImageView.visible()
    }

    private fun setupLeavingState() {
        localMedia?.videoMuted = true
        localMedia?.audioMuted = true
        binding.raiseHandBtn.isEnabled = false

        binding.toggleCameraBtn.isActivated = false
        binding.toggleCameraBtn.isChecked = false

        binding.toggleMicrophoneBtn.isActivated = false
        binding.toggleMicrophoneBtn.isChecked = false

        binding.localMediaFeed.showCameraTurnedOff()
        binding.localMediaFeed.showMicDisabledMuted()
        binding.teacherVideoFeedBackground.visible()
        binding.leavingStateTextview.visible()
        binding.showingByeImageView.visible()

        Handler(Looper.getMainLooper()).postDelayed({
            viewModel.leaveLiveClass()
        }, 5000)
    }

    private fun setControls() {
        binding.toggleMicrophoneBtn.setOnClickListener {
            if (binding.toggleMicrophoneBtn.isActivated) {
                if (binding.toggleMicrophoneBtn.isChecked) {
                    binding.localMediaFeed.showMicMuted()
                    localMedia?.audioMuted = true
                } else {
                    binding.localMediaFeed.showMicTurnedOn()
                    localMedia?.audioMuted = false
                }
            } else {
                val messageId = when (liveClassManager.getState()) {
                    LiveClassState.JOINED_AND_WAITING_FOR_TEACHER -> R.string.wait_for_teacher_to_arrive
                    LiveClassState.TEACHER_DISCONNECTED -> R.string.teacher_has_left_the_classroom
                    else -> R.string.teacher_turned_off_all_microphones
                }
                showCustomToast(getString(messageId), true, false)
            }
        }

        binding.toggleCameraBtn.setOnClickListener {
            if (binding.toggleCameraBtn.isActivated) {
                if (binding.toggleCameraBtn.isChecked) {
                    binding.localMediaFeed.showCameraTurnedOff()
                    localMedia?.videoMuted = true
                } else {
                    binding.localMediaFeed.showCameraTurnedOn()
                    localMedia?.videoMuted = false
                }
            } else {
                val messageId = when (liveClassManager.getState()) {
                    LiveClassState.JOINED_AND_WAITING_FOR_TEACHER -> R.string.wait_for_teacher_to_arrive
                    LiveClassState.TEACHER_DISCONNECTED -> R.string.teacher_has_left_the_classroom
                    else -> R.string.teacher_turned_off_all_cameras
                }
                showCustomToast(getString(messageId), false, true)
            }
        }

        binding.exitClassBtn.setOnClickListener {
            dialogsManager.showDialog(LeaveClassDialog.TAG)
            requireActivity().supportFragmentManager.setFragmentResultListener(
                LeaveClassDialog.TAG.toString(),
                viewLifecycleOwner
            ) { _, _ ->
                if (isMainTeacher) {
                    viewModel.endLiveClass()
                } else {
                    viewModel.leaveLiveClass()
                }
            }
        }

        binding.exitMenu.setOnClickListener {
            binding.liveClassOverlay.isVisible = false
        }

        binding.moreBtn.setOnClickListener {
            binding.liveClassOverlay.isVisible = true
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

    private fun openSfuDownstreamConnection(remoteConnectionInfo: ConnectionInfo) {
        // Create remote media.
        val remoteMedia = SFURemoteMedia(
            requireContext(),
            disableAudio = false,
            disableVideo = false,
            aecContext = AecContext()
        )

        val connection =
            viewModel.openSfuDownstreamConnection(remoteConnectionInfo, remoteMedia)

        // Adding remote view to UI.
        when (remoteConnectionInfo.clientRoles[0]) {
            TEACHER_ROLE -> {
                uiThreadPoster.post {
                    binding.raiseHandBtn.enable()
                    binding.teacherVideoFeed.tag = remoteConnectionInfo.clientId
                    binding.teacherVideoFeed.addView(remoteMedia.view, 1)
                }
            }
            STUDENT_ROLE -> uiThreadPoster.post {
                studentsFeedAdapter.addVideoFeed(remoteConnectionInfo.clientId, remoteMedia.view, STUDENT_ROLE)
            }
            ASSISTANT_TEACHER_ROLE -> uiThreadPoster.post {
                studentsFeedAdapter.addAssistantTeacherVideoFeed(remoteConnectionInfo.clientId, remoteMedia.view)
            }
        }

        connection?.addOnStateChange { conn: ManagedConnection ->
            if (conn.state == ConnectionState.Closing || conn.state == ConnectionState.Failing) {
                val isTeacherDisconnected = remoteConnectionInfo.clientRoles[0] == TEACHER_ROLE
                if (isTeacherDisconnected && liveClassManager.getState() != LiveClassState.TEACHER_ENDED_LIVE_CLASS) {
                    liveClassManager.setState(LiveClassState.TEACHER_DISCONNECTED)
                    localMedia?.videoMuted = true
                    localMedia?.audioMuted = true
                }
                val clientId = remoteConnectionInfo.clientId
                uiThreadPoster.post {
                    if (view != null) {
                        if (binding.teacherVideoFeed.tag == clientId) {
                            binding.teacherVideoFeed.removeViewAt(1)
                            if (liveClassManager.getState() != LiveClassState.TEACHER_ENDED_LIVE_CLASS)
                                setupWaitingState()
                            else
                                setupLeavingState()
                        } else {
                            studentsFeedAdapter.removeVideoFeed(clientId)
                        }
                    }
                }
                remoteMedia.destroy()
                liveClassManager.removeDownStreamConnection(clientId)
            } else if (conn.state == ConnectionState.Failed) {
                // Reconnect if the connection failed.
                openSfuDownstreamConnection(remoteConnectionInfo)
            }
        }
    }

    private fun getAudioStream(localMedia: LocalMedia<View>?): AudioStream? {
        return if (localMedia?.audioTrack != null) AudioStream(localMedia.audioTrack) else null
    }

    private fun getVideoStream(localMedia: LocalMedia<View>?): VideoStream? {
        return if (localMedia?.videoTrack != null) VideoStream(localMedia.videoTrack) else null
    }

    private fun showLoading() {
        binding.liveClassOverlay.gone()
        binding.liveClassGroup.gone()
        binding.loadingIndication.visible()
    }

    private fun hideLoading() {
        if (!isMainTeacher) {
            binding.loadingIndication.gone()
            binding.liveClassGroup.visible()
        }
    }

    private fun onClientRegistered(channel: Channel) {
        openSfuUpstreamConnection()
        // Check for existing remote upstream connections and open a downstream connection for
        // each of them.
        for (connectionInfo in channel.remoteUpstreamConnectionInfos) {
            openSfuDownstreamConnection(connectionInfo)
        }

        channel.addOnRemoteUpstreamConnectionOpen { connectionInfo ->
            openSfuDownstreamConnection(connectionInfo)
        }
    }

    private fun leaveLiveClass() {
        uiThreadPoster.post {
            findNavController().popBackStack()
        }
    }

    private fun stopLocalMedia() {
        localMedia?.stop()?.then { _ ->
            localMedia?.destroy()
            localMedia = null
            leaveLiveClass()
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
            getVideoStream(localMedia)
        )

        upstreamConnection?.addOnStateChange { connection ->
            if (connection.state == ConnectionState.Failed) {
                // Reconnect if the connection failed.
                openSfuUpstreamConnection()
            } else if (connection.state == ConnectionState.Connecting) {
                uiThreadPoster.post {
                    hideLoading()
                }
            }
        }
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
                showCustomToast(getString(R.string.teacher_turned_off_all_cameras), false, true)
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
            notificationToast?.setGravity(Gravity.TOP or Gravity.FILL_HORIZONTAL, 0, 0)
        } else {
            notificationToast?.setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, 0)
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
                showCustomToast(getString(R.string.teacher_turned_off_all_microphones), true, false)
            } else {
                notificationToast?.cancel()
                showCustomToast(getString(R.string.teacher_turned_off_all_students_cam_and_mic), true, true)
            }
        }
    }

    override fun onLiveClassEnding() {
        uiThreadPoster.post {
            setupLeavingState()
        }
    }

    private fun onLiveClassStarted() {
        uiThreadPoster.post {
            binding.teacherVideoFeedOverlay.gone()
            binding.blackboardImageView.gone()
            binding.waitingStateTextview.gone()
            binding.toggleCameraBtn.isActivated = true

            if (requireArguments().getBoolean(IS_CAMERA_TURNED_ON)) {
                localMedia?.videoMuted = false
                binding.toggleCameraBtn.isChecked = false
                binding.localMediaFeed.showCameraTurnedOn()
            } else {
                binding.toggleCameraBtn.isChecked = true
            }

            binding.toggleMicrophoneBtn.isActivated = true
            if (requireArguments().getBoolean(IS_MICROPHONE_TURNED_ON)) {
                localMedia?.audioMuted = false
                binding.toggleMicrophoneBtn.isChecked = false
                binding.localMediaFeed.showMicTurnedOn()
            } else {
                binding.localMediaFeed.showMicMuted()
                binding.toggleMicrophoneBtn.isChecked = true
            }
        }
    }

    private fun onLiveClassRestarted() {
        uiThreadPoster.post {
            binding.teacherVideoFeedOverlay.gone()
            binding.blackboardImageView.gone()
            binding.waitingStateTextview.gone()

            binding.toggleCameraBtn.isActivated = true
            binding.toggleCameraBtn.isChecked = true

            binding.toggleMicrophoneBtn.isActivated = true
            binding.toggleMicrophoneBtn.isChecked = true
            binding.localMediaFeed.showMicMuted()
        }
    }

    private fun onLiveClassEnded() {
        viewModel.leaveLiveClass()
    }
}

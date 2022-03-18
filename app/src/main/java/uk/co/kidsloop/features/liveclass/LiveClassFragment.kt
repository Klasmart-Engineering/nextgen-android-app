package uk.co.kidsloop.features.liveclass

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.* // ktlint-disable no-wildcard-imports
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import fm.liveswitch.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject
import uk.co.kidsloop.R
import uk.co.kidsloop.app.UiThreadPoster
import uk.co.kidsloop.app.common.BaseFragment
import uk.co.kidsloop.app.common.DialogsManager
import uk.co.kidsloop.app.common.LeaveClassDialog
import uk.co.kidsloop.app.common.ToastHelper
import uk.co.kidsloop.app.utils.* // ktlint-disable no-wildcard-imports
import uk.co.kidsloop.databinding.LiveClassFragmentBinding
import uk.co.kidsloop.features.liveclass.enums.CameraStatus
import uk.co.kidsloop.features.liveclass.enums.MicStatus
import uk.co.kidsloop.features.liveclass.feeds.FeedsAdapter
import uk.co.kidsloop.features.liveclass.localmedia.CameraLocalMedia
import uk.co.kidsloop.features.liveclass.localmedia.LocalMedia
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import uk.co.kidsloop.liveswitch.Config.ASSISTANT_TEACHER_ROLE
import uk.co.kidsloop.liveswitch.Config.LOCAL_ROLE
import uk.co.kidsloop.liveswitch.Config.STUDENT_ROLE
import uk.co.kidsloop.liveswitch.Config.TEACHER_ROLE
import uk.co.kidsloop.liveswitch.DataChannelActionsHandler

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

    @Inject
    lateinit var toastHelper: ToastHelper

    private val binding by viewBinding(LiveClassFragmentBinding::bind)
    private lateinit var window: Window
    private var localMedia: LocalMedia<View>? = null
    private lateinit var displayManager: DisplayManager
    private lateinit var display: Display
    private var initialDisplayOrientation: Int = 1

    private val viewModel by viewModels<LiveClassViewModel>()

    private lateinit var studentsFeedAdapter: FeedsAdapter
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
        window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        studentsFeedAdapter = FeedsAdapter()
        startLocalMedia()

        showLoading()

        binding.studentFeedsRecyclerview.apply {
            adapter = studentsFeedAdapter
            layoutManager = object : LinearLayoutManager(context) {

                override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean {
                    val newHeight = height / 4
                    val newWidth = newHeight * 4 / 3
                    lp?.height = newHeight - resources.getDimensionPixelSize(R.dimen.space_8)
                    lp?.width = newWidth - resources.getDimensionPixelSize(R.dimen.space_4)
                    return true
                }

                override fun canScrollVertically(): Boolean {
                    return false
                }
            }

            doOnNextLayout {
                startSettingUpUi()
            }
        }

        setControls()
        observeAdapter()

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
                    showLoading()
                    if (isMainTeacher) {
                        viewModel.endLiveClass()
                    } else {
                        viewModel.leaveLiveClass()
                    }
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

    private fun startSettingUpUi() {
        when {
            isMainTeacher -> setUiForTeacher()
            liveClassManager.isTeacherMissing() -> setupWaitingState()
            else -> setUiForStudent()
        }
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

        val newCameraStatus: CameraStatus
        val newMicStatus: MicStatus

        if (requireArguments().getBoolean(IS_CAMERA_TURNED_ON)) {
            localMedia?.videoMuted = false
            binding.toggleCameraBtn.isChecked = false
            newCameraStatus = CameraStatus.ON
        } else {
            localMedia?.videoMuted = true
            binding.toggleCameraBtn.isChecked = true
            newCameraStatus = CameraStatus.OFF
        }

        if (requireArguments().getBoolean(IS_MICROPHONE_TURNED_ON)) {
            localMedia?.audioMuted = false
            binding.toggleMicrophoneBtn.isChecked = false
            newMicStatus = MicStatus.ON
        } else {
            localMedia?.audioMuted = true
            binding.toggleMicrophoneBtn.isChecked = true
            newMicStatus = MicStatus.MUTED
        }

        studentsFeedAdapter.updateMicAndCameraStatus(newMicStatus, newCameraStatus, 0)
    }

    private fun setUiForStudent() {
        binding.liveClassGroup.visible()
        binding.raiseHandBtn.visible()

        val newCameraStatus: CameraStatus
        val newMicStatus: MicStatus

        if (requireArguments().getBoolean(IS_CAMERA_TURNED_ON)) {
            localMedia?.videoMuted = false
            binding.toggleCameraBtn.isChecked = false
            newCameraStatus = CameraStatus.ON
        } else {
            localMedia?.videoMuted = true
            binding.toggleCameraBtn.isChecked = true
            newCameraStatus = CameraStatus.OFF
        }

        if (requireArguments().getBoolean(IS_MICROPHONE_TURNED_ON)) {
            localMedia?.audioMuted = false
            binding.toggleMicrophoneBtn.isChecked = false
            newMicStatus = MicStatus.ON
        } else {
            localMedia?.audioMuted = true
            binding.toggleMicrophoneBtn.isChecked = true
            newMicStatus = MicStatus.MUTED // --> also here the same problem
        }

        studentsFeedAdapter.updateMicAndCameraStatus(newMicStatus, newCameraStatus, 0)
    }

    private fun setupWaitingState() {
        localMedia?.videoMuted = true
        localMedia?.audioMuted = true
        binding.raiseHandBtn.isEnabled = false

        binding.toggleCameraBtn.isActivated = false
        binding.toggleCameraBtn.isChecked = false

        binding.toggleMicrophoneBtn.isActivated = false
        binding.toggleMicrophoneBtn.isChecked = false

        studentsFeedAdapter.updateMicAndCameraStatus(MicStatus.DISABLED, CameraStatus.OFF, 0)

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

        studentsFeedAdapter.updateMicAndCameraStatus(MicStatus.MUTED, CameraStatus.OFF, 0)
        binding.teacherVideoFeedBackground.visible()
        binding.leavingStateTextview.visible()
        binding.showingByeImageView.visible()

        Handler(Looper.getMainLooper()).postDelayed({
            viewModel.leaveLiveClass()
        }, 5000)
    }

    private fun checkNumberOfStudents(number: Int) {
        (number - FeedsAdapter.MAX_FEEDS_VISIBLE).let {
            when (it) {
                in Int.MIN_VALUE..0 -> hideOthersLabel()
                0 -> hideOthersLabel()
                else -> showOthersLabel(it)
            }
        }
    }

    private fun showOthersLabel(number: Int) {
        when (number) {
            1 -> binding.othersLabel.text = getString(R.string.other_students_number_label, number)
            else -> binding.othersLabel.text = getString(R.string.others_students_number_label, number)
        }
        binding.othersLabel.visible()
    }

    private fun hideOthersLabel() {
        binding.othersLabel.gone()
    }

    private fun setControls() {
        binding.toggleMicrophoneBtn.setOnClickListener {
            if (binding.toggleMicrophoneBtn.isActivated) {
                if (binding.toggleMicrophoneBtn.isChecked) {
                    studentsFeedAdapter.showMicMuted(0)
                    localMedia?.audioMuted = true
                } else {
                    studentsFeedAdapter.showMicTurnedOn(0)
                    localMedia?.audioMuted = false
                }
            } else {
                toastHelper.onMicControlClicked()
            }
        }

        binding.toggleCameraBtn.setOnClickListener {
            if (binding.toggleCameraBtn.isActivated) {
                if (binding.toggleCameraBtn.isChecked) {
                    studentsFeedAdapter.showCameraTurnedOff(0)
                    localMedia?.videoMuted = true
                } else {
                    studentsFeedAdapter.showCameraTurnedOn(0)
                    localMedia?.videoMuted = false
                }
            } else {
                toastHelper.onCamControlClicked()
            }
        }

        binding.exitClassBtn.setOnClickListener {
            dialogsManager.showDialog(LeaveClassDialog.TAG)
            requireActivity().supportFragmentManager.setFragmentResultListener(
                LeaveClassDialog.TAG.toString(),
                viewLifecycleOwner
            ) { _, _ ->
                showLoading()
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
                studentsFeedAdapter.showLocalMediaHandRaised()
            } else {
                viewModel.showHandLowered()
                studentsFeedAdapter.hideLocalMediaRaiseHand()
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

    private fun observeAdapter() = with(FeedsAdapter) {
        studentsFeedAdapter.itemCount.observe(
            viewLifecycleOwner,
            Observer {
                checkNumberOfStudents(it)
            }
        )
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
                studentsFeedAdapter.addVideoFeed(
                    remoteConnectionInfo.clientId,
                    remoteMedia.view,
                    ASSISTANT_TEACHER_ROLE
                )
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
        binding.loadingIndication.gone()
        binding.liveClassGroup.visible()
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
                val clientId = liveClassManager.getUpstreamClientId() ?: "a1b2c3"
                val localView = localMedia?.view!!

                studentsFeedAdapter.addVideoFeed(clientId, localView, LOCAL_ROLE)
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
            studentsFeedAdapter.showCameraTurnedOff(0)
            binding.toggleCameraBtn.isActivated = false
            toastHelper.showCustomToast(binding.liveClassOverlay.isVisible)
        }
    }

    override fun onDisplayAdded(displayId: Int) {}

    override fun onDisplayRemoved(displayId: Int) {}

    override fun onDisplayChanged(displayId: Int) {
        if (initialDisplayOrientation == Surface.ROTATION_90) {
            if (display.rotation == Surface.ROTATION_90) {
                studentsFeedAdapter.updateMediaViewOrientationDefault(0)
            }
            if (display.rotation == Surface.ROTATION_270) {
                studentsFeedAdapter.updateMediaViewOrientationReverse(0)
            }
        } else {
            if (display.rotation == Surface.ROTATION_90) {
                studentsFeedAdapter.updateMediaViewOrientationReverse(0)
            }
            if (display.rotation == Surface.ROTATION_270) {
                studentsFeedAdapter.updateMediaViewOrientationDefault(0)
            }
        }
    }

    override fun onEnableMic() {
        uiThreadPoster.post {
            studentsFeedAdapter.showMicMuted(0)
            binding.toggleMicrophoneBtn.isActivated = true
            binding.toggleMicrophoneBtn.isChecked = true
        }
    }

    override fun onDisableMic(state: LiveClassState) {
        viewModel.turnOffAudio()
        uiThreadPoster.post {
            studentsFeedAdapter.showMicDisabledMuted(0)
            binding.toggleMicrophoneBtn.isActivated = false
            toastHelper.showCustomToast(binding.liveClassOverlay.isVisible)
        }
    }

    override fun onLiveClassEnding() {
        uiThreadPoster.post {
            setupLeavingState()
        }
    }

    private fun onLiveClassStarted() {
        // TODO: find a way of doing this
        // The issue here is that onLiveClassStarted gets called before the RecyclerView has inflated it's views.
        // Because of this the changes are calculated before they can be displayed.
        binding.teacherVideoFeedOverlay.gone()
        binding.blackboardImageView.gone()
        binding.waitingStateTextview.gone()

        binding.toggleCameraBtn.isActivated = true
        binding.toggleMicrophoneBtn.isActivated = true

        val newCameraStatus: CameraStatus
        val newMicStatus: MicStatus

        if (requireArguments().getBoolean(IS_CAMERA_TURNED_ON)) {
            localMedia?.videoMuted = false
            binding.toggleCameraBtn.isChecked = false
            newCameraStatus = CameraStatus.ON
        } else {
            localMedia?.videoMuted = true
            binding.toggleCameraBtn.isChecked = true
            newCameraStatus = CameraStatus.OFF
        }

        if (requireArguments().getBoolean(IS_MICROPHONE_TURNED_ON)) {
            localMedia?.audioMuted = false
            binding.toggleMicrophoneBtn.isChecked = false
            newMicStatus = MicStatus.ON
        } else {
            localMedia?.audioMuted = true
            binding.toggleMicrophoneBtn.isChecked = true
            newMicStatus = MicStatus.MUTED // --> also here the same problem
        }

        studentsFeedAdapter.updateMicAndCameraStatus(newMicStatus, newCameraStatus, 0)
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
            studentsFeedAdapter.showMicMuted(0)
        }
    }

    private fun onLiveClassEnded() {
        viewModel.leaveLiveClass()
    }
}

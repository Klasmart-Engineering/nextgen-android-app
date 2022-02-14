package uk.co.kidsloop.features.liveclass

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.* // ktlint-disable no-wildcard-imports
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import fm.liveswitch.* // ktlint-disable no-wildcard-imports
import uk.co.kidsloop.R
import uk.co.kidsloop.app.UiThreadPoster
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.app.utils.* // ktlint-disable no-wildcard-imports
import uk.co.kidsloop.data.enums.LiveSwitchNetworkQuality
import uk.co.kidsloop.data.enums.StudentFeedQuality
import uk.co.kidsloop.data.enums.TeacherFeedQuality
import uk.co.kidsloop.databinding.LiveClassFragmentBinding
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

    private lateinit var studentsFeedAdapter: StudentFeedsAdapter
    private var notificationToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localMedia = CameraLocalMedia(
            requireContext(),
            disableAudio = false,
            disableVideo = false,
            aecContext = AecContext()
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
            binding.localMediaContainer.showCameraTurnedOff()
        }
        if (!requireArguments().getBoolean(IS_MICROPHONE_TURNED_ON)) {
            binding.localMediaContainer.showMicMuted()
        }
        startLocalMedia()

        studentsFeedAdapter = StudentFeedsAdapter()
        binding.studentFeedsRecyclerview.apply {
            adapter = studentsFeedAdapter
            layoutManager = object : LinearLayoutManager(context) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
            setHasFixedSize(true)
            itemAnimator = null
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
                    binding.localMediaContainer.showMicMuted()
                } else {
                    binding.localMediaContainer.showMicTurnedOn()
                }
                viewModel.toggleLocalAudio()
            } else {
                showCustomToast(getString(R.string.teacher_turned_off_all_students_mic), true, false)
            }
        }

        binding.toggleCameraBtn.setOnClickListener {
            if (binding.toggleCameraBtn.isActivated) {
                if (binding.toggleCameraBtn.isChecked) {
                    binding.localMediaContainer.showCameraTurnedOff()
                } else {
                    binding.localMediaContainer.showCameraTurnedOn()
                }
                viewModel.toggleLocalVideo()
            } else {
                showCustomToast(getString(R.string.teacher_turned_off_all_students_camera), false, true)
            }
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
                binding.localMediaContainer.showHandRaised()
            } else {
                viewModel.showHandLowered()
                binding.localMediaContainer.hideRaiseHand()
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
            // TODO This is added for testing purpouse and it will be removed later on
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

        upstreamConnection?.addOnNetworkQuality { networkQuality ->
            // TODO @Paul remove these after QA get their stats
            uiThreadPoster.post {
                //                shortToast(networkQuality.toString())
                Log.d(TAG, networkQuality.toString())
            }

            val averageNetworkQuality = when (liveClassManager.getNetworkQualityArray().size) {
                0 -> {
                    liveClassManager.addToNetworkQualityArray(networkQuality)
                    // If there is no value inside the array, take the current reading as it is
                    networkQuality
                }
                else -> {
                    liveClassManager.addToNetworkQualityArray(networkQuality)
                    val networkQualityArray = liveClassManager.getNetworkQualityArray()
                    // Calculate the average of the last 2 readings
                    networkQualityArray.subList(
                        networkQualityArray.size - 2,
                        networkQualityArray.size - 1
                    ).average()
                }
            }

            // Handle averageNetworkQuality only if it is different from the latest reading
            if (averageNetworkQuality != networkQuality) {
                when (averageNetworkQuality) {
                    in LiveSwitchNetworkQuality.MODERATE.lowerLimit..LiveSwitchNetworkQuality.MODERATE.upperLimit -> {
                        liveClassManager.getDownStreamConnections().let { connectionsMap ->
                            liveClassManager.getDownStreamConnectionsRoles().let { rolesMap ->
                                connectionsMap.forEach { connection ->
                                    when (rolesMap[connection.key]) {
                                        STUDENT_ROLE -> {
                                            connection.value.videoStream.maxReceiveBitrate =
                                                StudentFeedQuality.MODERATE.bitrate
                                            connection.value.videoStream.maxSendBitrate =
                                                StudentFeedQuality.MODERATE.bitrate
                                        }
                                        TEACHER_ROLE -> {
                                            connection.value.videoStream.maxReceiveBitrate =
                                                TeacherFeedQuality.MODERATE.bitrate
                                            connection.value.videoStream.maxSendBitrate =
                                                TeacherFeedQuality.MODERATE.bitrate
                                        }
                                    }
                                }
                            }
                        }
                    }
                    in LiveSwitchNetworkQuality.GOOD.lowerLimit..LiveSwitchNetworkQuality.GOOD.upperLimit -> {
                        liveClassManager.getDownStreamConnections().let { connectionsMap ->
                            liveClassManager.getDownStreamConnectionsRoles().let { rolesMap ->
                                connectionsMap.forEach { connection ->
                                    when (rolesMap[connection.key]) {
                                        STUDENT_ROLE -> {
                                            connection.value.videoStream.maxReceiveBitrate =
                                                StudentFeedQuality.GOOD.bitrate
                                            connection.value.videoStream.maxSendBitrate =
                                                StudentFeedQuality.GOOD.bitrate
                                        }
                                        TEACHER_ROLE -> {
                                            connection.value.videoStream.maxReceiveBitrate =
                                                TeacherFeedQuality.GOOD.bitrate
                                            connection.value.videoStream.maxSendBitrate =
                                                TeacherFeedQuality.GOOD.bitrate
                                        }
                                    }
                                }
                            }
                        }
                    }
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
            binding.localMediaContainer.showCameraTurnedOff()
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
        notificationToast = Toast(requireActivity())
        val params =
            (toastView.findViewById<RelativeLayout>(R.id.toast_container).layoutParams as ViewGroup.MarginLayoutParams)
        if (binding.liveClassOverlay.isVisible) {
            notificationToast?.setGravity(Gravity.TOP or Gravity.FILL, 0, 0)
            toastView.findViewById<TextView>(R.id.start_space).visibility = View.VISIBLE
            toastView.findViewById<TextView>(R.id.end_space).visibility = View.VISIBLE
            params.marginStart = 0
        } else {
            notificationToast?.setGravity(Gravity.START or Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 40, 40)
            toastView.findViewById<TextView>(R.id.start_space).visibility = View.GONE
            toastView.findViewById<TextView>(R.id.end_space).visibility = View.GONE
            params.marginStart = 30
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
                binding.localMediaContainer.updateLocalMediaViewOrientationDefault(
                    localMedia?.view
                )
            }
            if (display.rotation == Surface.ROTATION_270) {
                binding.localMediaContainer.updateLocalMediaViewOrientationReverse(
                    localMedia?.view
                )
            }
        } else {
            if (display.rotation == Surface.ROTATION_90) {
                binding.localMediaContainer.updateLocalMediaViewOrientationReverse(
                    localMedia?.view
                )
            }
            if (display.rotation == Surface.ROTATION_270) {
                binding.localMediaContainer.updateLocalMediaViewOrientationDefault(
                    localMedia?.view
                )
            }
        }
    }

    override fun onEnableMic() {
        uiThreadPoster.post {
            binding.localMediaContainer.showMicMuted()
            binding.toggleMicrophoneBtn.isActivated = true
            binding.toggleMicrophoneBtn.isChecked = true
        }
    }

    override fun onDisableMic(state: LiveClassState) {
        viewModel.turnOffAudio()
        uiThreadPoster.post {
            binding.localMediaContainer.showMicDisabledMuted()
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

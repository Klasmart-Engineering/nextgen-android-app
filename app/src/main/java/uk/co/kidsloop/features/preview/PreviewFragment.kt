package uk.co.kidsloop.features.preview

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import uk.co.kidsloop.R
import uk.co.kidsloop.app.KidsloopActivity
import uk.co.kidsloop.app.features.login.LoginFragmentDirections
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.app.utils.*
import uk.co.kidsloop.app.utils.permissions.isPermissionGranted
import uk.co.kidsloop.app.utils.permissions.showSettingsDialog
import uk.co.kidsloop.data.enums.KidsloopPermissions
import uk.co.kidsloop.databinding.PreviewFragmentBinding
import java.io.IOException

@AndroidEntryPoint
class PreviewFragment : BaseFragment(R.layout.preview_fragment) {

    private val binding by viewBinding(PreviewFragmentBinding::bind)
    private val viewModel by viewModels<PreviewViewModel>()
    private var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private var isCameraActive = false
    private var isMicRecording = true
    private var recordingThread: Thread? = null
    private var audioRecord: AudioRecord? = null
    private var isRecordingAudio = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cameraPreviewContainer.clipToOutline = true
        // 1. Check if both are granted
        // 2. Check if CAMERA is not granted, in this case show rationale if you should
        // 3. Check if RECORD_AUDIO is not granted, in this case show rationale if you should
        // 4. Request the initial permissions
        when {
            isPermissionGranted(requireContext(), KidsloopPermissions.CAMERA.type) &&
                    isPermissionGranted(
                        requireContext(),
                        KidsloopPermissions.RECORD_AUDIO.type
                    ) -> {
                viewModel.havePermissionsBeenPreviouslyDenied = false
                handleCameraFeed()
                startRecording()
                handleNoPermissionViews()
                handleToggles()
            }
            isPermissionGranted(requireContext(), KidsloopPermissions.CAMERA.type).not() ||
                    isPermissionGranted(
                        requireContext(),
                        KidsloopPermissions.RECORD_AUDIO.type
                    ).not() -> {
                if (shouldShowRequestPermissionRationale(KidsloopPermissions.CAMERA.type) ||
                    shouldShowRequestPermissionRationale(KidsloopPermissions.RECORD_AUDIO.type)
                ) {
                    showRationaleDialog(
                        KidsloopPermissions.getPreviewPermissions(),
                        getString(R.string.permission_rationale_dialog_title),
                        getString(R.string.permission_rationale_dialog_message)
                    )
                    handleNoPermissionViews()
                    handleToggles()
                } else
                    requestPermissionsLauncher.launch(KidsloopPermissions.getPreviewPermissions()) // Asking for @Permissions directly
            }
            else -> {
                requestPermissionsLauncher.launch(KidsloopPermissions.getPreviewPermissions()) // Asking for @Permissions directly
            }
        }

        setControls()
        observe()
    }

    override fun onResume() {
        super.onResume()
        // Handle return in the app from settings only when the user has previously denied the permissions
        if (isPermissionGranted(requireContext(), KidsloopPermissions.CAMERA.type) &&
            isPermissionGranted(
                requireContext(),
                KidsloopPermissions.RECORD_AUDIO.type
            ) && viewModel.havePermissionsBeenPreviouslyDenied
        ) {
            viewModel.havePermissionsBeenPreviouslyDenied = false
            handleCameraFeed()
            startRecording()
            handleNoPermissionViews()
            handleToggles()
        }
    }

    private fun setControls() {
        binding.cameraBtn.setOnClickListener {
            binding.noCameraTextview.isVisible = binding.cameraBtn.isChecked
            handleCameraFeed()
        }

        binding.backButton.setOnClickListener{
            Navigation.findNavController(requireView())
                .navigate(PreviewFragmentDirections.previewToLogin())
        }

        binding.microphoneBtn.setOnClickListener {
            binding.progressBar.isVisible = !binding.microphoneBtn.isChecked
            onRecord()
        }

        binding.allowBtn.setOnClickListener {
            if (shouldShowRequestPermissionRationale(KidsloopPermissions.CAMERA.type) ||
                shouldShowRequestPermissionRationale(KidsloopPermissions.RECORD_AUDIO.type)
            )
                requestPermissionsLauncher.launch(KidsloopPermissions.getPreviewPermissions())
            else {
                showSettingsDialog(activity as KidsloopActivity)
            }
        }

        binding.joinBtn.setOnClickListener {
            val isCameraTurnedOn = binding.cameraBtn.isChecked.not()
            val isMicrophoneTurnedOn = binding.microphoneBtn.isChecked.not()
            Navigation.findNavController(requireView())
                .navigate(
                    PreviewFragmentDirections.previewToLiveclass(
                        isCameraTurnedOn,
                        isMicrophoneTurnedOn
                    )
                )
        }
    }

    private fun observe() = with(viewModel) {
    }

    private fun handleNoPermissionViews() {
        if (!isPermissionGranted(
                requireContext(),
                KidsloopPermissions.CAMERA.type
            ) && !isPermissionGranted(requireContext(), KidsloopPermissions.RECORD_AUDIO.type)
        ) {
            binding.cameraPreview.invisible()
            binding.permissionsLayout.visible()
            binding.permissionsTextview.text = getString(R.string.permissions_denied)
            return
        } else if (!isPermissionGranted(requireContext(), KidsloopPermissions.CAMERA.type)) {
            binding.cameraPreview.invisible()
            binding.permissionsLayout.visible()
            binding.permissionsTextview.text = getString(R.string.camera_permission_denied)
            return
        } else if (!isPermissionGranted(requireContext(), KidsloopPermissions.RECORD_AUDIO.type)) {
            binding.cameraPreview.invisible()
            binding.permissionsLayout.visible()
            binding.permissionsTextview.text = getString(R.string.mic_permission_denied)
        } else {
            binding.cameraPreview.visible()
            binding.permissionsLayout.gone()
        }
    }

    private fun handleToggles() {
        // In one of the permissions is not enabled, do not enable any toggle
        if (!isPermissionGranted(
                requireContext(),
                KidsloopPermissions.RECORD_AUDIO.type
            ) || !isPermissionGranted(requireContext(), KidsloopPermissions.CAMERA.type)
        ) {
            binding.microphoneBtn.isEnabled = false
            binding.progressBar.isVisible = false
            binding.cameraBtn.isEnabled = false
            binding.joinBtn.isEnabled = false
        } else {
            binding.microphoneBtn.isEnabled =
                isPermissionGranted(requireContext(), KidsloopPermissions.RECORD_AUDIO.type)
            binding.progressBar.isVisible =
                isPermissionGranted(requireContext(), KidsloopPermissions.RECORD_AUDIO.type)
            binding.cameraBtn.isEnabled =
                isPermissionGranted(requireContext(), KidsloopPermissions.CAMERA.type)
            binding.joinBtn.isEnabled = true
        }
    }

    init {
        // this is a callback that handles permissions results
        this.requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            var allAreGranted = true
            for (value in result.values) {
                allAreGranted = allAreGranted && value
            }

            if (allAreGranted) {
                viewModel.havePermissionsBeenPreviouslyDenied = false
                handleCameraFeed()
                startRecording()
            } else {
                if (shouldShowRequestPermissionRationale(KidsloopPermissions.CAMERA.type) ||
                    shouldShowRequestPermissionRationale(KidsloopPermissions.RECORD_AUDIO.type)
                ) {
                    showRationaleDialog(
                        KidsloopPermissions.getPreviewPermissions(),
                        getString(R.string.permission_rationale_dialog_title),
                        getString(R.string.permission_rationale_dialog_message)
                    )
                }

                // Handle UI
                when (isPermissionGranted(
                    requireContext(),
                    KidsloopPermissions.CAMERA.type
                )) {
                    true -> {
                        viewModel.havePermissionsBeenPreviouslyDenied = false
                        handleCameraFeed()
                    }
                    false -> {
                        viewModel.havePermissionsBeenPreviouslyDenied = true
                    }
                }

                when (isPermissionGranted(
                    requireContext(),
                    KidsloopPermissions.RECORD_AUDIO.type
                )) {
                    true -> {
                        startRecording()
                    }
                    false -> {
                        viewModel.havePermissionsBeenPreviouslyDenied = true
                    }
                }
            }

            handleNoPermissionViews()
            handleToggles()
        }
    }

    private fun onRecord() {
        if (!isMicRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        if (audioRecord == null) {
            if (isPermissionGranted(requireContext(), KidsloopPermissions.RECORD_AUDIO.type).not())
                return

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                BUFFER_SIZE_RECORDING
            )

            if (audioRecord!!.state != AudioRecord.STATE_INITIALIZED) { // check for proper initialization
                Log.e(TAG, "error initializing AudioRecord")
                shortToast("Couldn't initialize AudioRecord, check configuration")
                return
            }

            audioRecord!!.startRecording()
            isRecordingAudio = true
            isMicRecording = true
            recordingThread = Thread { getAudioDataToProgressBar() }
            recordingThread!!.start()
        }
    }

    private fun getAudioDataToProgressBar() {
        val data =
            ByteArray(BUFFER_SIZE_RECORDING / 2)

        while (isRecordingAudio) {
            val read = audioRecord!!.read(data, 0, data.size)
            try {
                var sum = 0.0
                for (i in 0 until read) {
                    sum += (data[i] * data[i]).toDouble() + 50
                }
                if (read > 0) {
                    val amplitude = sum / read
                    binding.progressBar.progress = Math.sqrt(amplitude).toInt()
                }
            } catch (e: IOException) {
                Log.d(
                    TAG,
                    "Exception while recording with AudioRecord, $e"
                )
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        if (audioRecord != null) {
            isRecordingAudio = false
            isMicRecording = false
            audioRecord!!.stop()
            audioRecord!!.release()
            audioRecord = null
            recordingThread = null
        }
    }

    private fun handleCameraFeed() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            when (isCameraActive) {
                true -> {
                    cameraProvider.unbindAll()
                    binding.cameraPreviewContainer.removeAllViews()
                    isCameraActive = false
                }
                false -> {
                    cameraProvider.unbindAll()
                    binding.cameraPreviewContainer.removeAllViews()
                    binding.cameraPreviewContainer.addView(binding.cameraPreview)
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                        }

                    val cameraSelector =
                        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build()
                    cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview)
                    isCameraActive = true
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onAllowClicked(permissions: Array<String>) {
        super.onAllowClicked(permissions)
        requestPermissionsLauncher.launch(permissions)
    }

    override fun onDenyClicked(permissions: Array<String>) {
        super.onDenyClicked(permissions)
        viewModel.havePermissionsBeenPreviouslyDenied = true
    }

    override fun onStop() {
        super.onStop()
        isRecordingAudio = false
    }

    companion object {
        val TAG = PreviewFragment::class.qualifiedName
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_8BIT
        private val BUFFER_SIZE_RECORDING =
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
    }
}
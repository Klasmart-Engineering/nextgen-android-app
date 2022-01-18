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
    private var isCameraActive = true
    private var isMicRecording = true
    private var recordingThread: Thread? = null
    private var audioRecord: AudioRecord? = null
    private var isRecordingAudio = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                viewModel.isCameraGranted = true
                viewModel.isMicGranted = true
                displayCameraPreview()
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
        // Handle return in the app from settings without impacting somehow the rest
        if (isPermissionGranted(requireContext(), KidsloopPermissions.CAMERA.type) &&
            isPermissionGranted(
                requireContext(),
                KidsloopPermissions.RECORD_AUDIO.type
            )
        ) {
            viewModel.isCameraGranted = true
            viewModel.isMicGranted = true
            displayCameraPreview()
            startRecording()
            handleNoPermissionViews()
            handleToggles()
        }
    }

    private fun setControls() {
        binding.cameraBtn.setOnClickListener {
            when (binding.cameraBtn.isChecked) {
                true -> {
                    binding.cameraPreview.invisible()
                    binding.noCameraTextview.isVisible = true
                }
                false -> {
                    binding.cameraPreview.visible()
                    binding.noCameraTextview.isVisible = false
                }
            }
        }

        binding.microphoneBtn.setOnClickListener {
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
            Navigation.findNavController(requireView())
                .navigate(PreviewFragmentDirections.previewToLiveclass())
        }
    }

    private fun observe() = with(viewModel) {
    }

    private fun handleNoPermissionViews() {
        if (!viewModel.isCameraGranted && !viewModel.isMicGranted) {
            binding.cameraPreview.invisible()
            binding.permissionsLayout.visible()
            binding.permissionsTextview.text = getString(R.string.permissions_denied)
            return
        } else if (!viewModel.isCameraGranted) {
            binding.cameraPreview.invisible()
            binding.permissionsLayout.visible()
            binding.permissionsTextview.text = getString(R.string.camera_permission_denied)
            return
        } else if (!viewModel.isMicGranted) {
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
        if (!viewModel.isMicGranted || !viewModel.isCameraGranted) {
            binding.cameraBtn.isEnabled = false
            binding.microphoneBtn.isEnabled = false
        } else {
            binding.cameraBtn.isEnabled = viewModel.isCameraGranted
            binding.microphoneBtn.isEnabled = viewModel.isMicGranted
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
                viewModel.isCameraGranted = true
                viewModel.isMicGranted = true
                displayCameraPreview()
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
                        viewModel.isCameraGranted = true
                        displayCameraPreview()
                    }
                    false -> {
                        viewModel.isCameraGranted = false
                    }
                }

                when (isPermissionGranted(
                    requireContext(),
                    KidsloopPermissions.RECORD_AUDIO.type
                )) {
                    true -> {
                        viewModel.isMicGranted = true
                        startRecording()
                    }
                    false -> {
                        viewModel.isMicGranted = false
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
                    // This is commented for now....It crashes the app
                    // Moreover, be advised that we no longer have a progressBar inside the layout!
                    //binding.progressBar.progress = Math.sqrt(amplitude).toInt()
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
            audioRecord!!.stop()
            audioRecord!!.release()
            audioRecord = null
            recordingThread = null
        }
    }

    private fun displayCameraPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            if(isCameraActive){
                cameraProvider.unbindAll()
            }
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
            cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onAllowClicked(permissions: Array<String>) {
        super.onAllowClicked(permissions)
        requestPermissionsLauncher.launch(permissions)
    }

    override fun onDenyClicked(permissions: Array<String>) {
        super.onDenyClicked(permissions)
        viewModel.isCameraGranted = false
        viewModel.isMicGranted = false
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
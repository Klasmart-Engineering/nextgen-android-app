package uk.co.kidsloop.features.videostream

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import uk.co.kidsloop.R
import uk.co.kidsloop.app.KidsloopActivity
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.app.utils.*
import uk.co.kidsloop.app.utils.permissions.isPermissionGranted
import uk.co.kidsloop.app.utils.permissions.showSettingsDialog
import uk.co.kidsloop.data.enums.KidsloopPermissions
import uk.co.kidsloop.databinding.FragmentPreviewBinding
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PreviewFragment : BaseFragment(R.layout.fragment_preview) {

    private lateinit var binding: FragmentPreviewBinding
    private val viewModel by viewModels<PreviewViewModel>()

    private var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var cameraExecutor: ExecutorService
    private var isCameraActive = true
    private var isMicRecording = true
    private var recordingThread: Thread? = null

    var audioRecord: AudioRecord? = null
    var isRecordingAudio = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_preview, container, false)
        return binding.root
    }

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
                setUpCamera()
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

        cameraExecutor = Executors.newSingleThreadExecutor()
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
            setUpCamera()
            startRecording()
            handleNoPermissionViews()
            handleToggles()
        }
    }

    private fun setControls() {
        binding.cameraBtn.setOnClickListener {
            setUpCamera()
            when (binding.cameraBtn.isChecked) {
                true -> {
                    binding.viewFinder.invisible()
                    binding.noCameraTextview.isVisible = true
                }
                false -> {
                    binding.viewFinder.visible()
                    binding.noCameraTextview.isVisible = false
                }
            }
        }

        binding.microphoneBtn.setOnClickListener {
            binding.progressBar.setVisible(!binding.microphoneBtn.isChecked)
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
            viewModel.onChange()
        }
    }

    private fun observe() = with(viewModel) {
        viewModel.isChecked.observe(viewLifecycleOwner, Observer {
            updateUi(it);
        })
    }

    private fun handleNoPermissionViews() {
        if (!viewModel.isCameraGranted && !viewModel.isMicGranted) {
            binding.viewFinder.invisible()
            binding.permissionsLayout.visible()
            binding.permissionsTextview.text = getString(R.string.permissions_denied)
            return
        } else if (!viewModel.isCameraGranted) {
            binding.viewFinder.invisible()
            binding.permissionsLayout.visible()
            binding.permissionsTextview.text = getString(R.string.camera_permission_denied)
            return
        } else if (!viewModel.isMicGranted) {
            binding.viewFinder.invisible()
            binding.permissionsLayout.visible()
            binding.permissionsTextview.text = getString(R.string.mic_permission_denied)
        } else {
            binding.viewFinder.visible()
            binding.permissionsLayout.gone()
        }
    }

    private fun handleToggles() {
        // In one of the permissions is not enabled, do not enable any toggle
        if (!viewModel.isMicGranted || !viewModel.isCameraGranted) {
            binding.cameraBtn.isEnabled = false
            binding.microphoneBtn.isEnabled = false
            binding.progressBar.invisible()
        } else {
            binding.cameraBtn.isEnabled = viewModel.isCameraGranted

            if (viewModel.isMicGranted) {
                binding.microphoneBtn.isEnabled = true
                binding.progressBar.visible()
            } else {
                binding.microphoneBtn.isEnabled = false
                binding.progressBar.invisible()
            }
        }
    }

    private fun updateUi(isChecked: Boolean) {
        if (isChecked) {
            binding.joinBtn.text = getString(R.string.join)
            binding.joinBtn.isEnabled = true
        } else {
            binding.joinBtn.text = getString(R.string.waiting_teacher)
            binding.joinBtn.isEnabled = false
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
                setUpCamera()
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
                        setUpCamera()
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
                Log.e(LiveVideoStreamFragment.TAG, "error initializing AudioRecord")
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
            audioRecord!!.stop()
            audioRecord!!.release()
            audioRecord = null
            recordingThread = null
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = context?.let { ProcessCameraProvider.getInstance(it) }

        cameraProviderFuture?.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

            try {
                cameraProvider.unbindAll()

                if (isCameraActive) {
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview
                    )
                }
            } catch (exc: Exception) {
                Log.e(LiveVideoStreamFragment.TAG, "Use case binding failed", exc)
            }
        },
            context?.let { ContextCompat.getMainExecutor(it) }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
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
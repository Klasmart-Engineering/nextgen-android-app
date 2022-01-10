package uk.co.kidsloop.features.videostream

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
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
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.app.utils.*
import uk.co.kidsloop.data.enums.KidsloopPermissions
import uk.co.kidsloop.databinding.FragmentLiveVideostreamBinding
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LiveVideoStreamFragment : BaseFragment(R.layout.fragment_live_videostream) {

    private val binding by viewBinding(FragmentLiveVideostreamBinding::bind)
    private val viewModel by viewModels<LiveVideoStreamViewModel>()

    private lateinit var cameraExecutor: ExecutorService
    private var isCameraActive = true
    private var isMicRecording = true
    private var isCameraPermissionGranted = false
    private var isMicPermissionGranted = false
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
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityResultLauncher.launch(KidsloopPermissions.getPreviewPermissions())
        cameraExecutor = Executors.newSingleThreadExecutor()
        setControls()
    }

    private fun setControls() {
        binding.cameraBtn.setOnClickListener {
            if (isCameraPermissionGranted) {
                setUpCamera()
                binding.noCameraTextview.isVisible = binding.cameraBtn.isChecked
            }
        }

        binding.microphoneBtn.setOnClickListener {
            if (isMicPermissionGranted) {
                binding.progressBar.setVisible(!binding.microphoneBtn.isChecked)
                onRecord()
            }
        }
    }

    private var activityResultLauncher: ActivityResultLauncher<Array<String>>

    init {
        this.activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            var allAreGranted = true
            for (value in result.values) {
                allAreGranted = allAreGranted && value
            }

            if (allAreGranted) {
                isCameraPermissionGranted = true
                isMicPermissionGranted = true
                setUpCamera()
                startRecording()
            } else {
                when(isPermissionGranted(
                    requireContext(),
                    KidsloopPermissions.CAMERA.type
                )) {
                    true -> {
                        isCameraPermissionGranted = true
                        setUpCamera()
                    }
                    false -> {
                        isCameraPermissionGranted = false
                        binding.cameraBtn.setBackgroundResource(R.drawable.ic_cam_disabled)
                    }
                }

                when(isPermissionGranted(
                    requireContext(),
                    KidsloopPermissions.RECORD_AUDIO.type
                )) {
                    true -> {
                        isMicPermissionGranted = true
                        binding.progressBar.visible()
                        startRecording()
                    }
                    false -> {
                        isMicPermissionGranted = false
                        binding.microphoneBtn.isEnabled = false
                        binding.progressBar.invisible()
                        longToast(getString(R.string.mic_permission_denied))
                        binding.microphoneBtn.setBackgroundResource(R.drawable.ic_mic_disabled)
                    }
                }

                if(isCameraPermissionGranted.not() && isMicPermissionGranted.not()) {
                    binding.noCameraTextview.visibility = View.VISIBLE
                    binding.noCameraTextview.text = getString(R.string.permissions_denied)
                }
            }
        }
    }

    private fun onRecord() {
        if (!isMicRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        if (audioRecord == null) {
            if (context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.RECORD_AUDIO
                    )
                } != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

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

        cameraProviderFuture?.addListener(
            Runnable {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

                val cameraSelector =
                    CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build()

                try {
                    cameraProvider.unbindAll()

                    if (isCameraActive) {
                        cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview
                        )
                    }
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }
            },
            context?.let { ContextCompat.getMainExecutor(it) }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        val TAG = LiveVideoStreamFragment::class.qualifiedName
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_8BIT
        private val BUFFER_SIZE_RECORDING =
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
    }
}

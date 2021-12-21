package uk.co.kidsloop.app.features.videostream

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import uk.co.kidsloop.R
import uk.co.kidsloop.app.BaseFragment
import uk.co.kidsloop.app.viewmodel.ViewModelFactory
import uk.co.kidsloop.databinding.LiveVideostreamFragmentBinding
import uk.co.kidsloop.features.videostream.LiveVideoStreamViewModel
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

class LiveVideoStreamFragment : BaseFragment(R.layout.live_videostream_fragment) {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private val binding by viewBinding(LiveVideostreamFragmentBinding::bind)

    private val viewModel: LiveVideoStreamViewModel by viewModels { viewModelFactory }
    private lateinit var cameraExecutor: ExecutorService
    private var isCameraActive = true
    private var isMicRecording = true

    val SAMPLE_RATE = 44100 // supported on all devices
    val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
    val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_8BIT // not supported on all devices
    val BUFFER_SIZE_RECORDING =
        AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
    var audioRecord: AudioRecord? = null
    var isRecordingAudio = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)

        cameraExecutor = Executors.newSingleThreadExecutor()
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
        binding.cameraBtn.setOnClickListener {
            isCameraActive = !binding.cameraBtn.isChecked
            setUpCamera()
        }

        binding.microphoneBtn.setOnClickListener {
            isMicRecording = !binding.microphoneBtn.isChecked
            onRecord()
        }
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private var activityResultLauncher: ActivityResultLauncher<Array<String>>
    init {
        this.activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            var allAreGranted = true
            for (b in result.values) {
                allAreGranted = allAreGranted && b
            }

            if (allAreGranted) {
                setUpCamera()
                startRecording()
            } else {
                Toast.makeText(context, "Permissions denied!", Toast.LENGTH_LONG).show()
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
        if (audioRecord == null) { // safety check

            if (context?.let {
                    ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.RECORD_AUDIO
                    )
                } != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                BUFFER_SIZE_RECORDING
            )

            if (audioRecord!!.getState() != AudioRecord.STATE_INITIALIZED) { // check for proper initialization
                Log.e(TAG, "error initializing AudioRecord")
                Toast.makeText(
                    context,
                    "Couldn't initialize AudioRecord, check configuration",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            audioRecord!!.startRecording()
            Toast.makeText(
                context,
                "recording started with AudioRecord",
                Toast.LENGTH_SHORT
            ).show()

            isRecordingAudio = true
        }
    }

    private fun stopRecording() {
        if (audioRecord != null) {
            Toast.makeText(
                context,
                "recording stopped with AudioRecord",
                Toast.LENGTH_SHORT
            ).show()
            isRecordingAudio = false
            audioRecord!!.stop()
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

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

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
        private const val TAG = "CameraX"
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }
}

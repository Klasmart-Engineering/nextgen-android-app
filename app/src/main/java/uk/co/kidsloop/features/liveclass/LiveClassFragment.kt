package uk.co.kidsloop.features.liveclass

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import fm.liveswitch.AudioStream
import fm.liveswitch.ConnectionInfo
import fm.liveswitch.android.LayoutManager
import uk.co.kidsloop.R
import uk.co.kidsloop.app.BaseFragment
import uk.co.kidsloop.databinding.LiveClassFragmentBinding
import javax.inject.Inject
import fm.liveswitch.ConnectionState
import fm.liveswitch.IAction1
import fm.liveswitch.LocalMedia
import fm.liveswitch.ManagedConnection
import fm.liveswitch.SfuDownstreamConnection
import fm.liveswitch.VideoStream
import uk.co.kidsloop.features.liveclass.localmedia.CameraLocalMedia
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia

@AndroidEntryPoint
class LiveClassFragment : BaseFragment(R.layout.live_class_fragment) {

    @Inject
    lateinit var liveClassManager: LiveClassManager

    private val binding by viewBinding(LiveClassFragmentBinding::bind)
    private lateinit var layoutManager: LayoutManager
    private lateinit var localMedia: CameraLocalMedia

    private val viewModel: LiveClassViewModel by viewModels<LiveClassViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localMedia = CameraLocalMedia(requireContext(), false, false, AecContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layoutManager = LayoutManager(binding.videoContainer)
        layoutManager.localView = localMedia.view
        val channel = liveClassManager.getChannel()
        localMedia.start().then(IAction1 { localMedia ->
            viewModel.openSfuUpstreamConnection(getAudioStream(localMedia), getVideoStream(localMedia))
            for (connectionInfo: ConnectionInfo in channel.remoteUpstreamConnectionInfos) {
                openSfuDownstreamConnection(connectionInfo)
            }
        })
        channel.addOnRemoteUpstreamConnectionOpen(IAction1 { connectionInfo ->
            openSfuDownstreamConnection(connectionInfo)
        })
    }

    fun openSfuDownstreamConnection(remoteConnectionInfo: ConnectionInfo): SfuDownstreamConnection {
        // Create remote media.
        val remoteMedia = SFURemoteMedia(requireContext(), false, false, AecContext())

        // Adding remote view to UI.
        layoutManager.addRemoteView(remoteMedia.id, remoteMedia.view)

        // Create audio and video streams from remote media.
        val audioStream: AudioStream? = if (remoteConnectionInfo.hasAudio) AudioStream(remoteMedia) else null
        val videoStream = if (remoteConnectionInfo.hasVideo) VideoStream(remoteMedia) else null

        // Create a SFU downstream connection with remote audio and video and data streams.
        val connection: SfuDownstreamConnection =
            liveClassManager.getChannel().createSfuDownstreamConnection(remoteConnectionInfo, audioStream, videoStream)

        // Store the downstream connection.
        liveClassManager.saveDownStreamConnections(remoteMedia.id, connection)
        connection.addOnStateChange { conn: ManagedConnection ->
            if (conn.state == ConnectionState.Closing || conn.state == ConnectionState.Failing) {

                // Removing remote view from UI.
                layoutManager.removeRemoteView(remoteMedia.id)
                remoteMedia.destroy()
                liveClassManager.removeDownStreamConnection(remoteMedia.id)
            } else if (conn.state == ConnectionState.Failed) {
                // Reconnect if the connection failed.
                openSfuDownstreamConnection(remoteConnectionInfo)
            }
        }
        connection.open()
        return connection
    }

    private fun getAudioStream(localMedia: LocalMedia): AudioStream? {
        return if (localMedia.audioTrack != null) AudioStream(localMedia.audioTrack) else null
    }

    private fun getVideoStream(localMedia: LocalMedia): VideoStream? {
        return if (localMedia.videoTrack != null) VideoStream(localMedia.videoTrack) else null
    }
}

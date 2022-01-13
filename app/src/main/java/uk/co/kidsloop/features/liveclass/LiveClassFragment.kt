package uk.co.kidsloop.features.liveclass

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import fm.liveswitch.AudioStream
import fm.liveswitch.Channel
import fm.liveswitch.ConnectionInfo
import fm.liveswitch.ConnectionState
import fm.liveswitch.IAction1
import fm.liveswitch.android.LayoutManager
import uk.co.kidsloop.R
import uk.co.kidsloop.app.BaseFragment
import uk.co.kidsloop.databinding.LiveClassFragmentBinding
import fm.liveswitch.LocalMedia
import fm.liveswitch.ManagedConnection
import fm.liveswitch.SfuDownstreamConnection
import fm.liveswitch.VideoStream
import uk.co.kidsloop.features.liveclass.localmedia.CameraLocalMedia
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia

@AndroidEntryPoint
class LiveClassFragment : BaseFragment(R.layout.live_class_fragment) {

    private val binding by viewBinding(LiveClassFragmentBinding::bind)
    private var layoutManager: LayoutManager? = null
    private var localMedia: CameraLocalMedia? = null

    private val viewModel: LiveClassViewModel by viewModels<LiveClassViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localMedia = CameraLocalMedia(requireActivity(), false, false, AecContext())
        layoutManager = LayoutManager(binding.videoContainer)
        layoutManager?.localView = localMedia?.view

        viewModel.joinLiveClass()

        viewModel.classroomStateLiveData.observe(viewLifecycleOwner, Observer
        {
            when (it) {
                is LiveClassViewModel.LiveClassState.Loading -> showLoading()
                is LiveClassViewModel.LiveClassState.RegistrationSuccessful -> onClientRegistered(it.channel)
                is LiveClassViewModel.LiveClassState.FailedToJoiningLiveClass -> handleFailures()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        localMedia?.start()?.then({ localMedia -> }, { exception -> })
    }

    override fun onPause() {
        super.onPause()
        stopLocalMedia()
    }

    fun openSfuDownstreamConnection(remoteConnectionInfo: ConnectionInfo, channel: Channel): SfuDownstreamConnection {
        // Create remote media.

        val remoteMedia = SFURemoteMedia(requireContext(), false, false, AecContext())
        // Adding remote view to UI.
        layoutManager?.addRemoteView(remoteMedia.id, remoteMedia.view)

        // Create audio and video streams from remote media.
        val audioStream: AudioStream? = if (remoteConnectionInfo.hasAudio) AudioStream(remoteMedia) else null
        val videoStream = if (remoteConnectionInfo.hasVideo) VideoStream(remoteMedia) else null

        // Create a SFU downstream connection with remote audio and video and data streams.
        val connection: SfuDownstreamConnection = channel.createSfuDownstreamConnection(remoteConnectionInfo, audioStream, videoStream)

        // Store the downstream connection.
        //liveClassManager.saveDownStreamConnections(remoteMedia.id, connection)
        connection.addOnStateChange { conn: ManagedConnection ->
            if (conn.state == ConnectionState.Closing || conn.state == ConnectionState.Failing) {

                // Removing remote view from UI.
                layoutManager?.removeRemoteView(remoteMedia.id)
                remoteMedia.destroy()
                //liveClassManager.removeDownStreamConnection(remoteMedia.id)
            } else if (conn.state == ConnectionState.Failed) {
                // Reconnect if the connection failed.
                // openSfuDownstreamConnection(remoteConnectionInfo)
            }
        }
        connection.open()
        return connection
    }

    private fun getAudioStream(localMedia: LocalMedia?): AudioStream? {
        return if (localMedia?.audioTrack != null) AudioStream(localMedia.audioTrack) else null
    }

    private fun getVideoStream(localMedia: LocalMedia?): VideoStream? {
        return if (localMedia?.videoTrack != null) VideoStream(localMedia.videoTrack) else null
    }

    private fun showLoading() {
    }

    private fun hideLoading() {
    }

    private fun handleFailures() {
    }

    private fun onClientRegistered(channel: Channel) {
        Log.d("LiveClassManager", "onClientRegistered")
        val upstreamConnection = viewModel.openSfuUpstreamConnection(getAudioStream(localMedia), getVideoStream(localMedia))

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
        localMedia?.stop()?.then(IAction1 { result ->
            layoutManager?.removeRemoteViews()
            layoutManager?.unsetLocalView()
            layoutManager = null

            localMedia?.destroy()
            localMedia = null
        })
    }
}

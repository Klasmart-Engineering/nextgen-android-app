package uk.co.kidsloop.features.liveclass

import android.content.Context
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
import fm.liveswitch.IAction1
import fm.liveswitch.android.LayoutManager
import uk.co.kidsloop.R
import uk.co.kidsloop.app.BaseFragment
import uk.co.kidsloop.databinding.LiveClassFragmentBinding
import fm.liveswitch.SfuDownstreamConnection
import fm.liveswitch.VideoStream
import uk.co.kidsloop.features.liveclass.localmedia.CameraLocalMedia
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia
import uk.co.kidsloop.features.liveclass.localmedia.LocalMedia
import javax.inject.Inject

@AndroidEntryPoint
class LiveClassFragment : BaseFragment(R.layout.live_class_fragment) {

    @Inject lateinit var appContext: Context

    private val binding by viewBinding(LiveClassFragmentBinding::bind)
    private var layoutManager: LayoutManager? = null
    private var localMedia: LocalMedia<View>? = null

    private val viewModel: LiveClassViewModel by viewModels<LiveClassViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localMedia = CameraLocalMedia(appContext, false, false, AecContext())
        layoutManager = LayoutManager(binding.videoContainer)
        layoutManager?.localView = localMedia?.view
        startLocalMedia()

        viewModel.classroomStateLiveData.observe(viewLifecycleOwner, Observer
        {
            when (it) {
                is LiveClassViewModel.LiveClassState.Loading -> showLoading()
                is LiveClassViewModel.LiveClassState.RegistrationSuccessful -> onClientRegistered(it.channel)
                is LiveClassViewModel.LiveClassState.FailedToJoiningLiveClass -> handleFailures()
            }
        })

        binding.toggleAudioBtn.setOnClickListener {
            viewModel.toggleLocalAudio()
        }
        binding.toggleVideoBtn.setOnClickListener {
            viewModel.toggleLocalVideo()
        }
    }

    fun openSfuDownstreamConnection(remoteConnectionInfo: ConnectionInfo, channel: Channel): SfuDownstreamConnection {
        // Create remote media.

        val remoteMedia = SFURemoteMedia(requireContext(), false, false, AecContext())
        // Adding remote view to UI.
       // layoutManager?.alignment = LayoutAlignment.BottomRight
        layoutManager?.addRemoteView(remoteMedia.id, remoteMedia.view)

        // Create audio and video streams from remote media.
        val audioStream: AudioStream? = if (remoteConnectionInfo.hasAudio) AudioStream(remoteMedia) else null
        val videoStream = if (remoteConnectionInfo.hasVideo) VideoStream(remoteMedia) else null

        // Create a SFU downstream connection with remote audio and video and data streams.
        val connection: SfuDownstreamConnection = channel.createSfuDownstreamConnection(remoteConnectionInfo, audioStream, videoStream)

        // Store the downstream connection.
        //liveClassManager.saveDownStreamConnections(remoteMedia.id, connection)
        //        connection.addOnStateChange { conn: ManagedConnection ->
        //            if (conn.state == ConnectionState.Closing || conn.state == ConnectionState.Failing) {
        //
        //                // Removing remote view from UI.
        //                layoutManager?.removeRemoteView(remoteMedia.id)
        //                remoteMedia.destroy()
        //                //liveClassManager.removeDownStreamConnection(remoteMedia.id)
        //            } else if (conn.state == ConnectionState.Failed) {
        //                // Reconnect if the connection failed.
        //                // openSfuDownstreamConnection(remoteConnectionInfo)
        //            }
        //        }
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

    private fun startLocalMedia() {
        localMedia?.start()?.then({ localMedia ->
                                      requireActivity().runOnUiThread {
                                            viewModel.joinLiveClass()
                                      }
                                  }, { exception -> })
    }
}

package uk.co.kidsloop.features.liveclass.usecases

import fm.liveswitch.AudioStream
import fm.liveswitch.ConnectionInfo
import fm.liveswitch.SfuDownstreamConnection
import fm.liveswitch.VideoStream
import javax.inject.Inject
import uk.co.kidsloop.features.liveclass.LiveClassManager
import uk.co.kidsloop.features.liveclass.remoteviews.SFURemoteMedia
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import uk.co.kidsloop.liveswitch.Config

class OpenSfuDownstreamConnection @Inject constructor(private val liveClassManager: LiveClassManager) {

    fun openSfuDownstreamConnection(
        remoteConnectionInfo: ConnectionInfo,
        remoteMedia: SFURemoteMedia
    ): SfuDownstreamConnection? {
        // Create audio and video streams from remote media.
        val audioStream: AudioStream? =
            if (remoteConnectionInfo.hasAudio) AudioStream(remoteMedia) else null
        val videoStream: VideoStream? =
            if (remoteConnectionInfo.hasVideo) VideoStream(remoteMedia) else null

        val channel = liveClassManager.getChannel()
        // Create a SFU downstream connection with remote audio and video and data streams.
        val connection: SfuDownstreamConnection? =
            channel?.createSfuDownstreamConnection(
                remoteConnectionInfo,
                audioStream,
                videoStream,
                liveClassManager.getNewDownstreamDataStream()
            )

        liveClassManager.saveDownStreamConnection(remoteConnectionInfo.clientId, connection)
        if (remoteConnectionInfo.clientRoles[0] == Config.TEACHER_ROLE) {
            liveClassManager.setState(LiveClassState.LIVE_CLASS_STARTED)
        }
        connection?.open()
        return connection
    }
}

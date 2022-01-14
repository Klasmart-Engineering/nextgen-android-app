package uk.co.kidsloop.features.liveclass

import fm.liveswitch.AudioStream
import fm.liveswitch.SfuUpstreamConnection
import fm.liveswitch.VideoStream
import javax.inject.Inject

class OpenSfuUpstreamConnectionUseCase @Inject constructor(private val liveClassManager: LiveClassManager) {

    fun openSfuUpstreamConnection(audioStream: AudioStream?, videoStream: VideoStream?): SfuUpstreamConnection {
        val channel = liveClassManager.getChannel()
        val connection: SfuUpstreamConnection = channel.createSfuUpstreamConnection(audioStream, videoStream)
        return connection
    }
}
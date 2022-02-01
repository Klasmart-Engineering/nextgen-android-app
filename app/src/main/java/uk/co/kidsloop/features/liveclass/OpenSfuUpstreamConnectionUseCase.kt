package uk.co.kidsloop.features.liveclass

import fm.liveswitch.*
import javax.inject.Inject

class OpenSfuUpstreamConnectionUseCase @Inject constructor(private val liveClassManager: LiveClassManager) {

    fun openSfuUpstreamConnection(
        audioStream: AudioStream?,
        videoStream: VideoStream?
    ): SfuUpstreamConnection? {
        val channel = liveClassManager.getChannel()
        val connection: SfuUpstreamConnection? = channel?.createSfuUpstreamConnection(audioStream, videoStream)
        //connection?.open()
        return connection
    }
}
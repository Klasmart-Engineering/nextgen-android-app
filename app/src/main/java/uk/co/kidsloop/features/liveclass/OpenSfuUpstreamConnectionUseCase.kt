package uk.co.kidsloop.features.liveclass

import fm.liveswitch.*
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import javax.inject.Inject

class OpenSfuUpstreamConnectionUseCase @Inject constructor(private val liveClassManager: LiveClassManager) {

    fun openSfuUpstreamConnection(
        audioStream: AudioStream?,
        videoStream: VideoStream?
    ): SfuUpstreamConnection? {
        val channel = liveClassManager.getChannel()
        val dataStream = liveClassManager.getUpstreamDataStream()

        val upstreamConnection = channel?.createSfuUpstreamConnection(audioStream, videoStream, dataStream)
        upstreamConnection?.statsEventInterval = LiveClassManager.STATS_COLLECTING_INTERVAL
        upstreamConnection?.open()
        liveClassManager.setState(LiveClassState.JOINED)
        return upstreamConnection
    }
}
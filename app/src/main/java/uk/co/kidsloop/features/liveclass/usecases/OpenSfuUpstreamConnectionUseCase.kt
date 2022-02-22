package uk.co.kidsloop.features.liveclass.usecases

import fm.liveswitch.*
import uk.co.kidsloop.features.liveclass.LiveClassManager
import javax.inject.Inject
import uk.co.kidsloop.features.liveclass.state.LiveClassState

class OpenSfuUpstreamConnectionUseCase @Inject constructor(
    private val liveClassManager: LiveClassManager
) {

    fun openSfuUpstreamConnection(
        audioStream: AudioStream?,
        videoStream: VideoStream?
    ): SfuUpstreamConnection? {
        val channel = liveClassManager.getChannel()
        val dataStream = liveClassManager.getUpstreamDataStream()

        // Set SimulcastMode
        videoStream?.simulcastMode = SimulcastMode.RtpStreamId

        val upstreamConnection = channel?.createSfuUpstreamConnection(audioStream, videoStream, dataStream)
        upstreamConnection?.statsEventInterval = LiveClassManager.STATS_COLLECTING_INTERVAL
        upstreamConnection?.open()
        upstreamConnection?.let {
            liveClassManager.setUpstreamConnection(it)
        }
        liveClassManager.setState(LiveClassState.JOINED_AND_WAITING_FOR_TEACHER)
        return upstreamConnection
    }
}

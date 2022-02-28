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
        // Set SimulcastMode
        videoStream?.simulcastMode = SimulcastMode.RtpStreamId

        val upstreamDataChannel = DataChannel("testDataChannel")
        val upstreamDataStream = DataStream(upstreamDataChannel)
        val upstreamConnection = channel?.createSfuUpstreamConnection(audioStream, videoStream, upstreamDataStream)
        upstreamConnection?.statsEventInterval = LiveClassManager.STATS_COLLECTING_INTERVAL
        upstreamConnection?.open()
        upstreamConnection?.let {
            liveClassManager.setUpstreamDataChannel(upstreamDataChannel)
            liveClassManager.setUpstreamDataStream(upstreamDataStream)
            liveClassManager.setUpstreamConnection(it)
        }
        liveClassManager.setState(LiveClassState.JOINED_AND_WAITING_FOR_TEACHER)
        return upstreamConnection
    }
}

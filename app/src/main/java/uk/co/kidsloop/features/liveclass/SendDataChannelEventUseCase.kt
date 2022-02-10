package uk.co.kidsloop.features.liveclass

import com.squareup.moshi.Moshi
import uk.co.kidsloop.data.enums.DataChannelActionsType
import uk.co.kidsloop.data.enums.KidsLoopDataChannel
import javax.inject.Inject

class SendDataChannelEventUseCase @Inject constructor(
    private val liveClassManager: LiveClassManager,
    private val moshi: Moshi
) {

    fun sendDataChannelEvent(eventType: DataChannelActionsType) {
        val jsonAdapter = moshi.adapter(KidsLoopDataChannel::class.java)
        val json = jsonAdapter.toJson(KidsLoopDataChannel(liveClassManager.getUpstreamClientId(), eventType))
        liveClassManager.sendDataString(json.toString())
    }
}

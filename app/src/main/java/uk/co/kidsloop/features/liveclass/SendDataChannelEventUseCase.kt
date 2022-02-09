package uk.co.kidsloop.features.liveclass

import com.squareup.moshi.Moshi
import javax.inject.Inject
import uk.co.kidsloop.data.enums.DataChannel
import uk.co.kidsloop.data.enums.DataChannelActionsType

class SendDataChannelEventUseCase @Inject constructor(
    private val liveClassManager: LiveClassManager,
    private val moshi: Moshi
) {

    fun sendDataChannelEvent(eventType: DataChannelActionsType) {
        val jsonAdapter = moshi.adapter<DataChannel>(DataChannel::class.java)
        val json = jsonAdapter.toJson(DataChannel(liveClassManager.getUpstreamClientId(), eventType))
        liveClassManager.sendDataString(json.toString())
    }
}

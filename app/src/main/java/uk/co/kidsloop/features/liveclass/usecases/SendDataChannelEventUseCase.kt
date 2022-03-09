package uk.co.kidsloop.features.liveclass.usecases

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.kidsloop.data.enums.DataChannelActionsType
import uk.co.kidsloop.data.enums.KidsLoopDataChannel
import uk.co.kidsloop.features.liveclass.LiveClassManager
import javax.inject.Inject

class SendDataChannelEventUseCase @Inject constructor(
    private val liveClassManager: LiveClassManager,
    private val moshi: Moshi
) {

    suspend fun sendDataChannelEvent(eventType: DataChannelActionsType): fm.liveswitch.Future<Any>? {
        return withContext(Dispatchers.IO) {
            val jsonAdapter = moshi.adapter(KidsLoopDataChannel::class.java)
            val json = jsonAdapter.toJson(KidsLoopDataChannel(liveClassManager.getUpstreamClientId(), eventType))
            liveClassManager.sendDataString(json.toString())
        }
    }
}

package uk.co.kidsloop.features.liveclass.usecases

import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.kidsloop.data.enums.DataChannelActionsType
import uk.co.kidsloop.data.enums.KidsLoopDataChannel
import uk.co.kidsloop.features.liveclass.LiveClassManager
import uk.co.kidsloop.features.liveclass.LiveClassViewModel
import javax.inject.Inject

class SendDataChannelEventUseCase @Inject constructor(
    private val liveClassManager: LiveClassManager,
    private val moshi: Moshi
) {
    private var _classroomStateLiveData = MutableLiveData<LiveClassViewModel.LiveClassUiState>()

    suspend fun sendDataChannelEvent(eventType: DataChannelActionsType) {
        withContext(Dispatchers.IO) {
            val jsonAdapter = moshi.adapter(KidsLoopDataChannel::class.java)
            val json = jsonAdapter.toJson(KidsLoopDataChannel(liveClassManager.getUpstreamClientId(), eventType))
            liveClassManager.sendDataString(json.toString())?.then {
                _classroomStateLiveData.postValue(LiveClassViewModel.LiveClassUiState.LiveClassEnded)
            }
        }
    }
}

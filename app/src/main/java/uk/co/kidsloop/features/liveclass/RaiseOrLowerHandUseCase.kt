package uk.co.kidsloop.features.liveclass

import com.squareup.moshi.Moshi
import uk.co.kidsloop.data.enums.DataChannelActions
import uk.co.kidsloop.features.liveclass.datachannel.RaiseHand
import javax.inject.Inject

class RaiseOrLowerHandUseCase @Inject constructor(private val liveClassManager: LiveClassManager, private val moshi:Moshi) {

    fun raiseOrLowerHand(data:DataChannelActions){
//        val jsonAdapter = moshi.adapter<RaiseHand>(RaiseHand::class.java)
//        val json = jsonAdapter.toJson(RaiseHand(shouldRaiseHand, shouldLowerHand))
        liveClassManager.sendDataString(data)
    }
}
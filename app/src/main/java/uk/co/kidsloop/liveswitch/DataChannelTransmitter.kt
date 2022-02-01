package uk.co.kidsloop.liveswitch

import uk.co.kidsloop.data.enums.DataChannelActions
import uk.co.kidsloop.features.liveclass.LiveClassManager

/**
 *  Created by paulbisioc on 01.02.2022
 *
 *  Events sent over the DataChannel will always have the same format:
 *  $DataChannelAction:$param1:$param2:...
 */
object DataChannelTransmitter {
    fun sendRaiseHand(liveClassManager: LiveClassManager, id: String) {
        val payload = DataChannelActions.RAISE_HAND.type + ":" + id
        liveClassManager.sendDataString(payload)
    }

    fun sendLowerHand(liveClassManager: LiveClassManager, id: String) {
        val payload = DataChannelActions.LOWER_HAND.type + ":" + id
        liveClassManager.sendDataString(payload)
    }
}
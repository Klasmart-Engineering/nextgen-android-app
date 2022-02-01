package uk.co.kidsloop.liveswitch

/**
 *  Created by paulbisioc on 25.01.2022
 */
interface DataChannelActionsHandler {
    fun onRaiseHand(remoteId: String) {}
    fun onLowerHand(remoteId: String) {}
}
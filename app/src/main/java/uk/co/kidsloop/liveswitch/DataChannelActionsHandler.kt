package uk.co.kidsloop.liveswitch

/**
 *  Created by paulbisioc on 25.01.2022
 */
interface DataChannelActionsHandler {
    fun onRaiseHand(mediaId: String) {}
    fun onLowerHand(mediaId: String) {}
}
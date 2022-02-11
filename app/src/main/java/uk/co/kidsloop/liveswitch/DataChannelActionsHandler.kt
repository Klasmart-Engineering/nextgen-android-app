package uk.co.kidsloop.liveswitch

/**
 *  Created by paulbisioc on 25.01.2022
 */
interface DataChannelActionsHandler {
    fun onRaiseHand(clientId: String?)
    fun onLowerHand(clientId: String?)
    fun onVideoTurnedOff()
    fun onVideoEnabled()
    fun onEnableMic()
    fun onDisableMic()
}

package uk.co.kidsloop.liveswitch

import uk.co.kidsloop.features.liveclass.state.LiveClassState

/**
 *  Created by paulbisioc on 25.01.2022
 */
interface DataChannelActionsHandler {
    fun onRaiseHand(clientId: String?)
    fun onLowerHand(clientId: String?)
    fun onVideoDisabled(state: LiveClassState)
    fun onVideoEnabled()
    fun onEnableMic()
    fun onDisableMic(state: LiveClassState)
}

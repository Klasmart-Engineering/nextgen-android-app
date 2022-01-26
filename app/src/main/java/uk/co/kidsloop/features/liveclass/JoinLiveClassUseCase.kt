package uk.co.kidsloop.features.liveclass

import fm.liveswitch.Channel
import fm.liveswitch.ChannelClaim
import fm.liveswitch.Client
import fm.liveswitch.ClientState
import fm.liveswitch.Future
import fm.liveswitch.IAction1
import fm.liveswitch.Token
import uk.co.kidsloop.data.enums.SharedPrefsWrapper
import uk.co.kidsloop.liveswitch.Config
import javax.inject.Inject

class JoinLiveClassUseCase @Inject constructor(private val liveClassManager: LiveClassManager, private val sharedPrefsWrapper: SharedPrefsWrapper) {

    fun joinAsync(): Future<Array<Channel>> {
        val client = Client(Config.gatewayUrl, Config.applicationId, null, null, null, arrayOf(sharedPrefsWrapper.getRole()))
        val token = Token.generateClientRegisterToken(
            Config.applicationId, client.userId, client.deviceId, client.id,
            client.roles,
            arrayOf(ChannelClaim(Config.channelId)), Config.sharedSecret
        )
        liveClassManager.setToken(token)
        liveClassManager.setClient(client)


        client.addOnStateChange {
            IAction1<Client> { client ->
                if (client.state == ClientState.Unregistered) {

                }
            }
        }
        return client.register(token).then { channels -> liveClassManager.setChannel(channels[0]) }
    }
}
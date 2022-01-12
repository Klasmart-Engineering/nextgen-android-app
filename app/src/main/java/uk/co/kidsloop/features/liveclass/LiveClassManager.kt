package uk.co.kidsloop.features.liveclass

import fm.liveswitch.Channel
import fm.liveswitch.Client
import fm.liveswitch.SfuDownstreamConnection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveClassManager @Inject constructor() {

    private val downstreamConnectionsMap = mutableMapOf<String, SfuDownstreamConnection>()

    private lateinit var token: String
    private lateinit var remoteChannel: Channel
    private lateinit var client: Client

    fun setToken(token: String) {
        this.token = token
    }

    fun getToken(): String {
        return token
    }

    fun setChannel(channel: Channel) {
        remoteChannel = channel
    }

    fun getChannel(): Channel {
        return remoteChannel
    }

    fun setClient(client: Client) {
        this.client = client
    }

    fun getClient(): Client {
        return client
    }

    fun saveDownStreamConnections(remoteId: String, connection: SfuDownstreamConnection) {
        downstreamConnectionsMap[remoteId] = connection
    }

    fun removeDownStreamConnection(remoteId: String) {
        downstreamConnectionsMap.remove(remoteId)
    }
}
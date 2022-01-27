package uk.co.kidsloop.features.liveclass

import fm.liveswitch.Channel
import fm.liveswitch.Client
import fm.liveswitch.SfuDownstreamConnection
import fm.liveswitch.SfuUpstreamConnection
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveClassManager @Inject constructor() {

    private var upstreamConnection: SfuUpstreamConnection? = null

    private val downstreamConnectionsMap = mutableMapOf<String, SfuDownstreamConnection>()

    private var token: String? = null
    private var remoteChannel: Channel? = null
    private var client: Client? = null

    private var liveClassState: LiveClassState = LiveClassState.IDLE

    fun setToken(token: String) {
        this.token = token
    }

    fun getToken(): String? {
        return token
    }

    fun setChannel(channel: Channel) {
        remoteChannel = channel
    }

    fun getChannel(): Channel? {
        return remoteChannel
    }

    fun setClient(client: Client) {
        this.client = client
    }

    fun getClient(): Client? {
        return client
    }

    fun saveDownStreamConnections(remoteId: String, connection: SfuDownstreamConnection) {
        downstreamConnectionsMap[remoteId] = connection
    }

    fun getNumberOfActiveDownStreamConnections(): Int {
        return downstreamConnectionsMap.size
    }

    fun removeDownStreamConnection(remoteId: String) {
        downstreamConnectionsMap.remove(remoteId)
    }

    fun setUpstreamConnection(upstreamConnection: SfuUpstreamConnection) {
        this.upstreamConnection = upstreamConnection
    }

    fun getUpstreamConnection(): SfuUpstreamConnection? {
        return upstreamConnection
    }

    fun cleanConnection() {
        client = null
        remoteChannel = null
        upstreamConnection = null
        token = null
    }

    fun setState(newState: LiveClassState) {
        this.liveClassState = newState
    }

    fun getState(): LiveClassState {
        return liveClassState
    }
}
package uk.co.kidsloop.features.liveclass

import fm.liveswitch.*
import uk.co.kidsloop.data.enums.DataChannelActions
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import uk.co.kidsloop.liveswitch.DataChannelActionsHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveClassManager @Inject constructor() {

    companion object {

        const val STATS_COLLECTING_INTERVAL = 2500
    }

    private var upstreamConnection: SfuUpstreamConnection? = null

    // Data Channels & Streams
    // Upstream
    private var upstreamDataChannel: DataChannel? = null
    private var upstreamDataStream: DataStream? = null

    private val downstreamConnectionsMap = mutableMapOf<String, SfuDownstreamConnection>()

    private var token: String? = null
    private var remoteChannel: Channel? = null
    private var client: Client? = null

    var dataChannelActionsHandler: DataChannelActionsHandler? = null
    private var liveClassState: LiveClassState = LiveClassState.IDLE

    init {
        setUpstreamDataChannel()
    }

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

    fun getUpstreamDataStream(): DataStream? {
        return upstreamDataStream
    }

    // For the Downstreams, it needs to be an unique DataChannel and Data Stream for every connection,
    // so we will instantiate each one when we will open the DownstreamConnection.
    fun getNewDownstreamDataStream(): DataStream {
        val dataChannel = DataChannel("testDataChannel")
        dataChannel.setOnReceive { dataChannelReceiveArgs ->
            dataChannelReceiveArgs.dataString?.let {
                parseReceivedDataString(it)
            }
        }
        return DataStream(dataChannel)
    }

    fun saveDownStreamConnections(clientId: String, connection: SfuDownstreamConnection) {
        downstreamConnectionsMap[clientId] = connection
    }

    fun getDownStreamConnections(): Map<String, SfuDownstreamConnection> {
        return downstreamConnectionsMap.toMap()
    }

    fun getNumberOfActiveDownStreamConnections(): Int {
        return downstreamConnectionsMap.size
    }

    fun removeDownStreamConnection(clientId: String) {
        downstreamConnectionsMap.remove(clientId)
    }

    fun setUpstreamConnection(upstreamConnection: SfuUpstreamConnection) {
        this.upstreamConnection = upstreamConnection
    }

    fun getUpstreamConnection(): SfuUpstreamConnection? {
        return upstreamConnection
    }

    private fun setUpstreamDataChannel() {
        // TODO @Paul see what you do with this label
        upstreamDataChannel = DataChannel("testDataChannel")
        upstreamDataStream = DataStream(upstreamDataChannel)
    }

    fun sendDataString(data: String) {
        if (isUpstreamDataChannelConnected()) {
            upstreamDataChannel?.sendDataString(data)
        }
    }

    fun sendDataString(dataChannelActions: DataChannelActions) {
        if (isUpstreamDataChannelConnected()) {
            val data = dataChannelActions.type + ":" + getUpstreamConnection()?.id
            upstreamDataChannel?.sendDataString(data)
        }
    }

    private fun parseReceivedDataString(data: String?) {
        data?.let {
            val parsedData = data.split(":")
            handleReceivedDataString(parsedData)
        }
    }

    private fun handleReceivedDataString(data: List<String>) {
        when (data[0]) {
            DataChannelActions.RAISE_HAND.type -> {
                val clientId = data[1]
                dataChannelActionsHandler?.onRaiseHand(clientId)
            }
            DataChannelActions.LOWER_HAND.type -> {
                val remoteId = data[1]
                dataChannelActionsHandler?.onLowerHand(remoteId)
            }
        }
    }

    private fun isUpstreamDataChannelConnected(): Boolean {
        return upstreamDataChannel?.state == DataChannelState.Connected
    }

    fun cleanConnection() {
        client = null
        remoteChannel = null
        upstreamConnection = null
        upstreamDataChannel = null
        upstreamDataStream = null
        token = null
        liveClassState = LiveClassState.IDLE
    }

    fun setState(newState: LiveClassState) {
        this.liveClassState = newState
    }

    fun getState(): LiveClassState {
        return liveClassState
    }
}
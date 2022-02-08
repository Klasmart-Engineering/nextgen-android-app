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
    private val connectionsRoleMap = mutableMapOf<String, String>()


    // TODO @Paul modify this to a 2-element array if the strategy doesn't changes
    private val networkQualityArray = mutableListOf<Double>()

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
            dataChannelReceiveArgs.dataBytes?.let {
                parseReceivedDataBytes(it)
            }
        }
        return DataStream(dataChannel)
    }

    fun saveDownStreamConnection(clientId: String, connection: SfuDownstreamConnection) {
        downstreamConnectionsMap[clientId] = connection
    }

    fun removeDownStreamConnection(clientId: String) {
        downstreamConnectionsMap.remove(clientId)
    }

    fun getDownStreamConnections(): Map<String, SfuDownstreamConnection> {
        return downstreamConnectionsMap.toMap()
    }

    fun saveDownStreamConnectionRole(clientId: String, role: String) {
        connectionsRoleMap[clientId] = role
    }

    fun removeDownStreamConnectionRole(clientId: String) {
        connectionsRoleMap.remove(clientId)
    }

    fun getDownStreamConnectionsRoles(): Map<String, String> {
        return connectionsRoleMap.toMap()
    }

    fun setUpstreamConnection(upstreamConnection: SfuUpstreamConnection) {
        this.upstreamConnection = upstreamConnection
    }

    fun getUpstreamConnection(): SfuUpstreamConnection? {
        return upstreamConnection
    }

    fun getNetworkQualityArray(): MutableList<Double> {
        return networkQualityArray
    }

    fun addToNetworkQualityArray(element: Double) {
        networkQualityArray.add(element)
    }

    private fun clearNetworkQualityArray() {
        networkQualityArray.clear()
    }

    private fun setUpstreamDataChannel() {
        // TODO @Paul see what you do with this label
        upstreamDataChannel = DataChannel("testDataChannel")
        upstreamDataStream = DataStream(upstreamDataChannel)
    }

    fun sendDataString(data: String) {
        if (isUpstreamDataChannelConnected())
            upstreamDataChannel?.sendDataString(data)
    }

    fun sendDataBytes(data: ByteArray) {
        if (isUpstreamDataChannelConnected())
            upstreamDataChannel?.sendDataBytes(DataBuffer.wrap(data))
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

    private fun parseReceivedDataBytes(data: DataBuffer?): ByteArray {
        data?.let {
            val bytes =
                it.data // The payload byte[] might contain extra bytes that are not part of the payload.
            val index = data.index // Starting index of the payload’s bytes you want.
            val length = data.length // Length of the payload’s bytes you want.

            val newValues = bytes.copyOfRange(index, index + length - 1)
            // TODO parsing of the states transmitted over the DataChannel will be done here

            return newValues
        }

        return ByteArray(0)
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
        clearNetworkQualityArray()
        liveClassState = LiveClassState.IDLE
    }

    fun setState(newState: LiveClassState) {
        this.liveClassState = newState
    }

    fun getState(): LiveClassState {
        return liveClassState
    }
}
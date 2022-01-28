package uk.co.kidsloop.features.liveclass

import fm.liveswitch.*
import uk.co.kidsloop.data.enums.DataChannelActions
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import uk.co.kidsloop.liveswitch.DataChannelActionsHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveClassManager @Inject constructor() {

    private var upstreamConnection: SfuUpstreamConnection? = null

    // Data Channels & Streams
    // Upstream
    private var upstreamDataChannel: DataChannel? = null
    private var upstreamDataStream: DataStream? = null

    // Downstream
    private var downstreamDataChannel: DataChannel? = null
    private var downstreamDataStream: DataStream? = null

    private val downstreamConnectionsMap = mutableMapOf<String, SfuDownstreamConnection>()

    private var token: String? = null
    private var remoteChannel: Channel? = null
    private var client: Client? = null

    var dataChannelActionsHandler: DataChannelActionsHandler? = null
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

    fun getUpstreamDataStream(): DataStream? {
        return upstreamDataStream
    }

    fun getDownstreamDataStream(): DataStream? {
        return downstreamDataStream
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

    fun setDataChannels() {
        // TODO @Paul see what you do with this label
        upstreamDataChannel = DataChannel("testDataChannel")
        upstreamDataStream = DataStream(upstreamDataChannel)
        downstreamDataChannel = DataChannel("testDataChannel")
        downstreamDataStream = DataStream(downstreamDataChannel)
    }

    fun setDataChannelsListeners() {
        // Receive data on your downstream
        downstreamDataChannel?.setOnReceive { dataChannelReceiveArgs ->
            dataChannelReceiveArgs.dataString?.let {
                handleReceivedDataString(it)
            }
            dataChannelReceiveArgs.dataBytes?.let {
                parseReceivedDataBytes(it)
            }
        }
    }

    fun sendDataString(data: String) {
        if (isUpstreamDataChannelConnected())
            upstreamDataChannel?.sendDataString(data)
    }

    fun sendDataBytes(data: ByteArray) {
        if (isUpstreamDataChannelConnected())
            upstreamDataChannel?.sendDataBytes(DataBuffer.wrap(data))
    }

    private fun handleReceivedDataString(data: String?) {
        when (data) {
            DataChannelActions.RAISE_HAND.type -> {
                dataChannelActionsHandler?.onRaiseHand()
            }
            DataChannelActions.LOWER_HAND.type -> {
                dataChannelActionsHandler?.onLowerHand()
            }
            else -> {}
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
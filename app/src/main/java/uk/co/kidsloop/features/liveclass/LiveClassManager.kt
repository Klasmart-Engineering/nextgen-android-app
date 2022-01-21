package uk.co.kidsloop.features.liveclass

import fm.liveswitch.*
import uk.co.kidsloop.app.utils.emptyString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveClassManager @Inject constructor() {

    private var upstreamConnection: SfuUpstreamConnection? = null

    // Data Channel & Stream
    private var dataChannel: DataChannel? = null
    private var dataStream: DataStream? = null

    private val downstreamConnectionsMap = mutableMapOf<String, SfuDownstreamConnection>()

    private var token: String? = null
    private var remoteChannel: Channel? = null
    private var client: Client? = null

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

    fun getDataStream(): DataStream? {
        return dataStream
    }

    fun saveDownStreamConnections(remoteId: String, connection: SfuDownstreamConnection) {
        downstreamConnectionsMap[remoteId] = connection
    }

    fun removeDownStreamConnection(remoteId: String) {
        downstreamConnectionsMap.remove(remoteId)
    }

    fun setUpstreamConnection(upstreamConnection: SfuUpstreamConnection) {
        this.upstreamConnection = upstreamConnection
        setDataChannel()
    }

    fun getUpstreamConnection(): SfuUpstreamConnection? {
        return upstreamConnection
    }

    private fun setDataChannel() {
        // TODO @Paul see what you do with this label
        dataChannel = DataChannel("testDataChannel")
        dataStream = DataStream(dataChannel)
        setDataChannelListeners()
    }

    private fun setDataChannelListeners() {
        // Catch the change of states
        dataChannel?.addOnStateChange {
            IAction1<DataChannel> { dataChannel ->
                // States are New, Connecting, Connected, Closing, Closed, Failed
                when (dataChannel.state) {
                    DataChannelState.Connected -> {

                    }
                    else -> {}
                }
            }
        }

        // Receive data on your channel
        dataChannel?.setOnReceive {
            IAction1<DataChannelReceiveArgs> { dataChannelReceiveArgs ->
                when (dataChannelReceiveArgs.dataString) {
                    null -> {}
                    else -> {
                        parseReceivedDataString(dataChannelReceiveArgs.dataString)
                    }
                }

                when (dataChannelReceiveArgs.dataBytes) {
                    null -> {}
                    else -> {
                        parseReceivedDataBytes(dataChannelReceiveArgs.dataBytes)
                    }
                }
            }
        }
    }

    fun sendDataString(data: String) {
        if (isDataChannelConnected())
            dataChannel?.sendDataString(data)
    }

    fun sendDataBytes(data: ByteArray) {
        if (isDataChannelConnected())
            dataChannel?.sendDataBytes(DataBuffer.wrap(data))
    }

    private fun parseReceivedDataString(data: String?): String {
        return data ?: emptyString()
    }

    private fun parseReceivedDataBytes(data: DataBuffer?): ByteArray {
        data?.let {
            val bytes = it.data // The payload byte[] might contain extra bytes that are not part of the payload.
            val index = data.index // Starting index of the payload’s bytes you want.
            val length = data.length // Length of the payload’s bytes you want.
            return bytes.copyOfRange(index, index+length-1)
        }

        return ByteArray(0)
    }

    private fun isDataChannelConnected(): Boolean {
        return dataChannel?.state == DataChannelState.Connected
    }

    fun cleanConnection() {
        client = null
        remoteChannel = null
        upstreamConnection = null
        token = null
    }
}
package uk.co.kidsloop.features.liveclass

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import fm.liveswitch.*
import uk.co.kidsloop.data.enums.DataChannelActionsType
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import uk.co.kidsloop.liveswitch.DataChannelActionsHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveClassManager @Inject constructor(private val moshi:Moshi) {

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

    private val  dataChannelAdapter:JsonAdapter<uk.co.kidsloop.data.enums.DataChannel>

    init {
        setUpstreamDataChannel()
        dataChannelAdapter = moshi.adapter(uk.co.kidsloop.data.enums.DataChannel::class.java)
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

    fun removeDownStreamConnection(clientId: String) {
        downstreamConnectionsMap.remove(clientId)
    }

    fun setUpstreamConnection(upstreamConnection: SfuUpstreamConnection) {
        this.upstreamConnection = upstreamConnection
    }

    fun getUpstreamConnection(): SfuUpstreamConnection? {
        return upstreamConnection
    }

    fun getUpstreamClientId(): String? {
        return upstreamConnection?.clientId
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

    private fun parseReceivedDataString(data: String?) {
        data?.let {
            val dataChannel = dataChannelAdapter.fromJson(data)
            handleReceivedDataString(dataChannel)
        }
    }

    private fun handleReceivedDataString(dataChannel:uk.co.kidsloop.data.enums.DataChannel?) {
        when (dataChannel?.eventType) {
            DataChannelActionsType.RAISE_HAND -> dataChannelActionsHandler?.onRaiseHand(dataChannel.clientId)
            DataChannelActionsType.LOWER_HAND -> dataChannelActionsHandler?.onLowerHand(dataChannel.clientId)
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
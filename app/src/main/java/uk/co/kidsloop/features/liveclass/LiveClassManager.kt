package uk.co.kidsloop.features.liveclass

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import fm.liveswitch.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject
import javax.inject.Singleton
import uk.co.kidsloop.data.enums.DataChannelActionsType
import uk.co.kidsloop.data.enums.KidsLoopDataChannel
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import uk.co.kidsloop.liveswitch.DataChannelActionsHandler

@Singleton
class LiveClassManager @Inject constructor(private val moshi: Moshi) {

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

    private val dataChannelAdapter: JsonAdapter<KidsLoopDataChannel>

    init {
        setUpstreamDataChannel()
        dataChannelAdapter = moshi.adapter(KidsLoopDataChannel::class.java)
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

    fun getUpstreamClientId(): String? {
        return upstreamConnection?.clientId
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

    private fun handleReceivedDataString(dataChannel: KidsLoopDataChannel?) {
        when (dataChannel?.eventType) {
            DataChannelActionsType.RAISE_HAND -> dataChannelActionsHandler?.onRaiseHand(dataChannel.clientId)
            DataChannelActionsType.LOWER_HAND -> dataChannelActionsHandler?.onLowerHand(dataChannel.clientId)
            DataChannelActionsType.ENABLE_VIDEO -> {
                setState(LiveClassState.CAMERA_ENABLED_BY_TEACHER)
                dataChannelActionsHandler?.onVideoEnabled()
            }
            DataChannelActionsType.DISABLE_VIDEO -> {
                setState(LiveClassState.CAMERA_DISABLED_BY_TEACHER)
                dataChannelActionsHandler?.onVideoDisabled(getState())
            }
            DataChannelActionsType.ENABLE_AUDIO -> {
                setState(LiveClassState.MIC_ENABLED_BY_TEACHER)
                dataChannelActionsHandler?.onEnableMic()
            }
            DataChannelActionsType.DISABLE_AUDIO -> {
                setState(LiveClassState.MIC_DISABLED_BY_TEACHER)
                dataChannelActionsHandler?.onDisableMic(getState())
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
        clearNetworkQualityArray()
        liveClassState = LiveClassState.IDLE
    }

    fun setState(newState: LiveClassState) {
        if (newState == LiveClassState.CAMERA_DISABLED_BY_TEACHER && this.liveClassState == LiveClassState.MIC_DISABLED_BY_TEACHER) {
            this.liveClassState = LiveClassState.MIC_AND_CAMERA_DISABLED
        } else if (newState == LiveClassState.MIC_DISABLED_BY_TEACHER && this.liveClassState == LiveClassState.CAMERA_DISABLED_BY_TEACHER) {
            this.liveClassState = LiveClassState.MIC_AND_CAMERA_DISABLED
        } else {
            this.liveClassState = newState
        }
    }

    fun getState(): LiveClassState {
        return liveClassState
    }
}

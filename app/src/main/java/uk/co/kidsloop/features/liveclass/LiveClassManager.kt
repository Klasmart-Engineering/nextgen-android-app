package uk.co.kidsloop.features.liveclass

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import fm.liveswitch.* // ktlint-disable no-wildcard-imports
import uk.co.kidsloop.data.enums.DataChannelActionsType
import uk.co.kidsloop.data.enums.KidsLoopDataChannel
import uk.co.kidsloop.features.liveclass.state.LiveClassState
import uk.co.kidsloop.liveswitch.Config
import uk.co.kidsloop.liveswitch.DataChannelActionsHandler
import javax.inject.Inject
import javax.inject.Singleton

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

    private val downstreamConnectionsMap = mutableMapOf<String, SfuDownstreamConnection?>()

    private var token: String? = null
    private var remoteChannel: Channel? = null
    private var client: Client? = null

    var dataChannelActionsHandler: DataChannelActionsHandler? = null
    private var liveClassState: LiveClassState = LiveClassState.IDLE

    private val dataChannelAdapter: JsonAdapter<KidsLoopDataChannel> = moshi.adapter(KidsLoopDataChannel::class.java)

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

    fun saveDownStreamConnection(clientId: String, connection: SfuDownstreamConnection?) {
        downstreamConnectionsMap[clientId] = connection
    }

    fun removeDownStreamConnection(clientId: String) {
        downstreamConnectionsMap.remove(clientId)
    }

    fun getDownStreamConnections(): Map<String, SfuDownstreamConnection?> {
        return downstreamConnectionsMap.toMap()
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

    fun setUpstreamDataStream(dataStream: DataStream) {
        upstreamDataStream = dataStream
    }

    fun setUpstreamDataChannel(dataChannel: DataChannel) {
        upstreamDataChannel = dataChannel
    }

    fun isTeacherPresent(): Boolean {
        return getDownStreamConnections().values.firstOrNull {
            it?.remoteConnectionInfo?.clientRoles?.get(0) == Config.TEACHER_ROLE
        } == null
    }

    fun sendDataString(data: String): Future<Any>? {
        if (isUpstreamDataChannelConnected()) {
            return upstreamDataChannel?.sendDataString(data)
        }
        return null
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
                setState(LiveClassState.CAM_ENABLED_BY_TEACHER)
                dataChannelActionsHandler?.onVideoEnabled()
            }
            DataChannelActionsType.DISABLE_VIDEO -> {
                setState(LiveClassState.CAM_DISABLED_BY_TEACHER)
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
            DataChannelActionsType.END_LIVE_CLASS -> {
                setState(LiveClassState.TEACHER_ENDED_LIVE_CLASS)
                dataChannelActionsHandler?.onLiveClassEnding()
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
        if (newState == LiveClassState.CAM_DISABLED_BY_TEACHER && this.liveClassState == LiveClassState.MIC_DISABLED_BY_TEACHER) {
            this.liveClassState = LiveClassState.MIC_AND_CAMERA_DISABLED
        } else if (newState == LiveClassState.MIC_DISABLED_BY_TEACHER && this.liveClassState == LiveClassState.CAM_DISABLED_BY_TEACHER) {
            this.liveClassState = LiveClassState.MIC_AND_CAMERA_DISABLED
        } else if (newState == LiveClassState.MIC_ENABLED_BY_TEACHER && this.liveClassState == LiveClassState.MIC_AND_CAMERA_DISABLED) {
            this.liveClassState = LiveClassState.CAM_DISABLED_BY_TEACHER
        } else if (newState == LiveClassState.CAM_ENABLED_BY_TEACHER && this.liveClassState == LiveClassState.MIC_AND_CAMERA_DISABLED) {
            this.liveClassState = LiveClassState.MIC_DISABLED_BY_TEACHER
        } else {
            this.liveClassState = newState
        }
    }

    fun getState(): LiveClassState {
        return liveClassState
    }
}

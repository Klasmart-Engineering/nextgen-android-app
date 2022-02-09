package uk.co.kidsloop.data.enums

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DataChannel(val clientId:String?, val eventType: DataChannelActionsType)
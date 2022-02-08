package uk.co.kidsloop.features.liveclass.datachannel

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RaiseHand(val showRaisedHand:Boolean, val showLowHand:Boolean)

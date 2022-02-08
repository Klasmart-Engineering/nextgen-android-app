package uk.co.kidsloop.features.liveclass.datachannel

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ToggleStudentsVideo(val status: Boolean, val name: String = "VideoNotification")

package uk.co.kidsloop.features.schedule.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SwitchRequest(@Json(name = "user_id") val userId: String)

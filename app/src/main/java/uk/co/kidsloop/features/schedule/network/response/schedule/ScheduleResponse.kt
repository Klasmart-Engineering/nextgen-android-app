package uk.co.kidsloop.features.schedule.network.response.schedule

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScheduleResponse(
    val data: List<Data>,
    val total: Int
)

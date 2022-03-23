package uk.co.kidsloop.features.schedule.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScheduleRequest(
    @Json(name = "view_type") val viewType: String = "full_view",
    @Json(name = "time_at") val timeAt: Int = 0,
    @Json(name = "start_at_ge") val startAt: Long = 1647900000,
    @Json(name = "order_by") val orderBy: String = "start_at",
    @Json(name = "time_boundary") val timeBoundary: String = "union"
)

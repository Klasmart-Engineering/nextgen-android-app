package uk.co.kidsloop.features.schedule.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import uk.co.kidsloop.features.schedule.network.response.schedule.ScheduleResponse

interface SchedulesApi {

    companion object {

        const val SCHEDULES_BASE_URL = "https://cms.alpha.kidsloop.net/v1/"
    }

    @Headers("Content-Type:application/json; charset=UTF-8", "Accept:text/plain")
    @POST("schedules_time_view/list")
    suspend fun schedule(
        @Header("cookie") authToken: String,
        @Query("org_id") orgId: String,
        @Body request: ScheduleRequest
    ): Response<ScheduleResponse>
}

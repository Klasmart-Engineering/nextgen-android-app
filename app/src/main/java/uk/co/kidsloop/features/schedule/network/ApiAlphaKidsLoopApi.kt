package uk.co.kidsloop.features.schedule.network

import uk.co.kidsloop.features.schedule.network.response.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import uk.co.kidsloop.features.schedule.network.requests.UserRequest

interface ApiAlphaKidsLoopApi {

    @Headers("Content-Type:application/json; charset=UTF-8", "Accept:text/plain")
    @POST("/user/")
    suspend fun user(@Header("cookie") authToken: String, @Body body: UserRequest): Response<User>
}

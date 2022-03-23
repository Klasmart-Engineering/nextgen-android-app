package uk.co.kidsloop.app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import uk.co.kidsloop.features.schedule.network.SwitchRequest

interface AuthAlphaKidsLoopApi {

    companion object {

        const val TRANSFER_API_BASE_URL = "https://auth.alpha.kidsloop.net"
    }

    @Headers("Content-Type:application/json; charset=UTF-8", "Accept:text/plain")
    @POST("/transfer")
    suspend fun transferToken(@Header("Authorization") authToken: String): Response<Void>

    @Headers("Content-Type:application/json; charset=UTF-8", "Accept:text/plain")
    @POST("/switch")
    suspend fun switch(@Header("cookie") authToken: String, @Body body: SwitchRequest): Response<Void>
}

package uk.co.kidsloop.app.network

import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ProfilesApi {

    companion object {

        const val PROFILES_BASE_URL = "https://api.alpha.kidsloop.net/user/"
    }

    @Headers("Content-Type:application/json; charset=UTF-8", "Accept:*/*")
    @POST("/user/")
    suspend fun fetchProfiles(@Header("Cookie") token: String, @Body json: JSONObject): Response<Void>
}

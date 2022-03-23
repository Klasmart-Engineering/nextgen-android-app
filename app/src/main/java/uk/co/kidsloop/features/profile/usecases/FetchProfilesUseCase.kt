package uk.co.kidsloop.features.profile.usecases

import android.text.TextUtils
import com.apollographql.apollo3.ApolloClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.kidsloop.ProfilesQuery
import uk.co.kidsloop.app.network.AuthAlphaKidsLoopApi
import uk.co.kidsloop.data.enums.SharedPrefsWrapper
import uk.co.kidsloop.features.authentication.AuthenticationManager
import javax.inject.Inject

class FetchProfilesUseCase @Inject constructor(
    private val authManager: AuthenticationManager,
    private val sharedPrefsWrapper: SharedPrefsWrapper,
    private val kidsloopApi: AuthAlphaKidsLoopApi,
    private val apolloClient: ApolloClient
) {

    sealed class ProfilesResult {
        data class Success(val myUser: ProfilesQuery.MyUser) : ProfilesResult()
        object Failure : ProfilesResult()
    }

    suspend fun fetchProfiles(): ProfilesResult {
        return withContext(Dispatchers.IO) {
            val authToken = authManager.getAccessToken1()
            val bearerToken = "Bearer $authToken"
            if (!authToken.isNullOrEmpty()) {
                val response = kidsloopApi.transferToken(bearerToken)
                if (response.isSuccessful) {
                    val accessToken = response.headers().toMultimap()["set-cookie"]?.get(0)
                    val refreshToken = response.headers().toMultimap()["set-cookie"]?.get(1)
                    if (!TextUtils.isEmpty(accessToken)) {
                        val accessToken2 = accessToken!!.split(";")[0]
                        sharedPrefsWrapper.saveAccessToken2(accessToken2)
                        val profiles = apolloClient.query(ProfilesQuery()).execute().data
                        profiles?.myUser?.let {
                            return@withContext ProfilesResult.Success(it)
                        }
                    }
                }
            }
            return@withContext ProfilesResult.Failure
        }
    }
}

package uk.co.kidsloop.features.schedule.usecases

import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.kidsloop.app.network.AuthAlphaKidsLoopApi
import uk.co.kidsloop.data.enums.SharedPrefsWrapper
import uk.co.kidsloop.features.schedule.network.ApiAlphaKidsLoopApi
import uk.co.kidsloop.features.schedule.network.ScheduleRequest
import uk.co.kidsloop.features.schedule.network.SchedulesApi
import uk.co.kidsloop.features.schedule.network.SwitchRequest
import uk.co.kidsloop.features.schedule.network.requests.UserRequest
import uk.co.kidsloop.features.schedule.network.response.User

class FetchScheduleUseCase @Inject constructor(
    private val mapper: ScheduleEntityMapper,
    private val sharedPrefsWrapper: SharedPrefsWrapper,
    private val kidsloopApi: AuthAlphaKidsLoopApi,
    private val usersApi: ApiAlphaKidsLoopApi,
    private val schedulesApi: SchedulesApi
) {

    sealed class ScheduleResult {
        data class Success(val entity: ScheduleEntity) : ScheduleResult()
        object Failure : ScheduleResult()
    }

    suspend fun fetchSchedule(userId: String): ScheduleResult {
        return withContext(Dispatchers.IO) {
            val accessToken2 = sharedPrefsWrapper.getAccessToken2()
            val request = SwitchRequest(userId)
            val response = kidsloopApi.switch(accessToken2, request)
            if (response.isSuccessful) {
                val accessToken = response.headers().toMultimap()["set-cookie"]?.get(0)
                val refreshToken = response.headers().toMultimap()["set-cookie"]?.get(1)

                val userResponse = usersApi.user(accessToken!!, UserRequest())
                if (userResponse.isSuccessful) {
                    val user: User? = userResponse.body()
                    user?.let {
                        val orgId = it.data.myUser.node.organizationMembershipsConnection.edges[0].node.organization.id
                        val response = schedulesApi.schedule(accessToken, orgId, ScheduleRequest())
                        if (response.isSuccessful) {
                            val scheduleResponse = response.body()
                            scheduleResponse?.let {
                                return@withContext ScheduleResult.Success(mapper.toScheduleEntity(scheduleResponse))
                            }
                        }
                    }
                }
            }
            return@withContext ScheduleResult.Failure
        }
    }
}

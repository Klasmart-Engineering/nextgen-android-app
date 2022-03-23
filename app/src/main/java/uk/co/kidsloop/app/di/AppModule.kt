package uk.co.kidsloop.app.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.network.okHttpClient
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import uk.co.kidsloop.app.network.AuthAlphaKidsLoopApi
import uk.co.kidsloop.app.network.NetworkConstants
import uk.co.kidsloop.data.enums.SharedPrefsWrapper
import uk.co.kidsloop.features.schedule.network.ApiAlphaKidsLoopApi
import uk.co.kidsloop.features.schedule.network.SchedulesApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TransferApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ScheduleApi

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun providesApplication(app: Application): Context = app

    @Provides
    fun providesResources(app: Application): Resources = app.resources

    @Provides
    fun providesSharedPreferences(app: Application): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(app)

    @Provides
    fun providesSharedPrefsWrapper(sharedPreferences: SharedPreferences): SharedPrefsWrapper {
        return SharedPrefsWrapper(sharedPreferences)
    }

    @Provides
    fun providesMoshiConverterFactory(): MoshiConverterFactory = MoshiConverterFactory.create()

    @Provides
    fun providesMoshi(): Moshi {
        return Moshi.Builder()
            .build()
    }

    @Provides
    fun providesOkHttpClient(): OkHttpClient {
        val timeout = 10L
        val timeUnit = TimeUnit.SECONDS
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient().newBuilder()
            .apply {
                connectTimeout(timeout, timeUnit)
                callTimeout(timeout, timeUnit)
                readTimeout(timeout, timeUnit)
                writeTimeout(timeout, timeUnit)
                addInterceptor(logging)
            }
        return client.build()
    }

    @Provides
    @TransferApi
    fun providesTransferRetrofit(
        moshiConverterFactory: MoshiConverterFactory,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AuthAlphaKidsLoopApi.TRANSFER_API_BASE_URL)
            .addConverterFactory(moshiConverterFactory)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @UserApi
    fun providesUsersRetrofit(
        moshiConverterFactory: MoshiConverterFactory,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConstants.USER_BASE_URL)
            .addConverterFactory(moshiConverterFactory)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @ScheduleApi
    fun providesSchedulesRetrofit(
        moshiConverterFactory: MoshiConverterFactory,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SchedulesApi.SCHEDULES_BASE_URL)
            .addConverterFactory(moshiConverterFactory)
            .client(okHttpClient)
            .build()
    }

    @Provides
    fun providesApolloClient(
        okHttpClient: OkHttpClient,
        sharedPrefsWrapper: SharedPrefsWrapper
    ): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl(NetworkConstants.PROFILES_BASE_URL)
            .okHttpClient(okHttpClient)
            .enableAutoPersistedQueries(true)
            .addInterceptor(object : ApolloInterceptor {
                override fun <D : Operation.Data> intercept(
                    request: ApolloRequest<D>,
                    chain: ApolloInterceptorChain
                ): Flow<ApolloResponse<D>> {
                    val newRequest =
                        request.newBuilder().addHttpHeader("cookie", sharedPrefsWrapper.getAccessToken2()).build()
                    return chain.proceed(newRequest)
                }
            })
            .build()
    }

    @Provides
    fun providesTransferApi(@TransferApi retrofit: Retrofit): AuthAlphaKidsLoopApi {
        return retrofit.create(AuthAlphaKidsLoopApi::class.java)
    }

    @Provides
    fun providesUserApi(@UserApi retrofit: Retrofit): ApiAlphaKidsLoopApi {
        return retrofit.create(ApiAlphaKidsLoopApi::class.java)
    }

    @Provides
    fun providesSchedulesApi(@ScheduleApi retrofit: Retrofit): SchedulesApi {
        return retrofit.create(SchedulesApi::class.java)
    }
}

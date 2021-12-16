package uk.co.kidsloop.app.di.app

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

@Module
class AppModule(private val application:Application) {

    @Provides
    fun providesApplication(): Application = application

    @Provides
    fun providesResources(app: Application): Resources = app.resources

    @Provides
    @AppScope
    fun providesSharedPref(app: Application): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)

    @Provides
    @AppScope
    fun providesMoshiConverterFactory(): MoshiConverterFactory = MoshiConverterFactory.create()

    @Provides
    @AppScope
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
    @AppScope
    fun providesRetrofit(
        moshiConverterFactory: MoshiConverterFactory,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(moshiConverterFactory)
            .client(okHttpClient)
            .build()
    }
}
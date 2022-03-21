package uk.co.kidsloop.app.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uk.co.kidsloop.features.connectivity.NetworkStatusTracker

/**
 *  Created by paulbisioc on 21.03.2022
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkStatusModule {
    @Provides
    fun provideNetworkStatusTracker(app: Application): NetworkStatusTracker = NetworkStatusTracker(app.applicationContext)
}
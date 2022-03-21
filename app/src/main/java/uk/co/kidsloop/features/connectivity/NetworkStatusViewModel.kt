package uk.co.kidsloop.features.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

/**
 *  Created by paulbisioc on 21.03.2022
 */
@HiltViewModel
class NetworkStatusViewModel @Inject constructor(networkStatusTracker: NetworkStatusTracker) : ViewModel() {

    @ExperimentalCoroutinesApi
    val state = networkStatusTracker.networkStatus.map(
        onWiFi = { NetworkFetchState.FETCHED_WIFI },
        onMobileData = { NetworkFetchState.FETCHED_MOBILE_DATA },
        onUnavailable = { NetworkFetchState.ERROR },
    ).asLiveData(Dispatchers.IO)
}
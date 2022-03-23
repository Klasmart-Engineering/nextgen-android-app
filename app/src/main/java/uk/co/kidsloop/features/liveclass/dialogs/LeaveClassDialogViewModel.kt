package uk.co.kidsloop.features.liveclass.dialogs

import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import uk.co.kidsloop.features.connectivity.NetworkFetchState
import uk.co.kidsloop.features.connectivity.NetworkStatusTracker
import uk.co.kidsloop.features.connectivity.map
import javax.inject.Inject
import androidx.lifecycle.*

/**
 *  Created by paulbisioc on 23.03.2022
 */
@HiltViewModel
class LeaveClassDialogViewModel @Inject constructor(
    private val networkStatusTracker: NetworkStatusTracker
) : ViewModel() {
    @ExperimentalCoroutinesApi
    val networkState = networkStatusTracker.networkStatus.map(
        onWiFi = { NetworkFetchState.FETCHED_WIFI },
        onMobileData = { NetworkFetchState.FETCHED_MOBILE_DATA },
        onUnavailable = { NetworkFetchState.ERROR }
    ).asLiveData(Dispatchers.IO)
}
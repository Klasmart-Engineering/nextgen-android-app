package uk.co.kidsloop.features.profile.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import uk.co.kidsloop.ProfilesQuery
import uk.co.kidsloop.features.profile.usecases.FetchProfilesUseCase
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(private val fetchProfile: FetchProfilesUseCase) : ViewModel() {

    private var _profilesLiveData = MutableLiveData<ProfilesUiState>()
    val profilesLiveData: LiveData<ProfilesUiState> get() = _profilesLiveData

    sealed class ProfilesUiState {
        data class Success(val profiles: List<ProfilesQuery.Profile>) : ProfilesUiState()
        object Failure : ProfilesUiState()
    }

    init {
        viewModelScope.launch {
            val result = fetchProfile.fetchProfiles()
            when (result) {
                is FetchProfilesUseCase.ProfilesResult.Success ->
                    _profilesLiveData.value =
                        ProfilesUiState.Success(result.myUser.profiles) // ktlint-disable max-line-length
                is FetchProfilesUseCase.ProfilesResult.Failure -> _profilesLiveData.value = ProfilesUiState.Failure
            }
        }
    }
}

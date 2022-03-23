package uk.co.kidsloop.features.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import uk.co.kidsloop.features.schedule.usecases.DataEntity
import uk.co.kidsloop.features.schedule.usecases.FetchScheduleUseCase
import javax.inject.Inject

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val scheduleUseCase: FetchScheduleUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var _schedulesLiveData = MutableLiveData<SchedulesUiState>()
    val schedulesLiveData: LiveData<SchedulesUiState> get() = _schedulesLiveData

    private val userId = savedStateHandle.get<String>("userId")

    sealed class SchedulesUiState {
        data class Success(val scheduleEntity: List<DataEntity>) : SchedulesUiState()
        object Failure : SchedulesUiState()
    }

    init {
        viewModelScope.launch {
            if (userId != null) {
                when (val scheduleResult = scheduleUseCase.fetchSchedule(userId)) {
                    is FetchScheduleUseCase.ScheduleResult.Success ->
                        _schedulesLiveData.value =
                            SchedulesUiState.Success(scheduleResult.entity.data)
                    is FetchScheduleUseCase.ScheduleResult.Failure ->
                        _schedulesLiveData.value =
                            SchedulesUiState.Failure
                }
            }
        }
    }
}

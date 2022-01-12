package uk.co.kidsloop.features.preview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fm.liveswitch.IAction1
import uk.co.kidsloop.features.liveclass.JoinLiveClassUseCase
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(private val joinLiveClassUseCase: JoinLiveClassUseCase) : ViewModel() {

    private var _joinClassroomStateLiveData = MutableLiveData<JoinStatus>()
    val joinClassroomStateLiveData: LiveData<JoinStatus> get() = _joinClassroomStateLiveData

    sealed class JoinStatus {
        object SuccessJoiningClassRoom : JoinStatus()
        data class FailureJoiningClassRoom(val message: String?) : JoinStatus()
        object WaitingToJoinClassRoom : JoinStatus()
    }

    fun joinLiveClass() {
        joinLiveClassUseCase.joinAsync().then { resultJoin ->
            _joinClassroomStateLiveData.postValue(JoinStatus.SuccessJoiningClassRoom)
        }.fail(IAction1<Exception> { exception ->
            _joinClassroomStateLiveData.postValue(JoinStatus.FailureJoiningClassRoom(exception.message))
        })
    }
}
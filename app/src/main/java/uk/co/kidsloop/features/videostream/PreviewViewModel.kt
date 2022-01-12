package uk.co.kidsloop.features.videostream

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class PreviewViewModel @Inject constructor() : ViewModel() {
    var isCameraGranted: Boolean = false
    var isMicGranted: Boolean = false

    private var _isChecked = MutableLiveData<Boolean>()
    val isChecked: LiveData<Boolean> get() = _isChecked
    val delay = 5000 // 5000 milliseconds == 5 second

    init {
        _isChecked.value = true
    }

    fun onChange() {
        _isChecked.value = false
        Handler(Looper.getMainLooper()).postDelayed({ _isChecked.postValue(true) }, delay.toLong())
    }
}
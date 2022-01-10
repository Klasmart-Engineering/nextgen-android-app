package uk.co.kidsloop.features.videostream

import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class PreviewViewModel @Inject constructor() : ViewModel() {
    private var _isChecked = MutableLiveData<Boolean>()
    val isChecked: LiveData<Boolean> get() = _isChecked

    val handler: Handler = Handler()
    val delay = 5000 // 5000 milliseconds == 5 second

    init {
        _isChecked.value = true
    }

    fun onChange() {
        _isChecked.value = false
        handler.postDelayed(object : Runnable {
            override fun run() {
                _isChecked.postValue(true)
                handler.postDelayed(this, delay.toLong())
            }
        }, delay.toLong())
    }
}
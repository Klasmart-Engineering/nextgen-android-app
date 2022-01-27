package uk.co.kidsloop.app

import android.os.Handler
import android.os.Looper
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

@FragmentScoped
class UiThreadPoster @Inject constructor() {

    private val uiHandler: Handler = Handler(Looper.getMainLooper())

    fun post(runnable: Runnable) {
        uiHandler.post(runnable)
    }
}
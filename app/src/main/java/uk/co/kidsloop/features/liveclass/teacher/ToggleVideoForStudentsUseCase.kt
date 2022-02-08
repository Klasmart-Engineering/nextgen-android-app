package uk.co.kidsloop.features.liveclass.teacher

import com.squareup.moshi.Moshi
import uk.co.kidsloop.features.liveclass.LiveClassManager
import uk.co.kidsloop.features.liveclass.datachannel.ToggleStudentsVideo
import javax.inject.Inject

class ToggleVideoForStudentsUseCase @Inject constructor(private val liveClassManager: LiveClassManager, private val moshi: Moshi) {

    fun toggleVideo(shouldTurnOff: Boolean) {
        val jsonAdapter = moshi.adapter<ToggleStudentsVideo>(ToggleStudentsVideo::class.java)
        val json = jsonAdapter.toJson(ToggleStudentsVideo(shouldTurnOff))
        liveClassManager.sendDataString(json.toString())
    }
}
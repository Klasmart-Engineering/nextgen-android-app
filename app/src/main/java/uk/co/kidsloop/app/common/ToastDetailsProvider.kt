package uk.co.kidsloop.app.common

import android.content.res.Resources
import javax.inject.Inject
import uk.co.kidsloop.R
import uk.co.kidsloop.features.liveclass.LiveClassManager
import uk.co.kidsloop.features.liveclass.state.LiveClassState

class ToastDetailsProvider @Inject constructor(
    private val liveClassManager: LiveClassManager,
    private val resources: Resources
) {

    companion object {
        private const val NO_MESSAGE_TO_BE_DISPLAYED: Int = -1
    }

    fun getToastDetails(): ToastDetails? {
        val state = liveClassManager.getState()
        if (state == LiveClassState.MIC_DISABLED_BY_TEACHER) {
            return ToastDetails(resources.getString(R.string.teacher_turned_off_all_microphones), false, true)
        } else if (state == LiveClassState.MIC_AND_CAMERA_DISABLED) {
            return ToastDetails(resources.getString(R.string.teacher_turned_off_all_students_cam_and_mic), true, true)
        } else if (state == LiveClassState.CAM_DISABLED_BY_TEACHER) {
            return ToastDetails(resources.getString(R.string.teacher_turned_off_all_cameras), true, false)
        }
        return null
    }

    fun getMessageOnMicClicked(): String? {
        val messageId = when (liveClassManager.getState()) {
            LiveClassState.JOINED_AND_WAITING_FOR_TEACHER -> R.string.wait_for_teacher_to_arrive
            LiveClassState.TEACHER_DISCONNECTED -> R.string.teacher_has_left_the_classroom
            LiveClassState.TEACHER_ENDED_LIVE_CLASS -> NO_MESSAGE_TO_BE_DISPLAYED
            LiveClassState.IDLE -> NO_MESSAGE_TO_BE_DISPLAYED
            else -> R.string.teacher_turned_off_all_microphones
        }
        if (messageId != NO_MESSAGE_TO_BE_DISPLAYED) {
            return resources.getString(messageId)
        }
        return null
    }

    fun getMessageOnCamClicked(): String? {
        val messageId = when (liveClassManager.getState()) {
            LiveClassState.JOINED_AND_WAITING_FOR_TEACHER -> R.string.wait_for_teacher_to_arrive
            LiveClassState.TEACHER_DISCONNECTED -> R.string.teacher_has_left_the_classroom
            LiveClassState.TEACHER_ENDED_LIVE_CLASS -> NO_MESSAGE_TO_BE_DISPLAYED
            LiveClassState.IDLE -> NO_MESSAGE_TO_BE_DISPLAYED
            else -> R.string.teacher_turned_off_all_cameras
        }
        if (messageId != NO_MESSAGE_TO_BE_DISPLAYED) {
            return resources.getString(messageId)
        }
        return null
    }
}

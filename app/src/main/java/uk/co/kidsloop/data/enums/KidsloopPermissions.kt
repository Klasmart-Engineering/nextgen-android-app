package uk.co.kidsloop.data.enums

import android.Manifest

/**
 *  Created by paulbisioc on 07.01.2022
 */
enum class KidsloopPermissions(val type: String) {
    CAMERA(Manifest.permission.CAMERA),
    RECORD_AUDIO(Manifest.permission.RECORD_AUDIO);

    companion object {
        fun getPreviewPermissions(): Array<String> {
            return arrayOf(CAMERA.type, RECORD_AUDIO.type)
        }
    }
}


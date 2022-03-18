package uk.co.kidsloop.features.liveclass.enums

/**
 *  Created by paulbisioc on 17.03.2022
 */
enum class MicStatus(val type: String) {
    INITIAL("mic_initial_status"),
    MUTED("mic_muted_status"),
    ON("mic_on_status"),
    DISABLED("mic_disabled_status")
}
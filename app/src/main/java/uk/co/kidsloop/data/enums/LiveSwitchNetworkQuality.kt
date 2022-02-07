package uk.co.kidsloop.data.enums

/**
 *  Created by paulbisioc on 07.02.2022
 */
enum class LiveSwitchNetworkQuality(val lowerLimit: Double, val upperLimit: Double) {
    MODERATE(0.3, 0.499),
    GOOD(0.5, 1.0)
}

enum class Bitrates(val type: String, val value: Int) {
    P480("480p", 2000),
    P720("720p", 3000)
}

enum class TeacherFeedQuality(val type: String, val bitrate: Int) {
    MODERATE("teacher_moderate", Bitrates.P480.value),
    GOOD("teacher_high", Bitrates.P720.value)
}
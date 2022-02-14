package uk.co.kidsloop.data.enums

/**
 *  Created by paulbisioc on 07.02.2022
 */
enum class LiveSwitchNetworkQuality(val lowerLimit: Double, val upperLimit: Double) {
    MODERATE(0.3, 0.499),
    GOOD(0.5, 1.0)
}

enum class Bitrates(val type: String, val videoBitrate: Int, val audioBitrate: Int) {
    P320("320p", 128,32),
    P480("480p", 256, 32),
    P720("720p", 512, 64)
}

enum class TeacherFeedQuality(val type: String, val videoBitrate: Int, val audioBitrate: Int) {
    MODERATE("teacher_moderate", Bitrates.P480.videoBitrate, Bitrates.P480.audioBitrate),
    GOOD("teacher_good", Bitrates.P720.videoBitrate, Bitrates.P720.audioBitrate)
}

enum class StudentFeedQuality(val type: String, val videoBitrate: Int, val audioBitrate: Int) {
    MODERATE("student_moderate", Bitrates.P320.videoBitrate, Bitrates.P320.audioBitrate),
    GOOD("student_good", Bitrates.P480.videoBitrate, Bitrates.P480.audioBitrate)
}

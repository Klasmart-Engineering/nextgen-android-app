package uk.co.kidsloop.data.enums

/**
 *  Created by paulbisioc on 07.02.2022
 */
enum class LiveSwitchNetworkQuality(val lowerLimit: Double, val upperLimit: Double) {
    MODERATE(0.3, 0.499),
    GOOD(0.5, 1.0)
}

enum class VideoQuality(val type: String, val videoBitrate: Int, val frameRate: Double, val scale: Double) {
    P320("320p", 128, 7.5, 0.25),
    P480("480p", 256, 15.0, 0.5),
    P720("720p", 512, 30.0, 1.0)
}

enum class TeacherFeedQuality(val type: String, val videoBitrate: Int) {
    MODERATE("teacher_moderate", VideoQuality.P480.videoBitrate),
    GOOD("teacher_good", VideoQuality.P720.videoBitrate)
}

enum class StudentFeedQuality(val type: String, val videoBitrate: Int) {
    MODERATE("student_moderate", VideoQuality.P320.videoBitrate),
    GOOD("student_good", VideoQuality.P480.videoBitrate)
}

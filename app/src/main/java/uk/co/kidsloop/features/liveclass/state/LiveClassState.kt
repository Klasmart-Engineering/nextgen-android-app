package uk.co.kidsloop.features.liveclass.state

enum class LiveClassState {
    IDLE,
    REGISTERED,
    JOINED,
    CAMERA_DISABLED_BY_TEACHER,
    CAMERA_ENABLED_BY_TEACHER,
    MIC_DISABLED_BY_TEACHER,
    MIC_ENABLED_BY_TEACHER,
    MIC_AND_CAMERA_DISABLED
}

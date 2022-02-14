package uk.co.kidsloop.features.liveclass.state

enum class LiveClassState {
    IDLE,
    REGISTERED,
    JOINED,
    CAM_DISABLED_BY_TEACHER,
    CAM_ENABLED_BY_TEACHER,
    MIC_DISABLED_BY_TEACHER,
    MIC_ENABLED_BY_TEACHER,
    MIC_AND_CAMERA_DISABLED
}

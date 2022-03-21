package uk.co.kidsloop.features.schedule

data class Class(
    val id: String,
    val classTitle: String,
    val startAt: Long,
    val endAt: Long,
    val classType: String,
    val classStatus: String,
    val teacherName: String
)

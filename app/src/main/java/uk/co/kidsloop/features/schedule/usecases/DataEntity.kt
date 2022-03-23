package uk.co.kidsloop.features.schedule.usecases

data class DataEntity(
    val id: String,
    val startAt: Int,
    val endAt: Int,
    val status: String,
    val title: String,
    val classType: String
)

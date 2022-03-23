package uk.co.kidsloop.features.schedule.network.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Branding(
    val __typename: String,
    val iconImageURL: Any?,
    val primaryColor: Any?
)

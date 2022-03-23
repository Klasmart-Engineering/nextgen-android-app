package uk.co.kidsloop.features.schedule.network.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Profile(
    val __typename: String,
    val avatar: Any?,
    val contactInfo: ContactInfoXX,
    val familyName: String,
    val givenName: String,
    val id: String
)

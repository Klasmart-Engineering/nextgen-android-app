package uk.co.kidsloop.features.schedule.network.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContactInfoX(
    val __typename: String,
    val phone: String
)

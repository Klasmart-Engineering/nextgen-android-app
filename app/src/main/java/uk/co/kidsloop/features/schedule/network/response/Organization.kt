package uk.co.kidsloop.features.schedule.network.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Organization(
    val __typename: String,
    val branding: Branding,
    val contactInfo: ContactInfoX,
    val id: String,
    val name: String,
    val owners: List<Owner>
)

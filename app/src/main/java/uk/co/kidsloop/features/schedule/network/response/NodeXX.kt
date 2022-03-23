package uk.co.kidsloop.features.schedule.network.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NodeXX(
    val __typename: String,
    val id: String,
    val name: String
)

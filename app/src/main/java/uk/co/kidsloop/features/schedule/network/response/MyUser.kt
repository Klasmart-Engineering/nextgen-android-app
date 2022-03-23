package uk.co.kidsloop.features.schedule.network.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MyUser(
    val __typename: String,
    val node: Node,
    val profiles: List<Profile>
)

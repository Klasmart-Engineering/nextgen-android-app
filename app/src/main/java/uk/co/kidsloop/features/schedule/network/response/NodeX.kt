package uk.co.kidsloop.features.schedule.network.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NodeX(
    val __typename: String,
    val organization: Organization,
    val rolesConnection: RolesConnection
)

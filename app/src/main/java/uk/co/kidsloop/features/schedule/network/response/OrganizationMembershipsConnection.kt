package uk.co.kidsloop.features.schedule.network.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrganizationMembershipsConnection(
    val __typename: String,
    val edges: List<Edge>
)

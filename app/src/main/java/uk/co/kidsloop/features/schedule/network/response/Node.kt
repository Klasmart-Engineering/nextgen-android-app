package uk.co.kidsloop.features.schedule.network.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Node(
    val __typename: String,
    val avatar: Any?,
    val contactInfo: ContactInfo,
    val familyName: String,
    val givenName: String,
    val id: String,
    val organizationMembershipsConnection: OrganizationMembershipsConnection,
    val username: Any?
)

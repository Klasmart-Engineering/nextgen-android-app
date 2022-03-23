package uk.co.kidsloop.features.schedule.network.requests

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserRequest(val extensions: Extensions = Extensions())

@JsonClass(generateAdapter = true)
data class Extensions(val persistedQuery: PersistedQuery = PersistedQuery())

@JsonClass(generateAdapter = true)
data class PersistedQuery(
    val version: Int = 1,
    val sha256Hash: String = "ba281cb5d505066e88a710012cbb9b652e1d709eb030e056f0dff6d25e9c1c60"
)

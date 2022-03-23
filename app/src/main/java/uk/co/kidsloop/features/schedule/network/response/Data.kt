package uk.co.kidsloop.features.schedule.network.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Data(
    val myUser: MyUser
)

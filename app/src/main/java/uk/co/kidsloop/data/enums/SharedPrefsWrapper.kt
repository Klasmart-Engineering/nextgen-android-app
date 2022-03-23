package uk.co.kidsloop.data.enums

import android.content.SharedPreferences
import uk.co.kidsloop.liveswitch.Config

class SharedPrefsWrapper(private val sharedPref: SharedPreferences) {

    companion object {
        const val LIVE_CLASS_ROLE = "live_class_role"
        const val CHANNEL_ID = "channel_id"
        const val ACCOUNT_ID = "account_id"
        const val ACCESS_TOKEN2 = "access_token2"
        const val ACCESS_TOKEN1 = "access_token1"
        const val ACCESS_TOKEN3 = "access_token3"
    }

    fun saveRole(role: String) {
        sharedPref.edit().putString(LIVE_CLASS_ROLE, role).apply()
    }

    fun getRole() = sharedPref.getString(LIVE_CLASS_ROLE, "") ?: ""

    fun saveChannelID(channelID: String) {
        sharedPref.edit().putString(CHANNEL_ID, channelID).apply()
    }

    fun getChannelID() = sharedPref.getString(CHANNEL_ID, Config.CHANNEL_ID) ?: Config.CHANNEL_ID

    fun saveAccountId(accountId: String) {
        sharedPref.edit().putString(ACCOUNT_ID, accountId).apply()
    }

    fun getAccountId(): String? = sharedPref.getString(ACCOUNT_ID, "")

    fun saveAccessToken2(accessToken2: String) {
        sharedPref.edit().putString(ACCESS_TOKEN2, accessToken2).apply()
    }

    fun getAccessToken2(): String = sharedPref.getString(ACCESS_TOKEN2, "")!!

    fun saveAccessToken1(accessToken: String) {
        sharedPref.edit().putString(ACCESS_TOKEN1, accessToken).apply()
    }

    fun getAccessToken1(): String = sharedPref.getString(ACCESS_TOKEN1, "")!!

    fun saveAccessToken3(accessToken3: String) {
        sharedPref.edit().putString(ACCESS_TOKEN3, accessToken3).apply()
    }

    fun getAccessToken3(): String = sharedPref.getString(ACCESS_TOKEN3, "")!!
}

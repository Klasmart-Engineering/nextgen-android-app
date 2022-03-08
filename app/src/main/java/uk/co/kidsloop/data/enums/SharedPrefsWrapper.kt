package uk.co.kidsloop.data.enums

import android.content.SharedPreferences
import uk.co.kidsloop.liveswitch.Config

class SharedPrefsWrapper(private val sharedPref: SharedPreferences) {

    companion object {
        const val LIVE_CLASS_ROLE = "live_class_role"
        const val CHANNEL_ID = "channel_id"
        const val ACCESS_TOKEN = "access_token"
        const val SCOPE = "scope"
        const val EXPIRES_ON = "expires_on"
        const val TENANT_ID = "tenant_id"
    }

    fun saveRole(role: String) {
        sharedPref.edit().putString(LIVE_CLASS_ROLE, role).apply()
    }

    fun getRole() = sharedPref.getString(LIVE_CLASS_ROLE, "") ?: ""

    fun saveChannelID(channelID: String) {
        sharedPref.edit().putString(CHANNEL_ID, channelID).apply()
    }

    fun getChannelID() = sharedPref.getString(CHANNEL_ID, Config.CHANNEL_ID) ?: Config.CHANNEL_ID
}

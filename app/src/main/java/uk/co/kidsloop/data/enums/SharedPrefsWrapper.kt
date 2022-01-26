package uk.co.kidsloop.data.enums

import android.content.SharedPreferences
import javax.inject.Singleton

class SharedPrefsWrapper(private val sharedPref: SharedPreferences) {

    companion object {
        const val LIVE_CLASS_ROLE = "live_class_role"
    }

    fun saveRole(role: String) {
        sharedPref.edit().putString(LIVE_CLASS_ROLE, role).apply()
    }

    fun getRole() = sharedPref.getString(LIVE_CLASS_ROLE, "") ?: ""
}
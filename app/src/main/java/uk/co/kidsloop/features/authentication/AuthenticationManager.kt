package uk.co.kidsloop.features.authentication

import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import javax.inject.Inject
import javax.inject.Singleton
import uk.co.kidsloop.data.enums.SharedPrefsWrapper

@Singleton
class AuthenticationManager @Inject constructor(val sharedPrefsWrapper: SharedPrefsWrapper) {

    private lateinit var b2cClientApp: IMultipleAccountPublicClientApplication
    private var accessToken: String? = null

    fun saveB2CClientApp(clientApp: IMultipleAccountPublicClientApplication) {
        b2cClientApp = clientApp
    }

    fun getB2CClientApp(): IMultipleAccountPublicClientApplication = b2cClientApp

    fun saveAccessToken(accessToken: String?) {
        this.accessToken = accessToken
    }

    fun getAccessToken(): String? {
        return accessToken
    }

    fun saveAccountId(accountId: String) {
        sharedPrefsWrapper.saveAccountId(accountId)
    }

    fun isNotAuthenticated(): Boolean = sharedPrefsWrapper.getAccountId().isNullOrEmpty()
}

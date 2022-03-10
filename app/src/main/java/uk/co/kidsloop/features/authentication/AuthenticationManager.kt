package uk.co.kidsloop.features.authentication

import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationManager @Inject constructor() {

    private lateinit var b2cClientApp: IMultipleAccountPublicClientApplication
    private var accessToken: String? = null
    private var scope: String? = null
    private var expiryDate: String? = null
    private var tenantId: String? = null

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

    fun isNotAutenticated(): Boolean = accessToken.isNullOrEmpty()
}

package uk.co.kidsloop.features.authentication

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import uk.co.kidsloop.features.authentication.B2CConfiguration.getAuthorityFromPolicyName

@AndroidEntryPoint
class B2CModeFragment : Fragment() {

    @Inject
    lateinit var authManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (authManager.isNotAutenticated()) {
            startAuthenticationFlow()
        }
    }

    /**
     * Initializes UI variables and callbacks.
     */
    private fun startAuthenticationFlow() {
        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .fromAuthority(getAuthorityFromPolicyName("b2c_1a_relying_party_sign_up_log_in"))
            .withScopes(B2CConfiguration.scopes)
            .withPrompt(Prompt.LOGIN)
            .withCallback(authInteractiveCallback)
            .build()

        val b2cApp = authManager.getB2CClientApp()
        b2cApp.acquireToken(parameters)
    }

    private val authInteractiveCallback: AuthenticationCallback
        private get() = object : AuthenticationCallback {
            override fun onSuccess(result: IAuthenticationResult) {

                authManager.saveAccessToken(result.accessToken)
                findNavController().navigate(B2CModeFragmentDirections.b2cToLogin())
            }

            override fun onError(exception: MsalException) {
                val B2C_PASSWORD_CHANGE = "AADB2C90118"
                if (exception.message!!.contains(B2C_PASSWORD_CHANGE)) {
                    //                    binding.txtLog.text = """
                    //                        The user clicks the 'Forgot Password' link in a sign-up or sign-in user flow.
                    //                        Your application needs to handle this error code by running a specific user flow that resets the password.
                    //                    """.trimIndent()
                    return
                }

                /* Failed to acquireToken */Log.d(TAG, "Authentication failed: $exception")
                if (exception is MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception is MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }
            }

            override fun onCancel() {
                /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.")
            }
        }

    companion object {

        private val TAG = B2CModeFragment::class.java.simpleName
    }
}

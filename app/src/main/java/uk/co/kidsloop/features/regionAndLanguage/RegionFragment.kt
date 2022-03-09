package uk.co.kidsloop.features.regionAndLanguage

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.FragmentRegionBinding
import uk.co.kidsloop.features.authentication.AuthenticationManager
import uk.co.kidsloop.features.authentication.B2CConfiguration
import uk.co.kidsloop.features.regionAndLanguage.data.Datasource

@AndroidEntryPoint
class RegionFragment : BaseFragment(R.layout.fragment_region) {

    companion object {

        private val TAG = RegionFragment::class.java.simpleName
    }

    private val binding by viewBinding(FragmentRegionBinding::bind)

    private lateinit var parameters: AcquireTokenParameters

    @Inject
    lateinit var authManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .fromAuthority(B2CConfiguration.getAuthorityFromPolicyName("b2c_1a_relying_party_sign_up_log_in"))
            .withScopes(B2CConfiguration.scopes)
            .withPrompt(Prompt.LOGIN)
            .withCallback(authInteractiveCallback)
            .build()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.regionRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.regionRecyclerView.adapter = context?.let { Datasource(it).getRegionsList() }
            ?.let {
                RegionAdapter({ clickedRegion ->
                    onRegionClicked(clickedRegion)
                }, it.toTypedArray())
            }
        binding.backButton.setOnClickListener {
            Navigation.findNavController(requireView()).navigateUp()
        }
    }

    private fun startAuthenticationFlow() {
        val b2cApp = authManager.getB2CClientApp()
        b2cApp.acquireToken(parameters)
    }

    private fun onRegionClicked(region: String) {
        startAuthenticationFlow()
    }

    private val authInteractiveCallback: AuthenticationCallback
        private get() = object : AuthenticationCallback {
            override fun onSuccess(result: IAuthenticationResult) {

                authManager.saveAccessToken(result.accessToken)
                findNavController().navigate(RegionFragmentDirections.regionToLogin())
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
}

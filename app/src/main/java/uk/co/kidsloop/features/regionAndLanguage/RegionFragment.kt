package uk.co.kidsloop.features.regionAndLanguage

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Pair
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.app.utils.UI.DividerItemDecorator
import uk.co.kidsloop.databinding.FragmentRegionBinding
import uk.co.kidsloop.features.authentication.AuthenticationManager
import uk.co.kidsloop.features.authentication.B2CConfiguration
import uk.co.kidsloop.features.regionAndLanguage.data.RegionsAndLanguages
import javax.inject.Inject

@AndroidEntryPoint
class RegionFragment : BaseFragment(R.layout.fragment_region) {

    companion object {

        private val TAG = RegionFragment::class.java.simpleName
        private const val LANGUAGE_CODE = "languageCode"
        private const val SIGN_UP_LOGIN_POLICY = "b2c_1a_relying_party_sign_up_log_in"
    }

    private val binding by viewBinding(FragmentRegionBinding::bind)
    private val regions = RegionsAndLanguages.regionsList()
    private val adapterRegion = RegionAdapter({ onRegionClicked() }, regions.toTypedArray())

    private lateinit var parameters: AcquireTokenParameters

    @Inject
    lateinit var authManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val languageCode: String = arguments?.getString(LANGUAGE_CODE)!!
        val extraQueryParameters: MutableList<Pair<String, String>> = mutableListOf()
        extraQueryParameters.add(Pair("ui_locales", languageCode))

        parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withAuthorizationQueryStringParameters(extraQueryParameters)
            .fromAuthority(B2CConfiguration.getAuthorityFromPolicyName(SIGN_UP_LOGIN_POLICY))
            .withScopes(B2CConfiguration.scopes)
            .withPrompt(Prompt.LOGIN)
            .withCallback(authInteractiveCallback)
            .build()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.regionRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = adapterRegion
            binding.regionRecyclerView.addItemDecoration(
                DividerItemDecorator(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.divider
                    )!!
                )
            )
        }

        binding.backButton.setOnClickListener {
            Navigation.findNavController(requireView()).navigateUp()
        }
    }

    override fun onStart() {
        super.onStart()

        adapterRegion.invalidateSelection()
    }

    private fun startAuthenticationFlow() {
        val b2cApp = authManager.getB2CClientApp()
        b2cApp.acquireToken(parameters)
    }

    private fun onRegionClicked() {
        startAuthenticationFlow()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.regionGroup.isVisible = false
            binding.loadingIndication.isVisible = true
        }, 300)
    }

    private val authInteractiveCallback: AuthenticationCallback
        private get() = object : AuthenticationCallback {
            override fun onSuccess(result: IAuthenticationResult) {

                authManager.saveAccessToken(result.accessToken)
                authManager.saveAccountId(result.account.id)
                findNavController().navigate(RegionFragmentDirections.regionToSchedule())
            }

            override fun onError(exception: MsalException) {
                binding.loadingIndication.isVisible = false
                binding.regionGroup.isVisible = true
                val B2C_PASSWORD_CHANGE = "AADB2C90118"
                if (exception.message!!.contains(B2C_PASSWORD_CHANGE)) {
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
                binding.loadingIndication.isVisible = false
                binding.regionGroup.isVisible = true
            }
        }
}

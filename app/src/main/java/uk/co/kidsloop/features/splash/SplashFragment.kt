package uk.co.kidsloop.features.splash

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.navigation.fragment.findNavController
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import uk.co.kidsloop.R
import uk.co.kidsloop.app.common.BaseFragment
import uk.co.kidsloop.features.authentication.AuthenticationManager

@AndroidEntryPoint
class SplashFragment : BaseFragment(R.layout.fragment_splash) {

    @Inject
    lateinit var appContext: Context

    @Inject
    lateinit var authManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (authManager.isNotAuthenticated()) {
            PublicClientApplication.createMultipleAccountPublicClientApplication(
                appContext,
                R.raw.auth_config_b2c,
                object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                    override fun onCreated(
                        application: IMultipleAccountPublicClientApplication
                    ) {
                        authManager.saveB2CClientApp(application)
                        findNavController().navigate(SplashFragmentDirections.splashToLanguage())
                    }

                    override fun onError(
                        exception: MsalException
                    ) {
                        Log.d("SplashFragment", exception.message.toString())
                    }
                }
            )
        } else {
            findNavController().navigate(SplashFragmentDirections.splashToLogin())
        }
    }
}

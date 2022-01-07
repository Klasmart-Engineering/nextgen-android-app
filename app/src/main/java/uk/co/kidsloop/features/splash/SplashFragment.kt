package uk.co.kidsloop.features.splash

import android.os.Handler
import androidx.navigation.Navigation
import uk.co.kidsloop.R
import uk.co.kidsloop.app.BaseFragment

class SplashFragment : BaseFragment(R.layout.splash_fragment) {

    private val handler = Handler()

    private val finishSplash: Runnable = Runnable {
        Navigation.findNavController(requireView())
            .navigate(SplashFragmentDirections.splashToLogin())
    }

    override fun onStart() {
        super.onStart()
        handler.postDelayed(finishSplash, 5L)
    }

    override fun onStop() {
        handler.removeCallbacks(finishSplash)
        super.onStop()
    }
}
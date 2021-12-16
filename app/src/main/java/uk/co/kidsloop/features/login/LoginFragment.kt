package uk.co.kidsloop.app.features.login

import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import uk.co.kidsloop.R
import uk.co.kidsloop.app.BaseFragment
import uk.co.kidsloop.databinding.LoginFragmentBinding

class LoginFragment : BaseFragment(R.layout.login_fragment) {

    private val binding by viewBinding(LoginFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginBtn.setOnClickListener {
            Navigation.findNavController(requireView())
                .navigate(LoginFragmentDirections.loginToLiveVideostream())
        }
    }
}
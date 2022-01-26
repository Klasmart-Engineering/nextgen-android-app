package uk.co.kidsloop.features.login

import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.data.enums.SharedPrefsWrapper
import uk.co.kidsloop.databinding.FragmentLoginBinding
import uk.co.kidsloop.liveswitch.Config.STUDENT_ROLE
import uk.co.kidsloop.liveswitch.Config.TEACHER_ROLE
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : BaseFragment(R.layout.fragment_login) {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    private val binding by viewBinding(FragmentLoginBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginAsStudentBtn.setOnClickListener {
            sharedPrefsWrapper.saveRole(STUDENT_ROLE)
            Navigation.findNavController(requireView())
                .navigate(LoginFragmentDirections.loginToPreview())
        }

        binding.loginAsTeacherBtn.setOnClickListener {
            sharedPrefsWrapper.saveRole(TEACHER_ROLE)
            Navigation.findNavController(requireView())
                .navigate(LoginFragmentDirections.loginToPreview())
        }
    }
}
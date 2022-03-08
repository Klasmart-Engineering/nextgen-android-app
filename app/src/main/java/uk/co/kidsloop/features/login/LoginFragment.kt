package uk.co.kidsloop.features.login

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.navigation.Navigation
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.app.utils.clickable
import uk.co.kidsloop.app.utils.disable
import uk.co.kidsloop.app.utils.enable
import uk.co.kidsloop.app.utils.unclickable
import uk.co.kidsloop.data.enums.SharedPrefsWrapper
import uk.co.kidsloop.databinding.FragmentLoginBinding
import uk.co.kidsloop.liveswitch.Config.ASSISTANT_TEACHER_ROLE
import uk.co.kidsloop.liveswitch.Config.CHANNEL_ID
import uk.co.kidsloop.liveswitch.Config.STUDENT_ROLE
import uk.co.kidsloop.liveswitch.Config.TEACHER_ROLE
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : BaseFragment(R.layout.fragment_login) {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    private val binding by viewBinding(FragmentLoginBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (sharedPrefsWrapper.getChannelID() != CHANNEL_ID) {
            binding.channelID.setText(sharedPrefsWrapper.getChannelID())
            binding.loginAsStudentBtn.enable()
            binding.loginAsStudentBtn.clickable()
            binding.loginAsTeacherBtn.enable()
            binding.loginAsTeacherBtn.clickable()
            binding.loginAsAssistantTeacherBtn.enable()
            binding.loginAsAssistantTeacherBtn.clickable()
        }

        binding.channelID.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.channelID.text.toString().isEmpty()) {
                    binding.loginAsStudentBtn.disable()
                    binding.loginAsStudentBtn.unclickable()
                    binding.loginAsTeacherBtn.disable()
                    binding.loginAsTeacherBtn.unclickable()
                    binding.loginAsAssistantTeacherBtn.disable()
                    binding.loginAsAssistantTeacherBtn.unclickable()
                } else {
                    binding.loginAsStudentBtn.enable()
                    binding.loginAsStudentBtn.clickable()
                    binding.loginAsTeacherBtn.enable()
                    binding.loginAsTeacherBtn.clickable()
                    binding.loginAsAssistantTeacherBtn.enable()
                    binding.loginAsAssistantTeacherBtn.clickable()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        binding.channelID.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                sharedPrefsWrapper.saveChannelID(binding.channelID.text.toString())
            }
        }

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

        binding.loginAsAssistantTeacherBtn.setOnClickListener {
            sharedPrefsWrapper.saveRole(ASSISTANT_TEACHER_ROLE)
            Navigation.findNavController(requireView())
                .navigate(LoginFragmentDirections.loginToPreview())
        }
    }
}

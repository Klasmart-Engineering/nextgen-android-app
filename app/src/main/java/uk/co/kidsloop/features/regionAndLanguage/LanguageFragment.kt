package uk.co.kidsloop.features.regionAndLanguage

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.FragmentLanguageBinding

class LanguageFragment : BaseFragment(R.layout.fragment_language) {
    private val binding by viewBinding(FragmentLanguageBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleTextview.setOnClickListener {
            Navigation.findNavController(requireView())
                .navigate(LanguageFragmentDirections.languageToRegion())
        }
    }
}

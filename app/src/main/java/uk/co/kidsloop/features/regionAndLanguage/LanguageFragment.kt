package uk.co.kidsloop.features.regionAndLanguage

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.FragmentLanguageBinding
import uk.co.kidsloop.features.regionAndLanguage.data.Datasource

class LanguageFragment : BaseFragment(R.layout.fragment_language) {

    private val binding by viewBinding(FragmentLanguageBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.languageRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.languageRecyclerView.adapter = context?.let { Datasource(it).getLanguageList() }
            ?.let {
                LanguageAdapter({ clickedRegion ->
                    onLanguageClicked(clickedRegion)
                }, it.toTypedArray())
            }
    }

    private fun onLanguageClicked(language: String) {
        findNavController().navigate(LanguageFragmentDirections.languageToRegion(language))
    }
}

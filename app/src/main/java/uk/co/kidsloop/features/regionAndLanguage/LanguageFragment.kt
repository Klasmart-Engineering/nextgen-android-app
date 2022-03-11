package uk.co.kidsloop.features.regionAndLanguage

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.FragmentLanguageBinding
import uk.co.kidsloop.features.regionAndLanguage.data.Language
import uk.co.kidsloop.features.regionAndLanguage.data.RegionsAndLanguages

class LanguageFragment : BaseFragment(R.layout.fragment_language) {

    private val binding by viewBinding(FragmentLanguageBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val languages = RegionsAndLanguages.languagesList()
        binding.languageRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = LanguageAdapter({ language -> onLanguageClicked(language) }, languages.toTypedArray())
            addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun onLanguageClicked(language: Language) {
        findNavController().navigate(LanguageFragmentDirections.languageToRegion(language.code))
    }
}

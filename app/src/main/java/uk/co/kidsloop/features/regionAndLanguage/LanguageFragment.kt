package uk.co.kidsloop.features.regionAndLanguage

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
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
            val dividerItemDecoration: ItemDecoration =
                DividerItemDecorator(ContextCompat.getDrawable(context, R.drawable.divider)!!)
            binding.languageRecyclerView.addItemDecoration(dividerItemDecoration)
        }
    }

    private fun onLanguageClicked(language: Language) {
        findNavController().navigate(LanguageFragmentDirections.languageToRegion(language.code))
    }
}

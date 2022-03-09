package uk.co.kidsloop.features.regionAndLanguage

import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.FragmentRegionBinding
import uk.co.kidsloop.features.regionAndLanguage.data.Datasource

class RegionFragment : BaseFragment(R.layout.fragment_region) {
    private val binding by viewBinding(FragmentRegionBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.regionRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = context?.let { Datasource(it).getRegionsList() }
                ?.let { RegionAdapter(it.toTypedArray()) }
            addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        }

        binding.backButton.setOnClickListener {
            Navigation.findNavController(requireView()).navigateUp()
        }
    }
}

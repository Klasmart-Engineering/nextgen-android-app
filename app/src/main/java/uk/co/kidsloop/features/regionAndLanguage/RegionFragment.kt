package uk.co.kidsloop.features.regionAndLanguage

import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.FragmentRegionBinding

class RegionFragment : BaseFragment(R.layout.fragment_region) {
    private val binding by viewBinding(FragmentRegionBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.regionRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.regionRecyclerView.adapter = context?.let { Datasource(it).getRegionsList() }
            ?.let { RegionAdapter(it.toTypedArray()) }
        binding.backButton.setOnClickListener {
            Navigation.findNavController(requireView()).navigateUp()
        }
    }
}

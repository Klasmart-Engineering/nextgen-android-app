package uk.co.kidsloop.features.regionAndLanguage

import android.content.pm.ActivityInfo
import android.os.Bundle
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.FragmentRegionBinding

class RegionFragment : BaseFragment(R.layout.fragment_region) {
    private val binding by viewBinding(FragmentRegionBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
    }
}

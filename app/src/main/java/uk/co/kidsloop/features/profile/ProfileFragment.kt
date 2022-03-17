package uk.co.kidsloop.features.profile

import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.FragmentProfileBinding

class ProfileFragment : BaseFragment(R.layout.fragment_profile) {
    private val binding by viewBinding(FragmentProfileBinding::bind)
}

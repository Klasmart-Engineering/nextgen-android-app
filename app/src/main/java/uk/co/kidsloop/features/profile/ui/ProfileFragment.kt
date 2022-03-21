package uk.co.kidsloop.features.profile.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import uk.co.kidsloop.R
import uk.co.kidsloop.app.structure.BaseFragment
import uk.co.kidsloop.databinding.FragmentProfileBinding

@AndroidEntryPoint
class ProfileFragment : BaseFragment(R.layout.fragment_profile) {
    private val profileAdapter = ProfileAdapter()

    private val binding by viewBinding(FragmentProfileBinding::bind)

    private val viewModel by viewModels<ProfileViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.profilesRecyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = profileAdapter
        }
        viewModel.profilesLiveData.observe(
            viewLifecycleOwner,
            Observer {
                if (it is ProfileViewModel.ProfilesUiState.Success) {
                    profileAdapter.refresh(it.profiles)
                }
            }
        )
    }
}

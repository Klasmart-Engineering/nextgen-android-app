package uk.co.kidsloop.features.schedule

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import uk.co.kidsloop.app.utils.getInitials
import uk.co.kidsloop.R
import uk.co.kidsloop.app.common.BaseFragment
import uk.co.kidsloop.app.utils.getInitials
import uk.co.kidsloop.app.utils.gone
import uk.co.kidsloop.app.utils.visible
import uk.co.kidsloop.databinding.FragmentScheduleBinding

@AndroidEntryPoint
class ScheduleFragment : BaseFragment(R.layout.fragment_schedule) {

    private val binding by viewBinding(FragmentScheduleBinding::bind)

    companion object {

        const val MAX_CLASSES_VISIBLE: Int = 6
        const val FIVE_MIN_IN_MILLIS = (5 * 60 * 1000).toLong()
        const val PROFILE_NAME = "profileName"
    }

    private val viewModel by viewModels<SchedulesViewModel>()

    private var profileName = ""
    private lateinit var scheduleAdapter: ScheduleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        profileName = arguments?.getString(PROFILE_NAME)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scheduleAdapter = ScheduleAdapter { onClassClicked() }
        binding.classesRecyclerView.apply {
            adapter = scheduleAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            addItemDecoration(DividerItemDecorationLastExcluded(resources.getDimensionPixelSize(R.dimen.space_16)))
        }

        viewModel.schedulesLiveData.observe(
            viewLifecycleOwner
        ) {
            if (it is SchedulesViewModel.SchedulesUiState.Success) {
                dismissLoading()
                if (it.scheduleEntity.isEmpty()) {
                    binding.noClassTextview.visible()
                } else {
                    binding.noClassTextview.gone()
                }
                scheduleAdapter.refresh(it.scheduleEntity)
            }
        }
        binding.initials.text = profileName.getInitials()
        binding.welcomeLabel.text = getString(R.string.welcome_comma_first_name_of_user, profileName.split(" ")[0])
    }

    private fun dismissLoading() {
        binding.loadingView.pauseAnimation()
        binding.loadingView.gone()
    }

    private fun onClassClicked() {
        findNavController().navigate(ScheduleFragmentDirections.scheduleToLogin())
    }
}

package uk.co.kidsloop.features.liveclass.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*
import uk.co.kidsloop.R
import uk.co.kidsloop.databinding.LeaveClassDialogFragmentBinding
import uk.co.kidsloop.features.connectivity.NetworkFetchState

@AndroidEntryPoint
class LeaveClassDialog : DialogFragment() {
    companion object {
        val TAG = LeaveClassDialog::class.qualifiedName
    }

    private val binding by viewBinding(LeaveClassDialogFragmentBinding::bind)
    private lateinit var timer: Timer
    private val viewModel by viewModels<LeaveClassDialogViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.leave_class_dialog_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setControls()
        observe()
    }

    private fun setControls() {
        binding.confirmExitClassBtn.setOnClickListener {
            timer.cancel()
            requireActivity().supportFragmentManager.setFragmentResult(TAG.toString(), Bundle.EMPTY)
        }

        binding.backBtn.setOnClickListener {
            dismiss()
        }
    }

    @ExperimentalCoroutinesApi
    private fun observe() {
        viewModel.networkState.observe(
            viewLifecycleOwner,
            {
                when (it) {
                    NetworkFetchState.FETCHED_WIFI -> {}
                    NetworkFetchState.FETCHED_MOBILE_DATA -> {}
                    NetworkFetchState.ERROR -> dismiss()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        setTimerForDialogToDismiss()
    }

    override fun onStop() {
        super.onStop()
        dismissAllowingStateLoss()
        timer.cancel()
    }

    private fun setTimerForDialogToDismiss() {
        timer = Timer().apply {
            schedule(
                object : TimerTask() {
                    override fun run() {
                        if (dialog?.isShowing == true)
                            dismiss()
                        cancel()
                    }
                },
                10000
            )
        }
    }
}

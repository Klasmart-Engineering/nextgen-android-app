package uk.co.kidsloop.features.liveclass

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import uk.co.kidsloop.R
import uk.co.kidsloop.databinding.LeaveClassDialogFragmentBinding
import java.util.*

class LeaveClassDialog : DialogFragment() {
    companion object {
        val TAG = LeaveClassDialog::class.qualifiedName
    }

    private val binding by viewBinding(LeaveClassDialogFragmentBinding::bind)
    private lateinit var timer: Timer

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

    private fun setControls() {
        binding.confirmExitClassBtn.setOnClickListener {
            timer.cancel()
            requireActivity().supportFragmentManager.setFragmentResult(TAG.toString(), Bundle.EMPTY)
        }

        binding.backBtn.setOnClickListener {
            dismiss()
        }
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

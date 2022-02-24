package uk.co.kidsloop.features.liveclass

import android.content.Context
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import java.util.*

class DialogManager(private val context: Context, private val fragmentManager: FragmentManager) {
    var onDialogClickedListener: OnDialogClickedListener? = null

    companion object {
        val LEAVE_CLASS_DIALOG_FRAGMENT_TAG = LeaveClassDialog::class.qualifiedName
    }

    fun showLeaveDialog() {
        val dialogFragment = LeaveClassDialog()
        showDialog(dialogFragment)
    }

    fun setTimerForDialog() {
        Timer().apply {
            schedule(
                object : TimerTask() {
                    override fun run() {
                        getCurrentlyShownDialog()?.dismiss()
                        cancel()
                    }
                },
                10000
            )
        }
    }

//    private fun getString(@StringRes stringRes: Int): String {
//        return context.getString(stringRes)
//    }

    private fun showDialog(dialog: DialogFragment) {
        fragmentManager.beginTransaction().add(dialog, LEAVE_CLASS_DIALOG_FRAGMENT_TAG).commitAllowingStateLoss()
    }

    fun getCurrentlyShownDialog(): DialogFragment? {
        val fragmentDialogWithTag =
            fragmentManager.findFragmentByTag(LEAVE_CLASS_DIALOG_FRAGMENT_TAG)
        return if (fragmentDialogWithTag != null && DialogFragment::class.java.isAssignableFrom(
                fragmentDialogWithTag.javaClass
            )
        ) {
            fragmentDialogWithTag as DialogFragment
        } else {
            null
        }
    }

    fun setOnDialogClickedListener() {
        onDialogClickedListener?.onDialogClicked()
    }
}

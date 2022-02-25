package uk.co.kidsloop.features.liveclass

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import javax.inject.Inject

class DialogsManager @Inject constructor(private val fragmentManager: FragmentManager) {
    companion object {
        val LEAVE_CLASS_DIALOG_FRAGMENT_TAG = LeaveClassDialog::class.qualifiedName
    }

    fun showLeaveDialog() {
        val dialogFragment = LeaveClassDialog()
        showDialog(dialogFragment)
    }

    fun dismissDialog() {
        getCurrentlyShownDialog()?.dismiss()
    }

    private fun showDialog(dialog: DialogFragment) {
        fragmentManager.beginTransaction().add(dialog, LEAVE_CLASS_DIALOG_FRAGMENT_TAG).commitAllowingStateLoss()
    }

    private fun getCurrentlyShownDialog(): DialogFragment? {
        val fragmentDialogWithTag = fragmentManager.findFragmentByTag(LEAVE_CLASS_DIALOG_FRAGMENT_TAG)
        return if (fragmentDialogWithTag != null &&
            DialogFragment::class.java.isAssignableFrom(fragmentDialogWithTag.javaClass)
        ) {
            fragmentDialogWithTag as DialogFragment
        } else {
            null
        }
    }
}

package uk.co.kidsloop.app.common

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import uk.co.kidsloop.features.liveclass.dialogs.LeaveClassDialog

class DialogsManager constructor(private val childFragmentManager: FragmentManager) {

    fun showDialog(tag: String?) {
        when (tag) {
            LeaveClassDialog.TAG -> {
                showDialog(LeaveClassDialog(), childFragmentManager, LeaveClassDialog.TAG)
            }
        }
    }

    private fun showDialog(dialog: DialogFragment, fragmentManager: FragmentManager, tag: String?) {
        fragmentManager.beginTransaction().add(dialog, tag).commitAllowingStateLoss()
    }
}

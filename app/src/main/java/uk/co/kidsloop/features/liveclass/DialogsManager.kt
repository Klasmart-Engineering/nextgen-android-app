package uk.co.kidsloop.features.liveclass

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import uk.co.kidsloop.app.di.ChildFragmentManger
import uk.co.kidsloop.app.di.ParentFragmentManager
import javax.inject.Inject

class DialogsManager @Inject constructor(
    @ParentFragmentManager private val parentFragmentManager: FragmentManager,
    @ChildFragmentManger private val childFragmentManager: FragmentManager
) {

    fun showDialog(tag: String?) {
        when (tag) {
            LeaveClassDialog.TAG -> {
                showDialog(LeaveClassDialog(), childFragmentManager, LeaveClassDialog.TAG)
            }
        }
    }

    fun dismissDialog(tag: String) {
        val fragmentInParentFM = parentFragmentManager.findFragmentByTag(tag)
        val fragmentInChildFM = childFragmentManager.findFragmentByTag(tag)

        if (fragmentInParentFM != null) {
            parentFragmentManager.popBackStack(parentFragmentManager.fragments.indexOf(fragmentInParentFM), 0)
        } else if (fragmentInChildFM != null) {
            childFragmentManager.popBackStack(childFragmentManager.fragments.indexOf(fragmentInChildFM), 0)
        }
    }

    private fun showDialog(dialog: DialogFragment, fragmentManager: FragmentManager, tag: String?) {
        fragmentManager.beginTransaction().add(dialog, tag).commitAllowingStateLoss()
    }
}

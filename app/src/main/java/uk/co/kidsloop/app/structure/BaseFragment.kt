package uk.co.kidsloop.app.structure

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import uk.co.kidsloop.app.KidsloopActivity
import uk.co.kidsloop.app.di.presentation.PresentationModule

/**
 * Base class for fragments
 */
open class BaseFragment(@LayoutRes layoutId: Int) : Fragment(layoutId) {

    private val presentationComponent by lazy {
        (requireActivity() as KidsloopActivity).activityComponent.newPresentationComponent(
            PresentationModule(this)
        )
    }

    protected val injector get() = presentationComponent
}

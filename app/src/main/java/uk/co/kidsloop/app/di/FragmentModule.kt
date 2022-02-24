package uk.co.kidsloop.app.di

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import uk.co.kidsloop.features.liveclass.DialogsManager

@Module
@InstallIn(FragmentComponent::class)
object FragmentModule {
    @Provides
    fun providesFragment(fragment: Fragment): FragmentManager = fragment.parentFragmentManager

    @Provides
    fun providesDialogManager(fragmentManager: FragmentManager) = DialogsManager(fragmentManager)
}

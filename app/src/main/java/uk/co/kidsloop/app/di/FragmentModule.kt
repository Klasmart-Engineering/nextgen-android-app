package uk.co.kidsloop.app.di

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import uk.co.kidsloop.features.liveclass.DialogsManager
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ParentFragmentManager

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ChildFragmentManger

@Module
@InstallIn(FragmentComponent::class)
object FragmentModule {
    @Provides
    @ParentFragmentManager
    fun providesParentFragmentManager(fragment: Fragment): FragmentManager = fragment.parentFragmentManager

    @Provides
    @ChildFragmentManger
    fun providesChildFragmentManger(fragment: Fragment): FragmentManager = fragment.childFragmentManager

    @Provides
    fun providesDialogManager(
        @ParentFragmentManager parentFragmentManager: FragmentManager,
        @ChildFragmentManger childFragmentManager: FragmentManager
    ) =
        DialogsManager(parentFragmentManager, childFragmentManager)
}

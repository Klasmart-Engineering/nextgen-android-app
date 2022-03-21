package uk.co.kidsloop.app.di

import android.app.Activity
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import uk.co.kidsloop.app.common.DialogsManager
import uk.co.kidsloop.app.common.ToastDetailsProvider
import uk.co.kidsloop.app.common.ToastHelper

@Module
@InstallIn(FragmentComponent::class)
object FragmentModule {

    @Provides
    fun providesChildFragmentManger(fragment: Fragment): FragmentManager = fragment.childFragmentManager

    @Provides
    fun providesLayoutInflater(fragment: Fragment): LayoutInflater = fragment.layoutInflater

    @Provides
    fun providesToastHelper(
        layoutInflater: LayoutInflater,
        context: Activity,
        toastDetailsProvider: ToastDetailsProvider
    ): ToastHelper = ToastHelper(layoutInflater, context, toastDetailsProvider)

    @Provides
    fun providesDialogManager(childFragmentManager: FragmentManager) = DialogsManager(childFragmentManager)
}

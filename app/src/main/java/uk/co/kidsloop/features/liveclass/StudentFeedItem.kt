package uk.co.kidsloop.features.liveclass

import android.view.View

data class StudentFeedItem(val remoteView: View, val clientId: String, var isDisplayed:Boolean = false)
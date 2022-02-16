package uk.co.kidsloop.features.liveclass.feeds

import android.view.View

data class StudentFeedItem(val remoteView: View, val clientId: String) : FeedItem(remoteView, clientId)

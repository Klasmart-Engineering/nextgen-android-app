package uk.co.kidsloop.features.liveclass.feeds

import android.view.View

data class LocalMediaFeedItem(
    val localView: View,
    val clientId: String,
    var isMicMuted: Boolean,
    var isCamTurnedOff: Boolean,
    var isHandRaised: Boolean = false,
    var isHandLowered: Boolean = false
) : FeedItem(localView, clientId)

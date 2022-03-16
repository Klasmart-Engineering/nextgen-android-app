package uk.co.kidsloop.features.liveclass.feeds

import android.view.View
import uk.co.kidsloop.liveswitch.Config

data class FeedItem(
    val videoFeedView: View,
    val clientId: String,
    val role: String = Config.STUDENT_ROLE,
    var hasHandRaised: Boolean = false
)

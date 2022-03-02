package uk.co.kidsloop.features.liveclass.localmedia

import android.content.Context
import android.view.View
import fm.liveswitch.ViewSink
import fm.liveswitch.LayoutScale
import fm.liveswitch.VideoConfig
import fm.liveswitch.VideoDegradationPreference
import fm.liveswitch.VideoSource
import fm.liveswitch.android.Camera2Source
import fm.liveswitch.android.CameraPreview
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext

class CameraLocalMedia(
    context: Context,
    disableAudio: Boolean,
    disableVideo: Boolean,
    aecContext: AecContext,
    enableSimulcast: Boolean,
    isTeacher: Boolean
) : LocalMedia<View>(context, disableAudio, disableVideo, aecContext) {

    private var viewSink: CameraPreview = CameraPreview(context, LayoutScale.Cover)
    private val videoConfig = when (isTeacher) {
        true -> VideoConfig(640, 480, 30.0)
        false -> VideoConfig(480, 360, 30.0)
    }

    init {
        if (enableSimulcast) {
            this.videoSimulcastEncodingCount = 3
            this.videoSimulcastDegradationPreference = VideoDegradationPreference.Automatic
            this.videoSimulcastDisabled = false
        }

        super.initialize()
    }

    override fun createVideoSource(): VideoSource {
        return Camera2Source(viewSink, videoConfig)
    }

    override fun createViewSink(): ViewSink<View>? {
        return null
    }

    // Return an Android View for local preview rather than using ViewSink.
    override fun getView(): View {
        return viewSink.view
    }
}

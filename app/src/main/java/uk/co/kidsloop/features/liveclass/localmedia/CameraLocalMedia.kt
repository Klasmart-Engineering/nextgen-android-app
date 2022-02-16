package uk.co.kidsloop.features.liveclass.localmedia

import android.content.Context
import android.view.View
import fm.liveswitch.*
import fm.liveswitch.android.Camera2Source
import fm.liveswitch.android.CameraPreview
import uk.co.kidsloop.data.enums.VideoQuality
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext

class CameraLocalMedia(
    context: Context,
    disableAudio: Boolean,
    disableVideo: Boolean,
    aecContext: AecContext,
    enableSimulcast: Boolean
) : LocalMedia<View>(context, disableAudio, disableVideo, aecContext) {

    private var viewSink: CameraPreview = CameraPreview(context, LayoutScale.Cover)
    private val videoConfig = VideoConfig(640, 480, 30.0)

    init {
        if (enableSimulcast) {
            this.videoSimulcastEncodingCount = 3

            val videoEncodings = Array(3) { VideoEncodingConfig() }
            videoEncodings[0].bitrate = VideoQuality.P720.videoBitrate
            videoEncodings[0].frameRate = VideoQuality.P720.frameRate
            videoEncodings[0].scale = VideoQuality.P720.scale

            videoEncodings[1].bitrate = VideoQuality.P480.videoBitrate
            videoEncodings[1].frameRate = VideoQuality.P480.frameRate
            videoEncodings[1].scale = VideoQuality.P480.scale

            videoEncodings[2].bitrate = VideoQuality.P320.videoBitrate
            videoEncodings[2].frameRate = VideoQuality.P320.frameRate
            videoEncodings[2].scale = VideoQuality.P320.scale

            this.videoSimulcastDisabled = false
            this.videoEncodings = videoEncodings
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

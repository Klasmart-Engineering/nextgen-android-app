package uk.co.kidsloop.features.liveclass.localmedia

import android.content.Context
import android.view.View
import fm.liveswitch.*
import fm.liveswitch.android.Camera2Source
import fm.liveswitch.android.CameraPreview
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext

class CameraLocalMedia(
    context: Context,
    disableAudio: Boolean,
    disableVideo: Boolean,
    aecContext: AecContext,
    enableSimulcast: Boolean
) : LocalMedia<View>(context, disableAudio, disableVideo, aecContext) {

    private var viewSink: CameraPreview = CameraPreview(context, LayoutScale.Cover)
    private val videoConfig = VideoConfig(640, 480, 32.0)

    init {
        if(enableSimulcast) {
            this.videoSimulcastEncodingCount = 3

            val videoEncodings = Array(3) { VideoEncodingConfig() }
            videoEncodings[0].bitrate = 512
            videoEncodings[0].frameRate = 30.0

            videoEncodings[1].bitrate = 256
            videoEncodings[1].frameRate = 15.0
            videoEncodings[1].scale = 0.5

            videoEncodings[2].bitrate = 128
            videoEncodings[2].frameRate = 7.5
            videoEncodings[2].scale = 0.25

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

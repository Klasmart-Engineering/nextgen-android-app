package uk.co.kidsloop.features.liveclass.remoteviews

import android.content.Context
import android.widget.FrameLayout
import fm.liveswitch.AudioConfig
import fm.liveswitch.AudioDecoder
import fm.liveswitch.AudioFormat
import fm.liveswitch.AudioSink
import fm.liveswitch.RtcRemoteMedia
import fm.liveswitch.VideoDecoder
import fm.liveswitch.VideoFormat
import fm.liveswitch.VideoPipe
import fm.liveswitch.VideoSink
import fm.liveswitch.ViewSink
import fm.liveswitch.android.AudioTrackSink
import fm.liveswitch.android.OpenGLSink
import fm.liveswitch.opus.Decoder
import fm.liveswitch.yuv.ImageConverter

class SFURemoteMedia(private val context: Context, disableAudio:Boolean, disableVideo:Boolean, aecContext: AecContext)
    : RtcRemoteMedia<FrameLayout>(disableAudio, disableVideo, aecContext) {

    init {
        super.initialize()
    }

    override fun createAudioRecorder(audioFormat: AudioFormat): AudioSink {
        return fm.liveswitch.matroska.AudioSink(id + "-remote-audio-" + audioFormat.name.lowercase() + ".mkv")
    }

    override fun createAudioSink(audioConfig: AudioConfig?): AudioSink {
        return AudioTrackSink(audioConfig)
    }

    override fun createOpusDecoder(audioConfig: AudioConfig?): AudioDecoder {
        return Decoder()
    }

    // Remote Video
    override fun createVideoRecorder(videoFormat: VideoFormat): VideoSink {
        return fm.liveswitch.matroska.VideoSink(id + "-remote-video-" + videoFormat.name.lowercase() + ".mkv")
    }

    override fun createViewSink(): ViewSink<FrameLayout?> {
        return OpenGLSink(context)
    }

    override fun createVp8Decoder(): VideoDecoder {
        return fm.liveswitch.vp8.Decoder()
    }

    override fun createVp9Decoder(): VideoDecoder {
        return fm.liveswitch.vp9.Decoder()
    }

    override fun createH264Decoder(): VideoDecoder? {
        return null
    }

    override fun createImageConverter(videoFormat: VideoFormat?): VideoPipe {
        return ImageConverter(videoFormat)
    }
}
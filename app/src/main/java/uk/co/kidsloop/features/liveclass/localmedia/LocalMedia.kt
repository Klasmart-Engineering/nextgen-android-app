package uk.co.kidsloop.features.liveclass.localmedia

import android.content.Context
import fm.liveswitch.AudioFormat
import fm.liveswitch.AudioSink
import fm.liveswitch.RtcLocalMedia
import uk.co.kidsloop.features.liveclass.remoteviews.AecContext
import fm.liveswitch.VideoFormat
import fm.liveswitch.VideoPipe
import fm.liveswitch.AudioConfig
import fm.liveswitch.AudioEncoder
import fm.liveswitch.AudioSource
import fm.liveswitch.VideoEncoder
import fm.liveswitch.android.AudioRecordSource
import fm.liveswitch.matroska.VideoSink
import fm.liveswitch.opus.Encoder
import fm.liveswitch.yuv.ImageConverter

abstract class LocalMedia<TView>(private val context: Context, disableAudio:Boolean, disableVideo:Boolean, aecContext:AecContext)
    : RtcLocalMedia<TView>(disableAudio, disableVideo, aecContext) {

    override fun createAudioRecorder(audioFormat: AudioFormat): AudioSink? {
        return fm.liveswitch.matroska.AudioSink(id + "-local-audio-" + audioFormat.name.lowercase() + ".mkv")
    }

    override fun createAudioSource(audioConfig: AudioConfig?): AudioSource {
        return AudioRecordSource(context, audioConfig)
    }

    override fun createOpusEncoder(audioConfig: AudioConfig?): AudioEncoder {
        return Encoder(audioConfig)
    }

    // Local Video
    override fun createVideoRecorder(videoFormat: VideoFormat): VideoSink? {
        return VideoSink(id + "-local-video-" + videoFormat.name.lowercase() + ".mkv")
    }

    override fun createVp8Encoder(): VideoEncoder {
        return fm.liveswitch.vp8.Encoder()
    }

    override fun createVp9Encoder(): VideoEncoder {
        return fm.liveswitch.vp9.Encoder()
    }

    override fun createH264Encoder(): VideoEncoder? {
        return null
    }

    override fun createImageConverter(videoFormat: VideoFormat): VideoPipe {
        return ImageConverter(videoFormat)
    }
}
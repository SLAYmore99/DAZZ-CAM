package com.funcam.app.video

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.util.Log
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer

private fun videoFormat(w: Int, h: Int, fps: Int): MediaFormat =
    MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        setInteger(MediaFormat.KEY_BIT_RATE, (w * h * 6).coerceIn(2_000_000, 16_000_000))
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

/** Video-only H.264 encoder drained synchronously by the caller. Used for wiggle clips. */
class SimpleVideoEncoder(out: File, w: Int, h: Int, fps: Int) {
    private val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    val inputSurface: Surface
    private val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private var track = -1
    private val info = MediaCodec.BufferInfo()

    init {
        codec.configure(videoFormat(w, h, fps), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = codec.createInputSurface()
        codec.start()
    }

    fun drain(end: Boolean) {
        if (end) codec.signalEndOfInputStream()
        while (true) {
            val idx = codec.dequeueOutputBuffer(info, if (end) 10_000 else 0)
            when {
                idx == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!end) return
                idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    track = muxer.addTrack(codec.outputFormat); muxer.start()
                }
                idx >= 0 -> {
                    val buf = codec.getOutputBuffer(idx)!!
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0 && track >= 0) {
                        buf.position(info.offset); buf.limit(info.offset + info.size)
                        muxer.writeSampleData(track, buf, info)
                    }
                    codec.releaseOutputBuffer(idx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    fun release() {
        try { codec.stop() } catch (_: Exception) {}
        codec.release()
        inputSurface.release()
        try { muxer.stop() } catch (_: Exception) {}
        muxer.release()
    }
}

/**
 * Live recorder: H.264 from a GL input surface plus optional AAC microphone audio.
 * All timestamps are relative to [startNs] (System.nanoTime clock).
 */
class LiveRecorder(val out: File, val width: Int, val height: Int, fps: Int, withAudio: Boolean) {
    val startNs = System.nanoTime()
    private val video = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    val inputSurface: Surface
    private val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private val lock = Object()
    private var videoTrack = -1
    private var audioTrack = -1
    private var started = false
    private val pending = ArrayList<Triple<Boolean, ByteBuffer, MediaCodec.BufferInfo>>()

    private var audio: MediaCodec? = null
    private var record: AudioRecord? = null
    @Volatile private var stopping = false
    private val hasAudio: Boolean
    private val videoThread: Thread
    private var audioThread: Thread? = null

    init {
        video.configure(videoFormat(width, height, fps), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = video.createInputSurface()
        video.start()
        hasAudio = withAudio && setupAudio()
        videoThread = Thread({ drainLoop(video, true) }, "FunCamVideoDrain").apply { start() }
        if (hasAudio) audioThread = Thread({ audioLoop() }, "FunCamAudio").apply { start() }
    }

    @SuppressLint("MissingPermission")
    private fun setupAudio(): Boolean = try {
        val rate = 44100
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(MediaRecorder.AudioSource.CAMCORDER, rate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(min, 8192) * 2)
        check(rec.state == AudioRecord.STATE_INITIALIZED)
        val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, rate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        rec.startRecording()
        record = rec; audio = codec
        true
    } catch (e: Exception) {
        Log.w("FunCam", "audio unavailable", e)
        record?.release(); record = null
        false
    }

    private fun audioLoop() {
        val codec = audio ?: return
        val rec = record ?: return
        val rate = 44100
        var samples = 0L
        val tmp = ByteArray(4096)
        var firstNs = -1L
        while (true) {
            val idx = codec.dequeueInputBuffer(10_000)
            if (idx >= 0) {
                val buf = codec.getInputBuffer(idx)!!
                buf.clear()
                val n = rec.read(tmp, 0, minOf(tmp.size, buf.capacity()))
                if (firstNs < 0) firstNs = System.nanoTime() - startNs
                val pts = (firstNs / 1000) + samples * 1_000_000L / rate
                if (n > 0) {
                    buf.put(tmp, 0, n)
                    samples += n / 2
                }
                val eos = stopping
                codec.queueInputBuffer(idx, 0, maxOf(n, 0), pts,
                    if (eos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                if (eos) break
            }
            drainOnce(codec, false)
        }
        drainLoop(codec, false)
    }

    private fun drainLoop(codec: MediaCodec, isVideo: Boolean) {
        while (true) {
            if (drainOnce(codec, isVideo)) return
        }
    }

    /** Returns true at end of stream. */
    private fun drainOnce(codec: MediaCodec, isVideo: Boolean): Boolean {
        val info = MediaCodec.BufferInfo()
        val idx = codec.dequeueOutputBuffer(info, 10_000)
        if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            synchronized(lock) {
                if (isVideo) videoTrack = muxer.addTrack(codec.outputFormat)
                else audioTrack = muxer.addTrack(codec.outputFormat)
                if (!started && videoTrack >= 0 && (!hasAudio || audioTrack >= 0)) {
                    muxer.start(); started = true
                    pending.forEach { (v, b, i) -> muxer.writeSampleData(if (v) videoTrack else audioTrack, b, i) }
                    pending.clear()
                }
            }
        } else if (idx >= 0) {
            val buf = codec.getOutputBuffer(idx)!!
            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
            if (info.size > 0 && info.presentationTimeUs >= 0) {
                buf.position(info.offset); buf.limit(info.offset + info.size)
                synchronized(lock) {
                    if (started) {
                        muxer.writeSampleData(if (isVideo) videoTrack else audioTrack, buf, info)
                    } else {
                        val copy = ByteBuffer.allocateDirect(info.size).put(buf).also { it.flip() }
                        val ci = MediaCodec.BufferInfo().apply { set(0, info.size, info.presentationTimeUs, info.flags) }
                        pending.add(Triple(isVideo, copy, ci))
                    }
                }
            }
            codec.releaseOutputBuffer(idx, false)
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return true
        }
        return false
    }

    /** Call after the GL side has stopped rendering into [inputSurface]. Blocks until the file is written. */
    fun finish(): Boolean {
        stopping = true
        try { video.signalEndOfInputStream() } catch (_: Exception) {}
        videoThread.join(4000)
        audioThread?.join(4000)
        var ok = false
        try { video.stop() } catch (_: Exception) {}
        video.release()
        audio?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        record?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        inputSurface.release()
        synchronized(lock) {
            try {
                if (started) { muxer.stop(); ok = true }
            } catch (e: Exception) {
                Log.e("FunCam", "muxer stop", e)
            }
            muxer.release()
        }
        if (!ok) out.delete()
        return ok
    }
}

package com.taptoneluthier.recorder

import android.media.AudioFormat

object AudioConfig(
    const val SAMPLE_RATE = 44100
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val CHANNEL_COUNT = 1
    const val BYTES_PER_SAMPLE = 2
    const val CHUNK_SIZE_IN_SAMPLES = 2048
    const val CHUNK_SIZE_IN_BYTES = CHUNK_SIZE_IN_SAMPLES * BYTES_PER_SAMPLE
    const val AUDIO_SOURCE = android.media.MediaRecorder.AudioSource.MIC
)
package com.taptoneluthier.recorder


data class AudioChunk(
    val sampleRate: Int,
    val bytesRead: Int,
    val samples: Int,
    val pcmBytes: ByteArray,
    val peak: Int,
    val timestampMs: Long
)
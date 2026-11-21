package com.omnisound.app.domain

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val streamUrl: String? = null,
    val genre: String = "Unknown",
    val bpm: Int? = null,
    val durationMs: Long = 0,
    val liked: Boolean = false,
    val tags: Set<String> = emptySet()
)

data class ListeningEvent(
    val trackId: String,
    val listenedRatio: Float,
    val skipped: Boolean,
    val liked: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

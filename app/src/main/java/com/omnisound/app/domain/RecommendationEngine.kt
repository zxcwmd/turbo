package com.omnisound.app.domain

import kotlin.math.abs
import kotlin.math.max

/** Fully local, deterministic recommendation scorer. No listening history leaves the device. */
class RecommendationEngine {
    fun rank(candidates: List<Track>, history: List<ListeningEvent>, seed: Track? = null): List<Track> {
        if (candidates.isEmpty()) return emptyList()
        val positive = history.filter { it.liked || it.listenedRatio >= .65f }
        val negative = history.filter { it.skipped || it.listenedRatio < .18f }
        val genreWeights = mutableMapOf<String, Float>()
        val tagWeights = mutableMapOf<String, Float>()
        positive.forEach { event -> candidates.firstOrNull { it.id == event.trackId }?.let { t ->
            genreWeights[t.genre] = (genreWeights[t.genre] ?: 0f) + if (event.liked) 2f else 1f
            t.tags.forEach { tagWeights[it] = (tagWeights[it] ?: 0f) + 1f }
        } }
        negative.forEach { event -> candidates.firstOrNull { it.id == event.trackId }?.let { t ->
            genreWeights[t.genre] = (genreWeights[t.genre] ?: 0f) - 1.4f
            t.tags.forEach { tagWeights[it] = (tagWeights[it] ?: 0f) - .7f }
        } }
        val recentIds = history.takeLast(18).map { it.trackId }.toSet()
        return candidates.map { track ->
            val genre = (genreWeights[track.genre] ?: 0f).coerceIn(-4f, 8f) / 8f
            val tags = if (track.tags.isEmpty()) 0f else track.tags.map { tagWeights[it] ?: 0f }.average().toFloat().coerceIn(-2f, 4f) / 4f
            val seedAffinity = if (seed == null) 0f else {
                (if (seed.genre == track.genre) .55f else 0f) +
                    (if (seed.artist.equals(track.artist, true)) .25f else 0f) +
                    (if (seed.tags.intersect(track.tags).isNotEmpty()) .20f else 0f)
            }
            val novelty = if (track.id !in recentIds) 1f else 0f
            val tempoFit = if (seed?.bpm != null && track.bpm != null) (1f - abs(seed.bpm - track.bpm) / 120f).coerceIn(0f, 1f) else .5f
            val score = .35f * genre + .25f * tags + .20f * seedAffinity + .10f * tempoFit + .10f * novelty
            track to score
        }.sortedByDescending { it.second }.map { it.first }
    }

    fun radioTitle(seed: Track?): String = seed?.let { "Radio • ${it.title}" } ?: "Your mix"
}

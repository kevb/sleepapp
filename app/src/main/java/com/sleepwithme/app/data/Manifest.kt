package com.sleepwithme.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Manifest(
    val version: Int,
    val collections: List<Collection>
)

@Serializable
data class Collection(
    val id: String,
    val title: String,
    val description: String,
    val tracks: List<Track>
)

@Serializable
data class Track(
    val id: String,
    val title: String,
    val url: String,
    @SerialName("duration_secs") val durationSecs: Int
)

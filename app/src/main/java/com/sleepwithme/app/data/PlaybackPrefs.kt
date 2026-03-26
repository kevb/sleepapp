package com.sleepwithme.app.data

import android.content.Context

class PlaybackPrefs(context: Context) {

    companion object {
        private const val PREFS_NAME = "playback_state"
        private const val KEY_COLLECTION_ID = "collection_id"
        private const val KEY_TRACK_INDEX = "track_index"
        private const val KEY_POSITION_MS = "position_ms"
        private const val KEY_TIMER_DURATION_MINS = "timer_duration_mins"
        const val DEFAULT_TIMER_MINS = 30
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var collectionId: String?
        get() = prefs.getString(KEY_COLLECTION_ID, null)
        set(value) = prefs.edit().putString(KEY_COLLECTION_ID, value).apply()

    var trackIndex: Int
        get() = prefs.getInt(KEY_TRACK_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_TRACK_INDEX, value).apply()

    var positionMs: Long
        get() = prefs.getLong(KEY_POSITION_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_POSITION_MS, value).apply()

    var timerDurationMins: Int
        get() = prefs.getInt(KEY_TIMER_DURATION_MINS, DEFAULT_TIMER_MINS)
        set(value) = prefs.edit().putInt(KEY_TIMER_DURATION_MINS, value).apply()

    fun savePosition(collectionId: String, trackIndex: Int, positionMs: Long) {
        prefs.edit()
            .putString(KEY_COLLECTION_ID, collectionId)
            .putInt(KEY_TRACK_INDEX, trackIndex)
            .putLong(KEY_POSITION_MS, positionMs)
            .apply()
    }
}

package com.dante.volguard

import android.media.AudioManager
import android.os.Build

/**
 * Every audio stream the app can guard. RING and NOTIF are separate so that
 * Xiaomi/MIUI/HyperOS (which splits them) can be controlled independently.
 */
enum class Channel(
    val key: String,
    val label: String,
    val stream: Int,
    val tileBg: Long,
    val tileFg: Long,
) {
    MEDIA("media", "Media", AudioManager.STREAM_MUSIC, 0xFF165A2E, 0xFF8FE0A8),
    CALL("call", "Call", AudioManager.STREAM_VOICE_CALL, 0xFF1F4E5F, 0xFF9AD8EE),
    RING("ring", "Ringtone", AudioManager.STREAM_RING, 0xFF5B3A12, 0xFFFFC77A),
    NOTIF("notif", "Notification", AudioManager.STREAM_NOTIFICATION, 0xFF3A3F5C, 0xFFC9D0FF),
    ALARM("alarm", "Alarm", AudioManager.STREAM_ALARM, 0xFF8C1C1C, 0xFFFFDAD6),
}

fun AudioManager.minVol(ch: Channel): Int =
    if (Build.VERSION.SDK_INT >= 28) getStreamMinVolume(ch.stream)
    else if (ch == Channel.CALL) 1 else 0

fun AudioManager.maxVol(ch: Channel): Int = getStreamMaxVolume(ch.stream)

/** Returns false if the system refused (e.g. missing Do Not Disturb access). */
fun AudioManager.setSafe(ch: Channel, value: Int): Boolean = try {
    setStreamVolume(ch.stream, value.coerceIn(minVol(ch), maxVol(ch)), 0)
    true
} catch (e: SecurityException) {
    false
}

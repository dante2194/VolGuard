package com.dante.volguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat

class GuardService : Service() {

    private lateinit var am: AudioManager
    private val handler = Handler(Looper.getMainLooper())

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            enforce()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            enforce()
        }
    }

    // Safety net in case a ROM swallows a volume broadcast.
    private val tick = object : Runnable {
        override fun run() {
            enforce()
            handler.postDelayed(this, 1500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        startInForeground()

        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)

        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Prefs.anyLocked(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        enforce()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { contentResolver.unregisterContentObserver(observer) } catch (_: Exception) {}
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun enforce() {
        for (ch in Channel.entries) {
            if (!Prefs.locked(this, ch)) continue
            val cur = am.getStreamVolume(ch.stream)
            val target = Prefs.target(this, ch, cur)
            val lowered = cur < target && !Prefs.allowLower(this, ch)
            val raised = cur > target && !Prefs.allowRaise(this, ch)
            if (lowered || raised) am.setSafe(ch, target)
        }
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_ID, "Volume guard", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(this, CH_ID)
            .setSmallIcon(R.drawable.ic_stat_lock)
            .setContentTitle("VolGuard is guarding your volume")
            .setContentText("Tap to change locks")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    companion object {
        private const val CH_ID = "guard"
        private const val NOTIF_ID = 1

        /** Starts the service if anything is locked, stops it otherwise. */
        fun sync(c: Context) {
            val i = Intent(c, GuardService::class.java)
            if (Prefs.anyLocked(c)) ContextCompat.startForegroundService(c, i)
            else c.stopService(i)
        }
    }
}

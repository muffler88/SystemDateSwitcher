package com.example.dateswitcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DateSwitchService : Service() {
    companion object {
        const val ACTION_START = "com.example.dateswitcher.START"
        const val ACTION_STOP = "com.example.dateswitcher.STOP"
        const val EXTRA_START = "startMillis"
        const val EXTRA_END = "endMillis"
        const val EXTRA_INTERVAL = "intervalMillis"
        private const val CHANNEL_ID = "date_switcher"
        private const val NOTIFICATION_ID = 1001
    }

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var currentMillis = 0L
    private var endMillis = 0L
    private var intervalMillis = 1000L
    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName

    override fun onCreate() {
        super.onCreate()
        dpm = getSystemService(DevicePolicyManager::class.java)
        admin = ComponentName(this, DateAdminReceiver::class.java)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSwitch()
            ACTION_START -> {
                currentMillis = intent.getLongExtra(EXTRA_START, 0L)
                endMillis = intent.getLongExtra(EXTRA_END, 0L)
                intervalMillis = intent.getLongExtra(EXTRA_INTERVAL, 1000L).coerceAtLeast(100L)
                startForeground(NOTIFICATION_ID, notification("Запуск..."))
                startSwitch()
            }
        }
        return START_NOT_STICKY
    }

    private fun startSwitch() {
        if (!dpm.isDeviceOwnerApp(packageName)) {
            updateNotification("Нет прав Device Owner")
            stopSelf()
            return
        }
        try { dpm.setAutoTimeEnabled(admin, false) } catch (_: Exception) { }
        running = true
        handler.removeCallbacksAndMessages(null)
        tick()
    }

    private fun tick() {
        if (!running) return
        if (currentMillis > endMillis) {
            updateNotification("Завершено")
            stopSelf()
            return
        }
        val ok = try { dpm.setTime(admin, currentMillis) } catch (_: Exception) { false }
        val text = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(currentMillis)
        updateNotification(if (ok) "Дата: $text" else "Ошибка установки: $text")
        if (!ok) {
            running = false
            stopSelf()
            return
        }
        val c = Calendar.getInstance().apply { timeInMillis = currentMillis; add(Calendar.DAY_OF_MONTH, 1) }
        currentMillis = c.timeInMillis
        handler.postDelayed({ tick() }, intervalMillis)
    }

    private fun stopSwitch() {
        running = false
        handler.removeCallbacksAndMessages(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "System Date Switcher", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notification(text: String): Notification {
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentTitle("System Date Switcher")
                .setContentText(text)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentTitle("System Date Switcher")
                .setContentText(text)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification(text: String) {
        if (running || !isFinishing) {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
        }
    }
}

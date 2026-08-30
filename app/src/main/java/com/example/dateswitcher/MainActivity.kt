package com.example.dateswitcher

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var start: EditText
    private lateinit var end: EditText
    private lateinit var interval: EditText
    private lateinit var status: TextView
    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(DevicePolicyManager::class.java)
        admin = ComponentName(this, DateAdminReceiver::class.java)

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        start = field("Начальная дата (ДД.ММ.ГГГГ)", "01.01.1980")
        end = field("Дата остановки (ДД.ММ.ГГГГ)", "31.12.1981")
        interval = field("Интервал между датами, секунд", "1")
        box.addView(start)
        box.addView(end)
        box.addView(interval)

        val owner = Button(this).apply {
            text = "1. ПРОВЕРИТЬ / НАСТРОИТЬ DEVICE OWNER"
            setOnClickListener { setupDeviceOwner() }
        }
        val startBtn = Button(this).apply {
            text = "2. СТАРТ"
            setOnClickListener { startSwitch() }
        }
        val stopBtn = Button(this).apply {
            text = "СТОП"
            setOnClickListener { stopSwitch() }
        }
        status = TextView(this).apply { text = statusText() }
        box.addView(owner)
        box.addView(startBtn)
        box.addView(stopBtn)
        box.addView(status)
        setContentView(box)
    }

    private fun field(hint: String, value: String) = EditText(this).apply {
        this.hint = hint
        setText(value)
        inputType = 1
        setPadding(0, 16, 0, 16)
    }

    private fun setupDeviceOwner() {
        if (dpm.isDeviceOwnerApp(packageName)) {
            status.text = "Device Owner уже настроен. Можно запускать."
            return
        }
        status.text = "Установите приложение Device Owner через ADB, затем нажмите эту кнопку снова.\n\nADB-команда:\nadb shell dpm set-device-owner com.example.dateswitcher/.DateAdminReceiver"
    }

    private fun startSwitch() {
        try {
            val s = parseDate(start.text.toString())
            val e = parseDate(end.text.toString())
            require(e >= s) { "Конечная дата раньше начальной" }
            val seconds = interval.text.toString().trim().toLong().coerceAtLeast(1L)
            if (!dpm.isDeviceOwnerApp(packageName)) {
                status.text = "Сначала назначьте приложение Device Owner через ADB."
                return
            }
            val intent = Intent(this, DateSwitchService::class.java).apply {
                action = DateSwitchService.ACTION_START
                putExtra(DateSwitchService.EXTRA_START, s)
                putExtra(DateSwitchService.EXTRA_END, e)
                putExtra(DateSwitchService.EXTRA_INTERVAL, seconds * 1000L)
            }
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
            status.text = "Служба запущена."
        } catch (_: Exception) {
            status.text = "Ошибка ввода даты или интервала."
        }
    }

    private fun stopSwitch() {
        startService(Intent(this, DateSwitchService::class.java).setAction(DateSwitchService.ACTION_STOP))
        status.text = "Остановлено"
    }

    private fun parseDate(text: String): Long {
        val d = format.parse(text) ?: throw IllegalArgumentException()
        val c = Calendar.getInstance().apply {
            time = d
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    private fun statusText(): String = if (dpm.isDeviceOwnerApp(packageName)) {
        "Device Owner: ДА\nГотово к запуску."
    } else {
        "Device Owner: НЕТ\nПосле установки APK назначьте его Device Owner через ADB."
    }
}

package com.example.dateswitcher

import android.app.*
import android.os.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var current = Calendar.getInstance()
    private lateinit var start: EditText
    private lateinit var end: EditText
    private lateinit var interval: EditText
    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val box = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        start = field("Начальная дата (ДД.ММ.ГГГГ)", "01.01.1980")
        end = field("Конечная дата (ДД.ММ.ГГГГ)", "31.12.1981")
        interval = field("Интервал, секунд", "1")
        val startBtn=Button(this).apply{text="СТАРТ"; setOnClickListener{startSwitch()}}
        val stopBtn=Button(this).apply{text="СТОП"; setOnClickListener{stopSwitch()}}
        status=TextView(this).apply{text="Остановлено"}
        box.addView(start); box.addView(end); box.addView(interval); box.addView(startBtn); box.addView(stopBtn); box.addView(status)
        setContentView(box)
    }

    private fun field(h:String,v:String)=EditText(this).apply{hint=h; setText(v); inputType=2}

    private fun startSwitch() {
        try {
            current = Calendar.getInstance().apply {
                time=SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(start.text.toString())!!
                set(Calendar.HOUR_OF_DAY,12); set(Calendar.MINUTE,0); set(Calendar.SECOND,0)
            }
            val finish=SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(end.text.toString())!!
            val ms=interval.text.toString().toLong()*1000L
            running=true
            fun tick() {
                if(!running || current.time.after(finish)) { running=false; status.text="Завершено"; return }
                val ok=trySetTime(current.timeInMillis)
                status.text="Дата: ${SimpleDateFormat("dd.MM.yyyy",Locale.getDefault()).format(current.time)}  ${if(ok)"OK" else "Нет прав"}"
                current.add(Calendar.DAY_OF_MONTH,1)
                if(running) handler.postDelayed({tick()}, ms)
            }
            tick()
        } catch(e:Exception) { status.text="Ошибка ввода даты/интервала" }
    }
    private fun stopSwitch(){running=false; handler.removeCallbacksAndMessages(null); status.text="Остановлено"}
    private fun trySetTime(ms:Long):Boolean {
        return try { SystemClock.setCurrentTimeMillis(ms); true } catch(e:SecurityException){false} catch(e:Exception){false}
    }
}

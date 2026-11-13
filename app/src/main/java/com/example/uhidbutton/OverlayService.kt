package com.example.uhidbutton

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class OverlayService : Service() {
    private val TAG = "OverlayService"
    private lateinit var wm: WindowManager
    private var view: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        startForeground(1, buildNotification())
        showOverlay()
        return START_STICKY
    }

    private fun showOverlay() {
        if (view != null) {
            Log.d(TAG, "view already exists")
            return
        }
        val density = resources.displayMetrics.density
        val size = (72 * density).toInt()

        val tv = TextView(this).apply {
            text = "␣"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#2196F3"))
                setStroke((2 * density).toInt(), Color.WHITE)
            }
            setOnClickListener {
                Log.d(TAG, "click: launching TapOnce")
                Toast.makeText(this@OverlayService, "Space ␣", Toast.LENGTH_SHORT).show()
                try {
                    Shizuku.newProcess(
                        arrayOf(
                            "app_process",
                            "-Djava.class.path=${packageCodePath}",
                            "/system/bin",
                            "--nice-name=tap_once",
                            "com.example.uhidbutton.TapOnce",
                            android.view.KeyEvent.KEYCODE_SPACE.toString()
                        ),
                        null, null
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "newProcess failed", e)
                    Toast.makeText(this@OverlayService, "err: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        val params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        try {
            wm.addView(tv, params)
            view = tv
            Log.d(TAG, "wm.addView OK")
        } catch (e: Exception) {
            Log.e(TAG, "wm.addView FAILED", e)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("ov", "Overlay", NotificationManager.IMPORTANCE_MIN)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, "ov")
        else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setContentTitle("UHID Button")
            .setContentText("Кнопка пробела активна")
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .build()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        super.onDestroy()
    }
}

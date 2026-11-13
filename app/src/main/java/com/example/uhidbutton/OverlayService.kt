package com.example.uhidbutton

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var view: View? = null
    private var injector: IKeyInjector? = null
    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val container = intent.getParcelableExtra("binder", BinderContainer::class.java)
                ?: return
            val b = container.binder
            if (!b.pingBinder()) return
            injector = IKeyInjector.Stub.asInterface(b)
            KeyInjector.binder = injector
            KeyInjector.ready = true
            showOverlay()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        val filter = IntentFilter("intent.uhidbutton.sendBinder")
        ContextCompat.registerReceiver(
            this, receiver, filter, ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        return START_STICKY
    }

    private fun showOverlay() {
        if (view != null) return
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
                try {
                    injector?.tapSpace()
                    Toast.makeText(this@OverlayService, "Space ␣", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@OverlayService,
                        "err: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // TYPE_APPLICATION_OVERLAY требует разрешения SYSTEM_ALERT_WINDOW
        // (оно уже запрашивается в MainActivity через askOverlay()).
        // TYPE_ACCESSIBILITY_OVERLAY здесь НЕ подходит — он доступен
        // только реальным AccessibilityService.
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

        wm.addView(tv, params)
        view = tv
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
        if (receiverRegistered) {
            try { unregisterReceiver(receiver) } catch (_: Exception) {}
        }
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        try { injector?.close() } catch (_: Exception) {}
        super.onDestroy()
    }
}

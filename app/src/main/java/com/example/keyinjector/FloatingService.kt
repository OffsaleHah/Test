package com.example.keyinjector

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingButton: Button

    // Какая клавиша отправляется при нажатии кнопки — меняйте здесь
    private val targetKeyCode = KeyEvent.KEYCODE_SPACE

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingButton = Button(this).apply { text = "⌨" }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 400

        // Тап — шлёт клавишу; перетаскивание двигает кнопку
        floatingButton.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> true
                MotionEvent.ACTION_UP -> {
                    Injector.keyDown(targetKeyCode)
                    Injector.keyUp(targetKeyCode)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = event.rawX.toInt() - floatingButton.width / 2
                    params.y = event.rawY.toInt() - floatingButton.height / 2
                    windowManager.updateViewLayout(floatingButton, params)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(floatingButton, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingButton.isInitialized) {
            windowManager.removeView(floatingButton)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

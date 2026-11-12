package com.example.keyinjector

import android.os.IBinder
import android.os.SystemClock
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

object Injector {
    private var inputManager: Any? = null
    private var injectMethod: Method? = null

    private fun ensure() {
        if (injectMethod != null) return
        val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("input"))
        val clazz = Class.forName("android.hardware.input.IInputManager")
        inputManager = clazz.getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder)
        injectMethod = clazz.getMethod(
            "injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType
        )
    }

    private fun inject(event: InputEvent) {
        ensure()
        injectMethod!!.invoke(inputManager, event, 0) // 0 = MODE_ASYNC
    }

    fun keyDown(keyCode: Int) {
        inject(KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            KeyEvent.ACTION_DOWN, keyCode, 0))
    }

    fun keyUp(keyCode: Int) {
        inject(KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP, keyCode, 0))
    }

    fun tap(x: Float, y: Float) {
        val down = MotionEvent.obtain(0, SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN, x, y, 0)
        inject(down); down.recycle()
        val up = MotionEvent.obtain(0, SystemClock.uptimeMillis(),
            MotionEvent.ACTION_UP, x, y, 0)
        inject(up); up.recycle()
    }
}

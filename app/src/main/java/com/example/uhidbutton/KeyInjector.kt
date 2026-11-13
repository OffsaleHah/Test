package com.example.uhidbutton

import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

object KeyEventInjector {
    private const val TAG = "KeyEventInjector"
    private var inputManager: Any? = null
    private var injectMethod: Method? = null
    private var injectMethodTakesUid = false

    fun prepare(): Boolean {
        if (inputManager != null && injectMethod != null) return true
        return try {
            // Обход скрытых API (на Android 10+ интерфейс IInputManager скрыт)
            runCatching {
                val c = Class.forName("org.lsposed.hiddenapibypass.HiddenApiBypass")
                c.getMethod("addHiddenApiExemptions", Array<String>::class.java)
                    .invoke(null, arrayOf("L"))
            }

            // 1. Binder системного сервиса "input"
            val inputBinder: IBinder = SystemServiceHelper.getSystemService("input")

            // 2. Получаем прокси IInputManager.
            //    Процесс уже работает под shell (UID 2000), а у shell есть
            //    разрешение INJECT_EVENTS — поэтому прямой вызов Binder сработает.
            val stubClass = Class.forName("android.hardware.input.IInputManager\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            inputManager = asInterface.invoke(null, inputBinder)

            // 3. Ищем injectInputEvent — сигнатура зависит от версии Android
            val methods = inputManager!!.javaClass.methods.filter { it.name == "injectInputEvent" }
            for (m in methods) {
                if (m.parameterTypes.size == 3) {
                    injectMethod = m
                    injectMethodTakesUid = true
                    break
                }
            }
            if (injectMethod == null) {
                for (m in methods) {
                    if (m.parameterTypes.size == 2) {
                        injectMethod = m
                        injectMethodTakesUid = false
                        break
                    }
                }
            }
            if (injectMethod == null) throw NoSuchMethodException("injectInputEvent not found")

            Log.i(TAG, "Ready. takesUid=$injectMethodTakesUid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "prepare() failed", e)
            false
        }
    }

    fun injectKey(
        action: Int,
        keyCode: Int,
        metaState: Int = 0,
        source: Int = -1,
        downTime: Long = SystemClock.uptimeMillis()
    ): Boolean {
        if (!prepare()) return false
        val event = KeyEvent(
            downTime, downTime, action, keyCode, 0, metaState,
            -1, 0, KeyEvent.FLAG_FROM_SYSTEM, source
        )
        return try {
            val result = if (injectMethodTakesUid) {
                // Android 14+: (event, mode, targetUid). -1 = текущий пользователь
                injectMethod!!.invoke(inputManager, event, 0, -1)
            } else {
                // Android ≤ 13: (event, mode). 0 = INJECT_INPUT_EVENT_MODE_ASYNC
                injectMethod!!.invoke(inputManager, event, 0)
            }
            result !is Boolean || result == true
        } catch (e: Exception) {
            Log.e(TAG, "injectInputEvent failed", e)
            false
        }
    }

    fun tapKey(keyCode: Int, holdMs: Long = 50) {
        val downTime = SystemClock.uptimeMillis()
        injectKey(KeyEvent.ACTION_DOWN, keyCode, downTime = downTime)
        Thread.sleep(holdMs)
        injectKey(KeyEvent.ACTION_UP, keyCode, downTime = downTime)
    }
}

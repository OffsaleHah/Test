package com.example.uhidbutton

import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import rikka.shizuku.ShizukuBinderWrapper
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
            // 1. Получаем IBinder системного сервиса "input"
            val inputBinder: IBinder = SystemServiceHelper.getSystemService("input")
            // 2. Оборачиваем его через Shizuku, чтобы вызовы шли от имени shell (UID 2000)
            val wrappedBinder = ShizukuBinderWrapper(inputBinder)
            // 3. Получаем прокси-объект IInputManager
            val stubClass = Class.forName("android.hardware.input.IInputManager\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            inputManager = asInterface.invoke(null, wrappedBinder)

            // 4. Ищем метод injectInputEvent (количество аргументов зависит от версии Android)
            val methods = inputManager!!.javaClass.methods.filter { it.name == "injectInputEvent" }
            for (m in methods) {
                if (m.parameterTypes.size == 2) { // API <= 33
                    injectMethod = m
                    injectMethodTakesUid = false
                    break
                } else if (m.parameterTypes.size == 3) { // API >= 34
                    injectMethod = m
                    injectMethodTakesUid = true
                    break
                }
            }
            if (injectMethod == null) throw NoSuchMethodException("injectInputEvent not found")
            Log.i(TAG, "KeyEventInjector prepared, method takes UID: $injectMethodTakesUid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare KeyEventInjector", e)
            false
        }
    }

    fun injectKey(action: Int, keyCode: Int, metaState: Int = 0, source: Int = -1, downTime: Long = SystemClock.uptimeMillis()): Boolean {
        if (!prepare()) return false
        val event = KeyEvent(downTime, downTime, action, keyCode, 0, metaState, -1, 0, KeyEvent.FLAG_FROM_SYSTEM, source)
        return try {
            val result = if (injectMethodTakesUid) {
                // Для API 34+: (event, mode, targetUid). INVALID_UID = -1
                injectMethod!!.invoke(inputManager, event, 0, -1)
            } else {
                // Для API <= 33: (event, mode). 0 = INJECT_INPUT_EVENT_MODE_ASYNC
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

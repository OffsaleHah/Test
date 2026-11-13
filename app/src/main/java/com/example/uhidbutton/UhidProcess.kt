package com.example.uhidbutton

import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent

object UhidProcess {
    private const val TAG = "UhidProcess"
    private const val ACTION = "intent.uhidbutton.sendBinder"

    @JvmStatic
    fun main(args: Array<String>) {
        val uid = android.os.Process.myUid()
        if (uid != 2000 && uid != 0) {
            System.err.println("Need shell (2000) or root (0), got $uid")
            System.exit(1)
        }
        Log.i(TAG, "Starting KeyEventInjector process, uid=$uid")

        if (!KeyEventInjector.prepare()) {
            Log.e(TAG, "KeyEventInjector.prepare() failed")
            System.exit(2)
        }
        Log.i(TAG, "KeyEventInjector is ready")

        val binder: IBinder = object : IKeyInjector.Stub() {
            override fun tapSpace() {
                KeyEventInjector.tapKey(KeyEvent.KEYCODE_SPACE)
            }
            override fun keyDown(keyCode: Int) {
                KeyEventInjector.injectKey(KeyEvent.ACTION_DOWN, keyCode)
            }
            override fun keyUp(keyCode: Int) {
                KeyEventInjector.injectKey(KeyEvent.ACTION_UP, keyCode)
            }
            override fun close() {
                // Нет UHID — закрывать нечего
            }
        }

        sendBinderByStickyBroadcast(binder)

        // Держим процесс живым, пока не убьют
        try {
            while (true) Thread.sleep(60_000)
        } catch (_: InterruptedException) {
        }
    }

    private fun sendBinderByStickyBroadcast(binder: IBinder) {
        try {
            val intent = Intent(ACTION).apply {
                putExtra("binder", BinderContainer(binder))
            }

            val activityBinder = Class.forName("android.os.ServiceManager")
                .getDeclaredMethod("getService", String::class.java)
                .invoke(null, "activity") as IBinder

            val iActivityManager = Class.forName("android.app.IActivityManager\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(null, activityBinder)
                ?: throw IllegalStateException("IActivityManager is null")

            // Ищем broadcastIntent по имени и числу аргументов (13),
            // чтобы не зависеть от скрытых IApplicationThread / IIntentReceiver
            val broadcastIntent = iActivityManager.javaClass.methods.firstOrNull {
                it.name == "broadcastIntent" && it.parameterCount == 13
            } ?: throw NoSuchMethodException("broadcastIntent/13 not found")

            broadcastIntent.isAccessible = true
            broadcastIntent.invoke(
                iActivityManager,
                null, intent, null, null, -1, null, null, null,
                0, null, false, /*sticky=*/ true, -1
            )
            Log.i(TAG, "Binder broadcast sent")
        } catch (e: Exception) {
            Log.e(TAG, "Broadcast failed", e)
            System.exit(-1)
        }
    }
}

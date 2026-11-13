package com.example.uhidbutton

import android.os.IBinder
import android.view.KeyEvent
import android.util.Log

object UhidProcess {
    private const val TAG = "UhidProcess"

    @JvmStatic
    fun main(args: Array<String>) {
        Log.i(TAG, "KeyEvent injector process started (uid=${android.os.Process.myUid()})")

        if (!KeyEventInjector.prepare()) {
            Log.e(TAG, "Failed to prepare KeyEventInjector")
            System.exit(2)
        }

        val binder: IBinder = object : IKeyInjector.Stub() {
            override fun tapSpace() {
                KeyEventInjector.tapKey(KeyEvent.KEYCODE_SPACE)
            }
            override fun keyDown(hid: Int) {
                KeyEventInjector.injectKey(KeyEvent.ACTION_DOWN, hid)
            }
            override fun keyUp(hid: Int) {
                KeyEventInjector.injectKey(KeyEvent.ACTION_UP, hid)
            }
            override fun close() { /* Нет UHID для закрытия */ }
        }
        // ... остальная часть метода main (broadcast и удержание процесса) без изменений
    }
    // ... метод sendBinderByStickyBroadcast без изменений
}

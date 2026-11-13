package com.example.uhidbutton

import android.util.Log
import android.view.KeyEvent

object TapOnce {
    private const val TAG = "TapOnce"

    @JvmStatic
    fun main(args: Array<String>) {
        Log.i(TAG, "TapOnce started, uid=${android.os.Process.myUid()}")
        val keyCode = args.getOrNull(0)?.toIntOrNull() ?: KeyEvent.KEYCODE_SPACE
        if (!KeyEventInjector.prepare()) {
            Log.e(TAG, "prepare() failed")
            return
        }
        KeyEventInjector.tapKey(keyCode)
        Log.i(TAG, "tap sent, key=$keyCode")
    }
}

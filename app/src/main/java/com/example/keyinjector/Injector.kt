package com.example.keyinjector

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

object Injector {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var appContext: Context? = null

    private fun showError(e: Throwable) {
        mainHandler.post {
            try {
                Toast.makeText(
                    appContext,
                    "Ошибка: ${e.javaClass.simpleName}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Throwable) { }
        }
    }

    fun sendKey(keyCode: Int) {
        executor.execute {
            try {
                val process = Shizuku.newProcess(
                    arrayOf("input", "keyevent", keyCode.toString()), null, null
                )
                process.waitFor()
            } catch (e: Throwable) {
                showError(e)
            }
        }
    }

    fun tap(x: Int, y: Int) {
        executor.execute {
            try {
                val process = Shizuku.newProcess(
                    arrayOf("input", "tap", x.toString(), y.toString()), null, null
                )
                process.waitFor()
            } catch (e: Throwable) {
                showError(e)
            }
        }
    }
}

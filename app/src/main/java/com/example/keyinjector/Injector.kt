package com.example.keyinjector

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import moe.shizuku.server.IShizukuService
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
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

    private fun runCommand(cmd: Array<String>) {
        executor.execute {
            try {
                // Берём у Shizuku его "shell"-интерфейс и запускаем системную команду
                val binder = ShizukuBinderWrapper(
                    SystemServiceHelper.getSystemService("shizuku")
                )
                val service = IShizukuService.Stub.asInterface(binder)
                val process = service.newProcess(cmd, null, null)
                process.waitFor()
            } catch (e: Throwable) {
                showError(e)
            }
        }
    }

    fun sendKey(keyCode: Int) {
        runCommand(arrayOf("input", "keyevent", keyCode.toString()))
    }

    fun tap(x: Int, y: Int) {
        runCommand(arrayOf("input", "tap", x.toString(), y.toString()))
    }
}

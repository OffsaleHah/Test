package com.example.uhidbutton

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuHelper {
    const val REQ_CODE = 1001

    fun ensurePermission() {
        if (!Shizuku.pingBinder()) return
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQ_CODE)
        }
    }

    fun startUhidProcess(context: Context) {
        if (!Shizuku.pingBinder()) error("Shizuku не запущен")
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
            error("Нет разрешения Shizuku")

        Shizuku.newProcess(
            arrayOf(
                "app_process",
                "-Djava.class.path=${context.packageCodePath}",
                "/system/bin",
                "--nice-name=uhid_process",
                "com.example.uhidbutton.UhidProcess"
            ),
            null, null
        )
    }
}

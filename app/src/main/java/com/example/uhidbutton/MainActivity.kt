package com.example.uhidbutton

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, grant ->
        val ok = grant == PackageManager.PERMISSION_GRANTED
        Toast.makeText(
            this,
            if (ok) "Shizuku: разрешено" else "Shizuku: отклонено",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)

        // Запрашиваем разрешение Shizuku (оно понадобится для запуска TapOnce)
        Shizuku.addRequestPermissionResultListener(permListener)
        ShizukuHelper.ensurePermission()

        // Кнопка выдачи разрешения "Поверх других приложений"
        findViewById<Button>(R.id.btnOverlay).setOnClickListener { askOverlay() }

        // Кнопка Start — запускает OverlayService с плавающей кнопкой
        findViewById<Button>(R.id.btnStart).setOnClickListener {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(Intent(this, OverlayService::class.java))
                else
                    startService(Intent(this, OverlayService::class.java))

                Toast.makeText(
                    this,
                    "Кнопка включена. Можно открывать Roblox.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Кнопка Stop — останавливает сервис
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Остановлено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun askOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            Toast.makeText(this, "Overlay уже разрешён", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        super.onDestroy()
    }
}

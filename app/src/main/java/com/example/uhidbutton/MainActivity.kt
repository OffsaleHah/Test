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
    private val reqOverlay = 100

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, grant ->
        val ok = grant == PackageManager.PERMISSION_GRANTED
        Toast.makeText(this, if (ok) "Shizuku: разрешено" else "Shizuku: отклонено",
            Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        Shizuku.addRequestPermissionResultListener(permListener)
        ShizukuHelper.ensurePermission()

        findViewById<Button>(R.id.btnOverlay).setOnClickListener { askOverlay() }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            try {
                ShizukuHelper.startUhidProcess(this)
                startForegroundService(Intent(this, OverlayService::class.java))
                Toast.makeText(this, "Запущено. Откройте Roblox.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            try { KeyInjector.binder?.close() } catch (_: Exception) {}
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

package com.example.keyinjector

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            startFloating()
        } else {
            Toast.makeText(this, "Доступ Shizuku не выдан", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Shizuku.addRequestPermissionResultListener(listener)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            // 1. Overlay-разрешение
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
                Toast.makeText(this, "Разрешите «Поверх других окон»", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 2. Проверяем, что Shizuku запущен
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this,
                    "Запустите приложение Shizuku (adb или беспроводная отладка)",
                    Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            // 3. Shizuku-разрешение
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                startFloating()
            } else {
                Shizuku.requestPermission(0)
            }
        }
    }

    private fun startFloating() {
        startService(Intent(this, FloatingService::class.java))
        Toast.makeText(this, "Кнопка запущена", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(listener)
    }
}

package com.example.floatrotate

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusOverlay: TextView
    private lateinit var statusWriteSettings: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusOverlay = findViewById(R.id.status_overlay)
        statusWriteSettings = findViewById(R.id.status_write_settings)

        findViewById<Button>(R.id.btn_overlay).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        findViewById<Button>(R.id.btn_write_settings).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            if (!hasOverlayPermission() || !hasWriteSettingsPermission()) {
                AlertDialog.Builder(this)
                    .setTitle("权限不足 / Permissions required")
                    .setMessage("请先开启「悬浮窗」和「修改系统设置」两个权限。\nPlease grant both \"Display over other apps\" and \"Modify system settings\" first.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }
            ContextCompat.startForegroundService(
                this,
                Intent(this, FloatingService::class.java)
            )
        }

        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            stopService(Intent(this, FloatingService::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        statusOverlay.text =
            "悬浮窗权限 / Overlay: " + if (hasOverlayPermission()) "已授权 ✓" else "未授权 ✗"
        statusWriteSettings.text =
            "修改系统设置 / Write settings: " + if (hasWriteSettingsPermission()) "已授权 ✓" else "未授权 ✗"
    }

    private fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(this)
    private fun hasWriteSettingsPermission(): Boolean = Settings.System.canWrite(this)
}

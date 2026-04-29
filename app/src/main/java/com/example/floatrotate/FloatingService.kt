package com.example.floatrotate

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundWithNotification()
        showFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun showFloatingButton() {
        if (floatingView != null) return
        val view = LayoutInflater.from(this).inflate(R.layout.floating_button, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private val touchSlop = 24

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                            isDragging = true
                        }
                        if (isDragging) {
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager.updateViewLayout(view, params)
                            } catch (_: Exception) {
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            v.performClick()
                            toggleRotation()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(view, params)
        floatingView = view
    }

    private fun toggleRotation() {
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            )

            val current = Settings.System.getInt(
                contentResolver,
                Settings.System.USER_ROTATION,
                Surface.ROTATION_0
            )

            val next = when (current) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> Surface.ROTATION_90
                else -> Surface.ROTATION_0
            }

            Settings.System.putInt(
                contentResolver,
                Settings.System.USER_ROTATION,
                next
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startForegroundWithNotification() {
        val channelId = "float_rotate_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "悬浮按钮服务 / Floating button",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FloatRotate 运行中")
            .setContentText("点悬浮按钮可切换横竖屏 / Tap to toggle orientation")
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        floatingView = null
    }
}

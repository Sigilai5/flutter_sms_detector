package com.example.sms_overlay_app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.NotificationCompat
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var imm: InputMethodManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_layout, null)
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        setupWindowManager()
        setupClickListeners()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "overlay_channel",
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "overlay_channel")
            .setContentTitle("SMS Detection Active")
            .setContentText("Monitoring for SMS messages")
            .setSmallIcon(R.drawable.launch_background)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupWindowManager() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        }

        windowManager.addView(overlayView, params)
    }

    private fun setupClickListeners() {
        overlayView.findViewById<View>(R.id.dismiss).setOnClickListener {
            imm.hideSoftInputFromWindow(overlayView.windowToken, 0)
            stopSelf()
        }

        overlayView.findViewById<View>(R.id.submit).setOnClickListener {
            imm.hideSoftInputFromWindow(overlayView.windowToken, 0)

            val feedback = overlayView.findViewById<EditText>(R.id.feedback).text.toString()
            Log.d("OverlayService", "User submitted feedback: $feedback")

            // Send feedback to Flutter via MethodChannel
            val engine = FlutterEngineCache.getInstance().get("my_engine_id")
            if (engine == null) {
                Log.e("OverlayService", "FlutterEngine is null. Did you cache it in MainActivity?")
            } else {
                MethodChannel(engine.dartExecutor.binaryMessenger, "com.example.sms_overlay_app")
                    .invokeMethod("onFeedbackSubmitted", feedback)
            }

            stopSelf()
        }

        overlayView.findViewById<View>(R.id.feedback).setOnClickListener {
            overlayView.findViewById<View>(R.id.feedback).requestFocus()
            imm.showSoftInput(overlayView.findViewById<View>(R.id.feedback), InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val sender = it.getStringExtra("sender") ?: "Unknown"
            val message = it.getStringExtra("message") ?: "No content"

            overlayView.findViewById<TextView>(R.id.sender).text = "From: $sender"
            overlayView.findViewById<TextView>(R.id.message).text = message

            val editText = overlayView.findViewById<EditText>(R.id.feedback)
            editText.setText(message)

            Log.d("OverlayService", "Message set in EditText: ${editText.text}")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized && ::windowManager.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}

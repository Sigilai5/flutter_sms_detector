package com.example.sms_overlay_app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.sms_overlay_app"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Cache FlutterEngine for access in OverlayService
        FlutterEngineCache.getInstance().put("my_engine_id", flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "hasOverlayPermission" -> {
                    result.success(Settings.canDrawOverlays(this))
                }
                "requestOverlayPermission" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                        result.success(true)
                    } else {
                        result.success(true)
                    }
                }
                "showOverlay" -> {
                    val sender = call.argument<String>("sender") ?: "Unknown"
                    val message = call.argument<String>("message") ?: "No content"

                    val intent = Intent(this, OverlayService::class.java).apply {
                        putExtra("sender", sender)
                        putExtra("message", message)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }
}

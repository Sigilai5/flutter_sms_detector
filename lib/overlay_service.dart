import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';

class OverlayService {
  static const MethodChannel _channel = MethodChannel('com.example.sms_overlay_app');

  // Requests overlay permission
  static Future<bool> hasOverlayPermission() async {
    try {
      final bool result = await _channel.invokeMethod('hasOverlayPermission');
      return result;
    } on PlatformException catch (e) {
      debugPrint("Error checking overlay permission: ${e.message}");
      return false;
    }
  }

  // Opens the overlay permission settings screen
  static Future<bool> requestOverlayPermission() async {
    try {
      final bool result = await _channel.invokeMethod('requestOverlayPermission');
      return result;
    } on PlatformException catch (e) {
      debugPrint("Error requesting overlay permission: ${e.message}");
      return false;
    }
  }

  // Triggers the overlay with sender and message
  static Future<void> showOverlay({
    required String sender,
    required String message,
  }) async {
    try {
      debugPrint("Showing Overlay");
      await _channel.invokeMethod('showOverlay', {
        'sender': sender,
        'message': message,
      });
    } on PlatformException catch (e) {
      debugPrint('Failed to show overlay: ${e.message}');
    }
  }

  // Initializes a listener for messages from Kotlin
  static void initializeListener() {
    _channel.setMethodCallHandler((MethodCall call) async {
      if (call.method == 'onFeedbackSubmitted') {
        final feedback = call.arguments as String;
        debugPrint("Feedback received from overlay: $feedback");

        // Handle feedback here: save to DB, update state, etc.
      }
    });
  }
}

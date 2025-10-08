import 'package:flutter/services.dart';

import 'overlay_service.dart';

class SmsReceiver {
  static const platform = MethodChannel('com.example.sms_overlay_app');

  static void initialize() {
    platform.setMethodCallHandler((call) async {
      if (call.method == 'onSmsReceived') {
        final sender = call.arguments['sender'] as String? ?? 'Unknown';
        final message = call.arguments['message'] as String? ?? 'No content';

        // Show overlay only if the sender is "MPESA"
        if (sender == "MPESA") {
          await OverlayService.showOverlay(sender: sender, message: message);
        }
      }
    });
  }
}
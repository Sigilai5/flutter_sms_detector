import 'package:flutter/material.dart';
import 'package:sms_overlay_app/permission_handler.dart';
import 'package:sms_overlay_app/overlay_service.dart'; // Import your Dart service

void main() {
  WidgetsFlutterBinding.ensureInitialized(); // Important: Ensures channels are ready

  // Set up channel listener early
  OverlayService.initializeListener();

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'SMS Detection App',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const PermissionHandler(),
    );
  }
}

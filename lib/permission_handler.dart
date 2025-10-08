import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:another_telephony/telephony.dart';
import 'overlay_service.dart';
import 'package:intl/intl.dart';

class PermissionHandler extends StatefulWidget {
  const PermissionHandler({super.key});

  @override
  State<PermissionHandler> createState() => _PermissionHandlerState();
}

class _PermissionHandlerState extends State<PermissionHandler> {
  bool _smsPermissionGranted = false;
  bool _overlayPermissionGranted = false;
  List<SmsMessage> _messages = [];
  final String _targetSender = "MPESA";
  final Telephony telephony = Telephony.instance;

  @override
  void initState() {
    super.initState();
    _checkPermissions();
  }

  Future<void> _checkPermissions() async {
    final smsStatus = await Permission.sms.status;
    setState(() {
      _smsPermissionGranted = smsStatus.isGranted;
    });

    if (!_smsPermissionGranted) {
      _requestSmsPermission();
    } else {
      await _fetchSMS();
      _checkOverlayPermission();
    }
  }

  Future<void> _requestSmsPermission() async {
    final status = await Permission.sms.request();
    setState(() {
      _smsPermissionGranted = status.isGranted;
    });

    if (_smsPermissionGranted) {
      await _fetchSMS();
      _checkOverlayPermission();
    } else {
      _showPermissionDeniedDialog();
    }
  }

  Future<void> _checkOverlayPermission() async {
    final hasOverlayPermission = await OverlayService.hasOverlayPermission();
    setState(() {
      _overlayPermissionGranted = hasOverlayPermission;
    });

    if (!_overlayPermissionGranted) {
      _showOverlayPermissionDialog();
    }
  }

  Future<void> _requestOverlayPermission() async {
    final granted = await OverlayService.requestOverlayPermission();
    setState(() {
      _overlayPermissionGranted = granted;
    });

    if (!granted) {
      _showOverlayPermissionDeniedDialog();
    }
  }

  void _showPermissionDeniedDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text('Permissions Required'),
        content: const Text('SMS permissions are required for this app to function properly.'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              _requestSmsPermission();
            },
            child: const Text('Try Again'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Exit'),
          ),
        ],
      ),
    );
  }

  void _showOverlayPermissionDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text('Overlay Permission Required'),
        content: const Text('This app needs overlay permission to show popup notifications when SMS is received.'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              _requestOverlayPermission();
            },
            child: const Text('Grant Permission'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
        ],
      ),
    );
  }

  void _showOverlayPermissionDeniedDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Overlay Permission Required'),
        content: const Text('Without overlay permission, the app cannot show popup notifications.'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              _requestOverlayPermission();
            },
            child: const Text('Try Again'),
          ),
        ],
      ),
    );
  }

  Future<void> _fetchSMS() async {
    try {
      final List<SmsMessage> messages = await telephony.getInboxSms(
        columns: [SmsColumn.ADDRESS, SmsColumn.BODY, SmsColumn.DATE],
        filter: SmsFilter.where(SmsColumn.ADDRESS).equals(_targetSender),
      );

      setState(() {
        _messages = messages;
      });
    } catch (e) {
      debugPrint("Error fetching SMS: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('SMS Detector'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _fetchSMS, // Add refresh button
          ),
        ],
      ),
      body: Column(
        children: [
          // Display messages if permissions granted
          if (_smsPermissionGranted) ...[
            const SizedBox(height: 20),
            Text(
              'Messages from $_targetSender',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 10),
            Expanded(
              child: RefreshIndicator(
                onRefresh: _fetchSMS, // Add pull-to-refresh
                child: ListView.builder(
                  itemCount: _messages.length,
                  itemBuilder: (context, index) {
                    final msg = _messages[index];
                    return ListTile(
                      title: Text(msg.body ?? 'No content'),
                      subtitle: Text(
                        msg.date != null
                            ? DateFormat('MMM dd, yyyy - hh:mm a')
                            .format(DateTime.fromMillisecondsSinceEpoch(msg.date!))
                            : 'Unknown date',
                        style: const TextStyle(fontSize: 12),
                      ),
                    );
                  },
                ),
              ),
            ),
          ],

          // Show permission button if any permission is missing
          if (!_smsPermissionGranted || !_overlayPermissionGranted)
            Padding(
              padding: const EdgeInsets.all(20),
              child: ElevatedButton(
                onPressed: _checkPermissions,
                child: const Text('Check Permissions Again'),
              ),
            ),
        ],
      ),
    );
  }
}
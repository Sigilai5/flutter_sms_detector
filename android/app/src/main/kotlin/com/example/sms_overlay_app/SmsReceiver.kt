package com.example.sms_overlay_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.sms_overlay_app.OverlayService

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            val fullMessage = smsMessages.joinToString(separator = "") { it.messageBody }
            println("Full Message: $fullMessage")

            val bundle = intent.extras
            if (bundle != null) {
                try {
                    val pdus = bundle.get("pdus") as Array<*>?
                    if (pdus != null) {
                        for (pdu in pdus) {
                            val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val format = bundle.getString("format")
                                SmsMessage.createFromPdu(pdu as ByteArray, format)
                            } else {
                                SmsMessage.createFromPdu(pdu as ByteArray)
                            }

                            val sender = smsMessage.displayOriginatingAddress
                            val message = smsMessage.messageBody

                            Log.d("SmsReceiver", "Received SMS from $sender: $message")

                            // Show overlay for all SMS messages
                            val serviceIntent = Intent(context, OverlayService::class.java).apply {
                                putExtra("sender", sender)
                                putExtra("message", fullMessage)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error processing SMS: ${e.message}")
                }
            }
        }
    }
}
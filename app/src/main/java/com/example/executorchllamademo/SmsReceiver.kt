package com.example.executorchllamademo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return
        val sender = parts[0].originatingAddress ?: "Unknown"
        val body = parts.joinToString("") { it.messageBody ?: "" }
        if (body.isBlank()) return

        val app = context.applicationContext
        val pending = goAsync()
        Thread {
            try {
                val r = getDetector(app).analyze(body)
                Log.i(TAG, "Incoming SMS from $sender score=${r.finalScore} level=${r.level}")
                if (r.isSuspicious) {
                    notifyScam(app, sender, body, r)
                }
            } catch (e: Exception) {
                Log.e(TAG, "SMS scan failed", e)
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun notifyScam(ctx: Context, sender: String, body: String, r: SmsResult) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "Scam alerts", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "On-device scam SMS warnings"
            nm.createNotificationChannel(ch)
        }
        val launch = Intent(ctx, WelcomeActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pi = PendingIntent.getActivity(ctx, System.currentTimeMillis().toInt(), launch, flags)

        val pct = (r.finalScore * 100).toInt()
        val title = "⚠️ Scam ho sakta hai ($pct%) - $sender"
        val summary = SmsAdvisor.fallbackSummary(r)
        val big = summary + "\n\nLink par click mat karo. OTP/PIN kisi ko mat batao. " +
            "Cyber crime helpline: 1930"

        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), n)
    }

    companion object {
        private const val TAG = "SmsReceiver"
        private const val CHANNEL = "scam_alerts"
        @Volatile private var detector: SmsDetector? = null

        @Synchronized
        private fun getDetector(ctx: Context): SmsDetector {
            var d = detector
            if (d == null) {
                d = SmsDetector.load(ctx)
                detector = d
            }
            return d
        }
    }
}

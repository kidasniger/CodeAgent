package com.zagent.app.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.zagent.app.MainActivity

/** Notifications de l'agent : service en cours et alertes (fin de tâche, confirmation attendue). */
object AgentNotifier {
    private const val CH_RUN = "agent_run"
    private const val CH_ALERT = "agent_alert"
    private const val ID_ALERT = 2

    /** true quand l'appli est à l'écran : pas d'alerte dans ce cas. */
    @Volatile
    var appInForeground = false

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_RUN, "Agent en cours", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Alertes de l'agent", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun openApp(ctx: Context): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun runningNotification(ctx: Context, text: String): Notification =
        Notification.Builder(ctx, CH_RUN)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("CodeAgent")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openApp(ctx))
            .build()

    fun alert(ctx: Context, title: String, text: String) {
        if (appInForeground) return
        try {
            ensureChannels(ctx)
            val n = Notification.Builder(ctx, CH_ALERT)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(openApp(ctx))
                .build()
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(ID_ALERT, n)
        } catch (e: Exception) {
            // notification refusée ou indisponible : sans conséquence pour l'agent
        }
    }
}

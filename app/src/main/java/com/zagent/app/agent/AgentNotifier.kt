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
    /** corr-097 : extra portant la conversation à rouvrir au tap. */
    const val EXTRA_CONVERSATION = "conv_id"
    private const val CH_RUN = "agent_run"
    private const val CH_ALERT = "agent_alert"
    private const val ID_ALERT = 2

    /** true quand l'appli est à l'écran : pas d'alerte dans ce cas. */
    @Volatile
    var appInForeground = false

    fun ensureChannels(ctx: Context) {
        // corr-097 : créer un canal existant est un no-op système — les réglages
        // utilisateur ne sont jamais écrasés ; noms localisés via les ressources.
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CH_RUN,
                ctx.getString(com.zagent.app.R.string.notif_channel_run),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CH_ALERT,
                ctx.getString(com.zagent.app.R.string.notif_channel_alert),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    /**
     * corr-097 — Le tap sur une notification rouvre l'application ET la
     * conversation en cours : l'identifiant est transmis en extra et lu par
     * MainActivity pour appeler switchToConversation.
     */
    private fun openApp(ctx: Context, conversationId: String? = null): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            conversationId?.let { putExtra(EXTRA_CONVERSATION, it) }
        }
        val code = conversationId?.hashCode() ?: 0
        return PendingIntent.getActivity(
            ctx, code, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun runningNotification(ctx: Context, text: String): Notification =
        Notification.Builder(ctx, CH_RUN)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("CodeAgent")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openApp(ctx))
            .build()

    fun alert(ctx: Context, title: String, text: String, conversationId: String? = null) {
        if (appInForeground) return
        // err-086 — notifications refusées : repli silencieux, l'alerte in-app
        // (statusMessage du chat) reste affichée dans tous les cas.
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.areNotificationsEnabled()) return
        try {
            ensureChannels(ctx)
            val n = Notification.Builder(ctx, CH_ALERT)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(openApp(ctx, conversationId))
                .build()
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(ID_ALERT, n)
        } catch (e: Exception) {
            // notification refusée ou indisponible : sans conséquence pour l'agent
        }
    }
}

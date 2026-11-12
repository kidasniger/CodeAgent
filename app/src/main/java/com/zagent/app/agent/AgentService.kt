package com.zagent.app.agent

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat

/** Service au premier plan : garde l'agent actif quand l'écran s'éteint ou que l'appli passe en arrière-plan. */
class AgentService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AgentNotifier.ensureChannels(this)
        val text = intent?.getStringExtra("text")
            ?: getString(com.zagent.app.R.string.notif_agent_travaille)
        try {
            // MA29 : type dataSync déclaré (manifeste) et passé explicitement ;
            // ServiceCompat ignore le type sous API 29.
            ServiceCompat.startForeground(
                this, 1, AgentNotifier.runningNotification(this, text),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "zagent:agent").apply {
                acquire(30 * 60 * 1000L)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        /** Vrai tant qu'un serveur de développement tourne : le service reste alors actif. */
        @Volatile var devServerActive = false

        fun start(ctx: Context, text: String = ctx.getString(com.zagent.app.R.string.notif_agent_travaille)) {
            try {
                ctx.startForegroundService(Intent(ctx, AgentService::class.java).putExtra("text", text))
            } catch (e: Exception) {
                // démarrage refusé : l'agent continue sans service
            }
        }

        fun stop(ctx: Context) {
            if (devServerActive) {
                start(ctx, ctx.getString(com.zagent.app.R.string.notif_serveur_dev))
                return
            }
            try {
                ctx.stopService(Intent(ctx, AgentService::class.java))
            } catch (_: Exception) {
                // arrêt refusé : rien à faire, le service se termine de toute façon
            }
        }
    }
}

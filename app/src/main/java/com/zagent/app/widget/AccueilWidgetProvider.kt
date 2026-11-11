package com.zagent.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.zagent.app.MainActivity
import com.zagent.app.R
import com.zagent.app.ui.viewmodel.MainViewModel

/**
 * feat-093 — Widget d'écran d'accueil : lance une nouvelle conversation ou
 * rouvre le dernier projet, via les mêmes actions que les raccourcis
 * d'application (MainViewModel.handleAppShortcut). Aucun traitement en fond :
 * le widget ne fait que déléguer à MainActivity.
 */
class AccueilWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_accueil)

            views.setOnClickPendingIntent(
                R.id.widget_nouveau,
                PendingIntent.getActivity(
                    context, 1,
                    Intent(context, MainActivity::class.java).apply {
                        action = MainViewModel.ACTION_SHORTCUT_NEW_CHAT
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_projet,
                PendingIntent.getActivity(
                    context, 2,
                    Intent(context, MainActivity::class.java).apply {
                        action = MainViewModel.ACTION_SHORTCUT_LAST_PROJECT
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

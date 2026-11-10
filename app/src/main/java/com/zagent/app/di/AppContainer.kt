package com.zagent.app.di

import android.app.Application
import com.zagent.app.data.ActivityRepository
import com.zagent.app.data.AiRepository
import com.zagent.app.data.ConversationRepository
import com.zagent.app.data.FileRepository
import com.zagent.app.data.SettingsManager
import com.zagent.app.data.UsageRepository

/**
 * corr-006 — Conteneur d'injection de dépendances simple.
 *
 * Avant : MainViewModel instanciait lui-même SettingsManager, AiRepository,
 * ConversationRepository, UsageRepository et ActivityRepository — impossible
 * à tester hors appareil et à remplacer par des simulations. Désormais les
 * dépôts sont créés ici en un seul exemplaire et injectés dans MainViewModel
 * (qui conserve ses propriétés par délégation, API inchangée).
 *
 * FileRepository est particulier : sa racine dépend du projet ouvert
 * (workingFolderUri), il est donc fourni par fabrique [fileRepo] — un
 * exemplaire par racine, la politique de cache restant dans MainViewModel.
 *
 * Évolution prévue : singletons applicatifs partagés entre plusieurs
 * ViewModel (aujourd'hui seul MainViewModel les consomme).
 */
class AppContainer(private val application: Application) {

    /**
     * corr-061/062 — EncryptedSharedPreferences.create est coûteux (150-300 ms).
     * Il est construit sur un thread d'arrière-plan lancé immédiatement, en
     * PARALLÈLE avec le reste du démarrage (splash, composition) ; le premier
     * accès au getter attend simplement la fin de ce travail. Le démarrage à
     * froid n'est plus séquentiel : réglages ⟂ composition.
     */
    private val settingsReady = java.util.concurrent.CountDownLatch(1)

    @Volatile
    private var _settingsManager: SettingsManager? = null

    private val settingsThread = Thread {
        try {
            _settingsManager = SettingsManager(application)
        } finally {
            settingsReady.countDown()
        }
    }.apply { name = "settings-warmup"; isDaemon = true; start() }

    val settingsManager: SettingsManager
        get() {
            settingsReady.await()
            return _settingsManager ?: SettingsManager(application)
        }

    val aiRepository = AiRepository()
    val conversationRepo = ConversationRepository(application)
    val usageRepo = UsageRepository(application)
    val activityRepo = ActivityRepository(application)

    /** Nouveau dépôt de fichiers pour la racine donnée (null = home du terminal). */
    fun fileRepo(rootPath: String?): FileRepository = FileRepository(application, rootPath)
}

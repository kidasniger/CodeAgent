package com.zagent.app.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.termux.terminal.TerminalSession

/**
 * corr-005 — La gestion des onglets du terminal quitte TerminalController.
 *
 * Ce ViewModel détient uniquement l'état et la logique d'onglets : liste
 * [tabs], onglet actif [activeTab], ouverture/fermeture/remplacement. La
 * création de la session shell elle-même reste dans TerminalController
 * (bootstrap, environnement, client d'E/S) et est injectée ici via la
 * fabrique [sessionFactory] ; si elle renvoie null (bootstrap incomplet),
 * l'onglet n'est pas créé.
 *
 * API conservée pour TerminalScreen : controller.tabs / controller.activeTab
 * (lecture et écriture) / controller.session / controller.sessionEnded.
 */
class TerminalViewModel(
    private val sessionFactory: () -> TerminalSession?
) : ViewModel() {

    /** Un onglet shell : sa session et son état (terminée ou non). */
    class ShellTab(val session: TerminalSession, val number: Int) {
        var ended by mutableStateOf(false)
        /** feat-048 — nom personnalisé de l'onglet (null = « Shell N »). */
        var nom by mutableStateOf<String?>(null)
    }

    val tabs = mutableStateListOf<ShellTab>()

    var activeTab by mutableIntStateOf(0)

    private var tabCounter = 0

    /** Session de l'onglet affiché (null si aucun onglet). */
    val session: TerminalSession?
        get() = tabs.getOrNull(activeTab)?.session

    val sessionEnded: Boolean
        get() = tabs.getOrNull(activeTab)?.ended ?: false

    /** Ouvre le premier onglet, ou relance l'onglet actif (nouvelle session). */
    fun openSession() {
        if (tabs.isEmpty()) {
            addTab()
            return
        }
        val i = activeTab.coerceIn(0, tabs.lastIndex)
        val old = tabs[i]
        val s = sessionFactory() ?: return
        old.session.finishIfRunning()
        tabs[i] = ShellTab(s, old.number)
    }

    /** Nouvel onglet shell créé par la fabrique de session du contrôleur. */
    fun addTab() {
        val s = sessionFactory() ?: return
        tabs.add(ShellTab(s, ++tabCounter))
        activeTab = tabs.lastIndex
    }

    fun closeTab(index: Int) {
        val t = tabs.getOrNull(index) ?: return
        t.session.finishIfRunning()
        val old = activeTab
        tabs.removeAt(index)
        activeTab = when {
            tabs.isEmpty() -> 0
            index < old -> old - 1
            else -> old.coerceAtMost(tabs.lastIndex)
        }
        if (tabs.isEmpty()) tabCounter = 0
    }

    /** Remplace la session de l'onglet actif sans changer sa numérotation. */
    fun replaceActiveSession(session: TerminalSession) {
        val i = activeTab.coerceIn(0, tabs.lastIndex.coerceAtLeast(0))
        if (tabs.isEmpty()) return
        val old = tabs[i]
        old.session.finishIfRunning()
        tabs[i] = ShellTab(session, old.number)
    }
}

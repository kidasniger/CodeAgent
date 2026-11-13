package com.zagent.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zagent.app.data.ConversationMeta
import com.zagent.app.data.ConversationRepository
import com.zagent.app.data.CorbeilleEntree
import com.zagent.app.data.SettingsManager
import com.zagent.app.data.StatsConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * corr-002 — La gestion des données du chat quitte MainViewModel.
 *
 * Ce ViewModel détient la liste de messages ([messages]), l'identifiant de
 * conversation courante ([conversationId]), les brouillons par conversation
 * (MA55) et l'index des conversations ([conversationList]) avec ses opérations
 * d'historique. La boucle agent (envoi, outils, mode Duo) reste dans
 * MainViewModel, qui écrit la liste via [edit] — l'API publique de
 * MainViewModel est conservée par délégation, aucun écran n'est modifié.
 */
class ChatViewModel(
    private val settings: SettingsManager,
    private val conversationRepo: ConversationRepository
) : ViewModel() {

    /** Éditable par MainViewModel (moteur agent) ; les écrans lisent [messages]. */
    internal val edit = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = edit.asStateFlow()

    /** Identifiant de la conversation courante : sert de clé au brouillon (MA55). */
    var conversationId by mutableStateOf("")
        private set

    internal fun setConversationId(id: String) { conversationId = id }

    private var draftSaveJob: Job? = null

    init {
        conversationId = settings.lastConversationId.ifBlank { UUID.randomUUID().toString() }
        settings.lastConversationId = conversationId
    }

    /** Appelé à chaque frappe : enregistre le brouillon avec un léger délai (MA55). */
    fun onInputChanged(text: String) {
        draftSaveJob?.cancel()
        val id = conversationId
        draftSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(600)
            settings.saveDraft(id, text)
        }
    }

    /** Appelé après un envoi : le brouillon de la conversation repart de zéro. */
    fun onInputSent() {
        draftSaveJob?.cancel()
        val id = conversationId
        viewModelScope.launch(Dispatchers.IO) { settings.saveDraft(id, "") }
    }

    /** Annule un enregistrement de brouillon en cours (changement de conversation). */
    fun cancelDraftSave() {
        draftSaveJob?.cancel()
    }

    /** Brouillon sauvegardé de la conversation courante (MA55). */
    fun currentDraft(): String = settings.draftFor(conversationId)

    // ── Index des conversations (MA56) ─────────────────────────────────────

    var conversationList by mutableStateOf<List<ConversationMeta>>(emptyList())
        private set

    fun refreshConversations() {
        // Session 12 — réconcilie index ↔ fichiers avant l'affichage : entrées
        // fantômes retirées, fichiers orphelis importés (aucun contenu lu si tout est sain).
        viewModelScope.launch(Dispatchers.IO) {
            val reconcilie = conversationRepo.reconcilierIndex()
            // hist-006 — les comptes de messages sont relus depuis les fichiers
            // (jamais depuis un index potentiellement périmé).
            val recompte = conversationRepo.recompterMessages(reconcilie)
            withContext(Dispatchers.Main) {
                conversationList = recompte
                orphelinsDernierScan = conversationRepo.derniersOrphelins
            }
        }
    }

    /** hist-012 — orphelins réconciliés au dernier scan (indicateur visuel de l'historique). */
    var orphelinsDernierScan by mutableStateOf(0)
        private set

    /** ia-028 — plan de projet de la conversation courante (persisté). */
    var planCourant by mutableStateOf("")
        private set

    internal fun setConversationList(list: List<ConversationMeta>) {
        conversationList = list
    }

    /**
     * Supprime une conversation de l'historique. Si c'est la conversation
     * courante, [onClearedCurrent] est appelé (MainViewModel y vide le chat).
     */
    fun deleteConversation(id: String, currentId: String, onClearedCurrent: () -> Unit) {
        if (id == currentId) onClearedCurrent()
        viewModelScope.launch(Dispatchers.IO) {
            conversationRepo.deleteMessages(id)
            val index = conversationRepo.loadIndex().filter { it.id != id }
            conversationRepo.saveIndex(index)
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    /** Épingle / désépingle une conversation (glisser vers la droite, MA56). */
    fun toggleConversationPin(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val index = conversationRepo.loadIndex().toMutableList()
            val i = index.indexOfFirst { it.id == id }
            if (i >= 0) {
                index[i] = index[i].copy(pinned = !index[i].pinned)
                conversationRepo.saveIndex(index)
            }
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    /**
     * feat-020 — Assigne (ou efface) l'étiquette / dossier d'une conversation.
     * Chaîne vide ou blanche = retire le classement.
     */
    fun setConversationEtiquette(id: String, valeur: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val index = conversationRepo.loadIndex().toMutableList()
            val i = index.indexOfFirst { it.id == id }
            if (i >= 0) {
                index[i] = index[i].copy(etiquette = valeur.trim().take(24))
                conversationRepo.saveIndex(index)
            }
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    // ── hist-003 : renommer une conversation ────────────────────────────────

    /** hist-003 — Édite le titre persisté d'une conversation (depuis l'historique). */
    fun renommerConversation(id: String, titre: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val index = conversationRepo.loadIndex().toMutableList()
            val i = index.indexOfFirst { it.id == id }
            if (i >= 0) {
                index[i] = index[i].copy(title = titre.trim().take(64).ifBlank { index[i].title })
                conversationRepo.saveIndex(index)
            }
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    // ── hist-004 : fusionner deux conversations ─────────────────────────────

    /**
     * hist-004 — Fusionne [idAutre] DANS [idCible] : messages de l'autre
     * ajoutés après ceux de la cible (ordre chronologique retrié par timestamp),
     * l'autre passe à la corbeille. Utilisé quand un fil de travail a éclaté.
     */
    fun fusionnerConversations(idCible: String, idAutre: String, courantId: String, onClearedCourant: () -> Unit) {
        if (idCible == idAutre) return
        viewModelScope.launch(Dispatchers.IO) {
            val msgsA = conversationRepo.loadMessages(idCible)
            val msgsB = conversationRepo.loadMessages(idAutre)
            val fusion = (msgsA + msgsB).sortedBy { if (it.time > 0) it.time else Long.MAX_VALUE }
            conversationRepo.saveMessages(idCible, fusion)
            // Les todos des deux fils sont concaténés (l'autre est corbeillé).
            val tdsA = conversationRepo.loadTodos(idCible)
            val tdsB = conversationRepo.loadTodos(idAutre)
            conversationRepo.saveTodos(idCible, tdsA + tdsB)
            conversationRepo.mettreCorbeille(idAutre)
            if (idAutre == courantId) withContext(Dispatchers.Main) { onClearedCourant() }
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    // ── hist-005 : dupliquer une conversation ───────────────────────────────

    /** hist-005 — Duplique une conversation (copie complète, originale intacte). */
    fun dupliquerConversation(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val msgs = conversationRepo.loadMessages(id)
            if (msgs.isEmpty()) return@launch
            val nouveau = UUID.randomUUID().toString()
            conversationRepo.saveMessages(nouveau, msgs)
            conversationRepo.saveTodos(nouveau, conversationRepo.loadTodos(id))
            val index = conversationRepo.loadIndex().toMutableList()
            val origine = index.firstOrNull { it.id == id }
            index += ConversationMeta(
                id = nouveau,
                title = (origine?.title ?: "Conversation") + " — copie",
                preview = origine?.preview.orEmpty(),
                updatedAt = System.currentTimeMillis(),
                messageCount = msgs.size,
                pinned = false,
                etiquette = origine?.etiquette.orEmpty()
            )
            conversationRepo.saveIndex(index)
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    // ── hist-009 : corbeille (suppression réversible) ────────────────────────

    /**
     * hist-009 — Suppression réversible : la conversation part en corbeille
     * pendant 7 jours (restaurable depuis l'historique), puis purge automatique.
     */
    fun supprimerVersCorbeille(id: String, currentId: String, onClearedCurrent: () -> Unit) {
        if (id == currentId) onClearedCurrent()
        viewModelScope.launch(Dispatchers.IO) {
            conversationRepo.mettreCorbeille(id)
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    fun listerCorbeille(): List<CorbeilleEntree> = conversationRepo.listerCorbeille()

    fun restaurerDeCorbeille(nom: String) {
        viewModelScope.launch(Dispatchers.IO) {
            conversationRepo.restaurerDeCorbeille(nom)
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    // ── hist-008 : archivage froid ──────────────────────────────────────────

    /** hist-008 — Compresse les conversations de plus de 30 jours (titre + résumé). */
    fun archiverFroid(onDone: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val n = conversationRepo.archiverFroid(30)
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) {
                conversationList = fresh
                onDone(n)
            }
        }
    }

    fun dearchiver(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            conversationRepo.dearchiver(id)
            val fresh = conversationRepo.loadIndex()
            withContext(Dispatchers.Main) { conversationList = fresh }
        }
    }

    /** hist-011 — Statistiques d'une conversation (lecture du fichier). */
    fun statistiques(id: String): StatsConversation = conversationRepo.statistiques(id)
}

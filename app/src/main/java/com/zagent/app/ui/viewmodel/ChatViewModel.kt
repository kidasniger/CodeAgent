package com.zagent.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zagent.app.data.ConversationMeta
import com.zagent.app.data.ConversationRepository
import com.zagent.app.data.SettingsManager
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
        conversationList = conversationRepo.loadIndex()
    }

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
}

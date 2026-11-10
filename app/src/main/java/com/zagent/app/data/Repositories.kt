package com.zagent.app.data

import android.net.Uri
import kotlinx.serialization.json.JsonArray

/**
 * corr-007 — Interfaces des dépôts de données.
 *
 * Objectif : permettre aux tests unitaires de simuler les dépôts au lieu
 * d'atteindre le réseau (AiRepository), le système de fichiers (FileRepository)
 * ou le stockage local des conversations (ConversationRepository). Les classes
 * concrètes implémentent ces interfaces sans changer leur API ; MainViewModel
 * les consomme via le conteneur [com.zagent.app.di.AppContainer].
 */

/** Contrat du client IA (API chat/completions en flux SSE). */
interface AiRepositoryApi {
    /** Annule l'appel réseau en cours (flux SSE) s'il existe. */
    fun cancelActive()

    /** Liste les modèles exposés par le serveur (endpoint /models). */
    suspend fun listerModeles(baseUrl: String, apiKey: String): List<String>

    suspend fun callChatCompletions(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: JsonArray,
        onRetryUpdate: (String) -> Unit,
        onDelta: (String) -> Unit,
        onReasoningDelta: (String) -> Unit,
        /** MàJ 104 — false pour un échange pur (mode Duo) : aucun outil transmis. */
        avecOutils: Boolean
    ): AiRepository.ApiResponse
}

/** Contrat du dépôt de fichiers du projet (racine = dossier du projet ouvert). */
interface FileRepositoryApi {
    fun listDir(path: String = "."): String

    fun listEntries(path: String = "."): List<FileRepository.Entry>

    fun listEntriesResult(path: String = "."): Result<List<FileRepository.Entry>>

    fun fileUri(path: String): Uri?

    fun isTextFile(path: String): Boolean

    fun mimeType(path: String): String

    fun folderName(): String

    fun readFile(path: String): String

    fun readBytes(path: String): ByteArray?

    /**
     * feat-036 — Informations d'un fichier : chemin, type, taille, dernière
     * modification, nombre de lignes si textuel. Message « Erreur: … » si
     * absent ou hors projet. Corps par défaut pour les implémentations de test.
     */
    fun fileInfo(path: String): String = "Erreur: fileInfo non supporté par ce dépôt."

    fun readFileForEdit(path: String): Result<String>

    fun createFile(path: String): String

    fun deletePath(path: String): String

    fun renamePath(path: String, newName: String): String

    fun makeDir(path: String): String

    fun writeFile(path: String, content: String): String
}

/** Contrat du stockage local des conversations (index + messages). */
interface ConversationRepositoryApi {
    fun loadIndex(): List<ConversationMeta>

    fun saveIndex(list: List<ConversationMeta>)

    fun saveMessages(id: String, messages: List<StoredMessage>)

    fun loadMessages(id: String): List<StoredMessage>

    fun deleteMessages(id: String)

    /** corr-077 — liste de tâches par conversation. */
    fun saveTodos(id: String, todos: List<TodoItem>)

    fun loadTodos(id: String): List<TodoItem>

    fun searchContent(query: String, maxPer: Int): List<ConversationHit>

    fun autoTitle(firstUserText: String?): String

    fun autoPreview(lastText: String?): String
}

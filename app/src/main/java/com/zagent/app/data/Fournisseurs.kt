package com.zagent.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * MàJ 101 — Fournisseur d'IA au format OpenAI (chat/completions + Bearer + SSE).
 *
 * Chaque fournisseur porte SA propre clé API : la requête part toujours avec la
 * clé du fournisseur du modèle choisi (voir [ModelCatalog.fournisseurPourModele]).
 * Fini l'époque où la clé z.ai partait chez NVIDIA et renvoyait 401.
 *
 * Les fournisseurs intégrés (z.ai, NVIDIA NIM) existent toujours ; l'utilisateur
 * peut en ajouter autant qu'il veut (OpenRouter, Groq, Ollama local, LM Studio…)
 * : tout endpoint compatible OpenAI marche, sans modification du code.
 */
@Serializable
data class Fournisseur(
    /** Identifiant stable : "zai", "nvidia" ou "perso-<uuid>" pour un ajout manuel. */
    val id: String,
    /** Nom affiché dans l'interface. */
    val nom: String,
    /** Endpoint chat/completions complet (ex. https://integrate.api.nvidia.com/v1/chat/completions). */
    val baseUrl: String,
    /** Clé API dédiée à ce fournisseur ; chiffrée au repos par EncryptedSharedPreferences. */
    val cle: String = "",
    /** Fournisseur désactivé : ses modèles restent visibles mais sont marqués « inactif ». */
    val actif: Boolean = true,
    /** Vrai pour un fournisseur ajouté par l'utilisateur (supprimable). */
    val personnalise: Boolean = false
)

object Fournisseurs {

    const val ID_ZAI = "zai"
    const val ID_NVIDIA = "nvidia"
    // MA82 : fournisseurs intégrés optionnels (clé configurée par l'utilisateur).
    const val ID_OPENROUTER = "openrouter"
    const val ID_GROQ = "groq"

    val JSON = Json { ignoreUnknownKeys = true }
    private val listeSerializer = ListSerializer(Fournisseur.serializer())

    /** Fournisseurs intégrés, toujours présents dans la liste enregistrée. */
    fun integres(): List<Fournisseur> = listOf(
        Fournisseur(ID_ZAI, "z.ai (GLM)", ModelCatalog.URL_ZAI),
        Fournisseur(ID_NVIDIA, "NVIDIA NIM", ModelCatalog.URL_NVIDIA),
        // MA82 : optionnels — restent visibles mais ne servent qu'avec une clé personnelle.
        Fournisseur(ID_OPENROUTER, "OpenRouter", "https://openrouter.ai/api/v1/chat/completions"),
        Fournisseur(ID_GROQ, "Groq", "https://api.groq.com/openai/v1/chat/completions")
    )

    /**
     * Détection automatique (MàJ 101) : devine le fournisseur d'après la clé collée
     * ou l'URL. Règles simples et déterministes :
     *  - clé commençant par « nvapi- » → NVIDIA NIM ;
     *  - clé commençant par « sk-or- » → OpenRouter (MA82) ;
     *  - clé commençant par « gsk_ » → Groq (MA82) ;
     *  - URL/texte contenant « z.ai » → z.ai ;
     *  - URL/texte contenant « nvidia » → NVIDIA NIM ;
     *  - URL/texte contenant « openrouter » ou « groq » → MA82 ;
     *  - sinon null : ce sera un fournisseur personnalisé.
     */
    fun detecter(cleOuUrl: String): String? {
        val v = cleOuUrl.trim()
        if (v.isEmpty()) return null
        if (v.startsWith("nvapi-", ignoreCase = true)) return ID_NVIDIA
        if (v.startsWith("sk-or-", ignoreCase = true)) return ID_OPENROUTER
        if (v.startsWith("gsk_", ignoreCase = true)) return ID_GROQ
        val bas = v.lowercase()
        return when {
            "z.ai" in bas -> ID_ZAI
            "nvidia" in bas -> ID_NVIDIA
            "openrouter" in bas -> ID_OPENROUTER
            "groq" in bas -> ID_GROQ
            else -> null
        }
    }

    /** Nom humain d'un identifiant de fournisseur (pour les messages). */
    fun nomDe(id: String, fournisseurs: List<Fournisseur>): String =
        fournisseurs.firstOrNull { it.id == id }?.nom ?: id

    /**
     * Complète une liste avec les fournisseurs intégrés manquants (jamais supprimables)
     * et dédoublonne par identifiant. Utilisé à chaque enregistrement.
     */
    fun complete(liste: List<Fournisseur>): List<Fournisseur> {
        val vus = HashSet<String>()
        val resultat = ArrayList<Fournisseur>()
        for (f in liste) {
            if (f.id in vus) continue
            vus.add(f.id)
            resultat.add(f)
        }
        for (f in integres()) if (f.id !in vus) resultat.add(f)
        return resultat
    }

    /** Sérialise la liste pour EncryptedSharedPreferences. */
    fun encode(liste: List<Fournisseur>): String =
        JSON.encodeToString(listeSerializer, complete(liste))

    /** Désérialise ; toute erreur renvoie simplement les intégrés (jamais de crash au démarrage). */
    fun decode(brut: String?): List<Fournisseur> {
        if (brut.isNullOrBlank()) return integres()
        return runCatching { complete(JSON.decodeFromString(listeSerializer, brut)) }
            .getOrDefault(integres())
    }
}

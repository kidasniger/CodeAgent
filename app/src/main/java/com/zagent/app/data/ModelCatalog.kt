package com.zagent.app.data

// ─────────────────────────────────────────────────────────────────────────────
// Catalogue des modèles connus de ZAgent (MA59), étendu au fournisseur NVIDIA
// NIM (compte gratuit, format OpenAI : /chat/completions + Bearer + SSE).
//
// MàJ 102 — chaque modèle porte ses CAPACITÉS [Capacite] : l'interface peut
// filtrer (Généralistes / Code / Vision / Raisonnement), afficher un badge et
// proposer le bon modèle pour la bonne tâche. Les modèles « spécialisés »
// (modération, extraction, traduction, calibration) restent écartés du chat.
//
// MàJ 101 — chaque modèle est rattaché à son fournisseur [Fournisseur] via son
// URL d'origine [ModelInfo.baseUrl] : la requête repart toujours vers le bon
// serveur avec la bonne clé (voir [ModelCatalog.fournisseurPourModele]).
//
// Testés sur https://integrate.api.nvidia.com/v1 — les modèles en timeout
// (> 180 s) et ceux retirés du catalogue (404) ne sont pas intégrés.
// ─────────────────────────────────────────────────────────────────────────────

/** Capacité d'un modèle (MàJ 102). Affichée en badge et utilisée par les filtres du sélecteur. */
enum class Capacite(val libelle: String) {
    TEXTE("Texte"),
    CODE("Code"),
    VISION("Vision"),
    RAISONNEMENT("Raisonnement"),
    TRADUCTION("Traduction"),
    MODERATION("Modération"),
    EXTRACTION("Extraction"),
    CALIBRATION("Calibration")
}

/** Description d'un modèle du catalogue (MA59). Les prix affichés sont ceux configurés dans Réglages. */
data class ModelInfo(
    val id: String,
    val label: String,
    val description: String,
    val contextWindow: String,
    val speed: Int,
    val recommended: Boolean,
    /** URL complète du endpoint chat/completions du fournisseur d'origine (null = URL par défaut). */
    val baseUrl: String? = null,
    /** Modèle spécialisé (modération, extraction…) : pas destiné à la conversation. */
    val specialized: Boolean = false,
    /** Ce que le modèle sait faire (MàJ 102) ; par défaut : texte seul. */
    val capacites: Set<Capacite> = setOf(Capacite.TEXTE),
    /** maj-083 — température conseillée du modèle (null = défaut du fournisseur). */
    val temperature: Float? = null,
    /** maj-083 — top_p conseillé (null = défaut du fournisseur). */
    val topP: Float? = null,
    /** maj-083 — plafond de tokens de sortie conseillé (null = défaut du fournisseur). */
    val maxTokens: Int? = null
) {
    /**
     * maj-083 — Paramètres de génération conseillés pour ce modèle.
     * Renvoie null si aucun des trois champs n'est renseigné : le corps de la
     * requête reste alors exactement comme avant (aucun changement de comportement).
     */
    fun generationParams(): GenerationParams? {
        if (temperature == null && topP == null && maxTokens == null) return null
        return GenerationParams(temperature, topP, maxTokens)
    }
    /** Identifiant du fournisseur d'origine, dérivé de l'URL (MàJ 101). */
    val fournisseurId: String
        get() = when (baseUrl) {
            ModelCatalog.URL_NVIDIA -> Fournisseurs.ID_NVIDIA
            else -> Fournisseurs.ID_ZAI
        }

    /** Vrai si le modèle peut répondre en texte et code (candidat conversation/agent). */
    val conversationnel: Boolean
        get() = !specialized && (Capacite.TEXTE in capacites || Capacite.CODE in capacites)
}

/**
 * maj-083 — Paramètres de génération envoyés au fournisseur dans le corps de la
 * requête chat/completions. Tous les champs sont optionnels : seuls ceux non
 * null sont ajoutés au JSON (le fournisseur applique ses défauts pour les autres).
 */
data class GenerationParams(
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxTokens: Int? = null
) {
    /** Vrai si au moins un paramètre est renseigné. */
    val hasAny: Boolean get() = temperature != null || topP != null || maxTokens != null
}

/**
 * corr-018 — Catalogue statique des modèles et résolution par fournisseur.
 *
 * Renseigne pour chaque modèle : capacité (vision, outils…), URL de base du
 * fournisseur (urlPourModele), chaîne de repli (chaineRepli), filtres pour
 * les sélecteurs (conversationnels). Source unique de URL_ZAI.
 */
object ModelCatalog {

    /** Endpoint z.ai par défaut (format OpenAI également). */
    const val URL_ZAI = "https://api.z.ai/api/paas/v4/chat/completions"

    /** Endpoint NVIDIA NIM (compte gratuit, format OpenAI). */
    const val URL_NVIDIA = "https://integrate.api.nvidia.com/v1/chat/completions"

    /** URLs « connues » : basculer automatiquement entre elles est sûr. Une URL
     *  personnalisée (passerelle, proxy) n'est jamais écrasée par la bascule.
     *  Propriété calculée : elle lit [all], déclaré plus bas. */
    val urlsConnues: Set<String>
        get() = all.mapNotNull { it.baseUrl }.toSet() + URL_ZAI

    /** GLM via z.ai — le fournisseur d'origine de l'application. */
    val glm = listOf(
        ModelInfo(
            "glm-4.5-flash", "GLM 4.5 Flash",
            "Rapide et économique — parfait au quotidien",
            "128 k tokens", 3, true, baseUrl = URL_ZAI,
            capacites = setOf(Capacite.TEXTE, Capacite.CODE)
        ),
        ModelInfo(
            "glm-4.7-flash", "GLM 4.7 Flash",
            "Plus récent, un peu plus capable",
            "128 k tokens", 2, false, baseUrl = URL_ZAI,
            capacites = setOf(Capacite.TEXTE, Capacite.CODE)
        )
    )

    /** NVIDIA NIM — discussion et code (testés, compte gratuit). */
    val nvidiaChat = listOf(
        ModelInfo(
            "nvidia/nemotron-3-ultra-550b-a55b", "Nemotron 3 Ultra 550B",
            "Le plus puissant",
            "NVIDIA NIM", 1, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.TEXTE, Capacite.CODE, Capacite.RAISONNEMENT)
        ),
        ModelInfo(
            "mistralai/mistral-large-4-1t-a52b", "Mistral Large 4 1T",
            "Généraliste, très gros",
            "NVIDIA NIM", 1, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.TEXTE, Capacite.CODE)
        ),
        ModelInfo(
            "nvidia/nemotron-3.5-super-vl-120b-a12b", "Nemotron 3.5 Super VL 120B",
            "Texte + image",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.TEXTE, Capacite.VISION)
        ),
        ModelInfo(
            "nvidia/nemotron-3.5-lightning-30b-a3b", "Nemotron 3.5 Lightning 30B",
            "Rapide",
            "NVIDIA NIM", 3, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.TEXTE, Capacite.CODE)
        ),
        ModelInfo(
            "nvidia/nemotron-3.5-bolt-e4b", "Nemotron 3.5 Bolt E4B",
            "Très léger",
            "NVIDIA NIM", 3, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.TEXTE)
        ),
        ModelInfo(
            "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning", "Nemotron 3 Nano Omni 30B",
            "Raisonnement",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.TEXTE, Capacite.RAISONNEMENT)
        ),
        ModelInfo(
            "minimaxai/minimax-m3.1-flash", "MiniMax M3.1 Flash",
            "Rapide",
            "NVIDIA NIM", 3, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.TEXTE, Capacite.CODE)
        ),
        ModelInfo(
            "stepfun-ai/step-5-preview", "Step 5 Preview",
            "Généraliste",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.TEXTE, Capacite.CODE)
        ),
        ModelInfo(
            "poolside/laguna-xs-2.1", "Laguna XS 2.1",
            "Spécialiste du code",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.CODE)
        ),
        ModelInfo(
            "meta/llama-3.2-90b-vision-instruct", "Llama 3.2 90B Vision",
            "Texte + image",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.TEXTE, Capacite.VISION)
        ),
        ModelInfo(
            "meta/muse-glimmer-30b", "Muse Glimmer 30B",
            "Généraliste",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.TEXTE)
        ),
        ModelInfo(
            "google/diffusiongemma-26b-a4b-it", "DiffusionGemma 26B",
            "Gemma diffusion",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA,
            capacites = setOf(Capacite.TEXTE)
        )
    )

    /** NVIDIA NIM — spécialisés (modération, extraction, traduction) : pas pour discuter. */
    val nvidiaSpecialises = listOf(
        ModelInfo(
            "meta/llama-guard-4-12b", "Llama Guard 4 12B",
            "Modération",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA, specialized = true,
            capacites = setOf(Capacite.MODERATION)
        ),
        ModelInfo(
            "nvidia/llama-3.1-nemotron-safety-guard-8b-v3", "Nemotron Safety Guard 8B",
            "Modération",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA, specialized = true,
            capacites = setOf(Capacite.MODERATION)
        ),
        ModelInfo(
            "nvidia/nemotron-parse-2.0", "Nemotron Parse 2.0",
            "Extraction de documents",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA, specialized = true,
            capacites = setOf(Capacite.EXTRACTION)
        ),
        ModelInfo(
            "nvidia/riva-translate-4b-instruct-v2", "Riva Translate 4B",
            "Traduction",
            "NVIDIA NIM", 3, false, baseUrl = URL_NVIDIA, specialized = true,
            capacites = setOf(Capacite.TRADUCTION)
        ),
        ModelInfo(
            "nvidia/ising-calibration-1.5-31b", "Ising Calibration 31B",
            "Calibration",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA, specialized = true,
            capacites = setOf(Capacite.CALIBRATION)
        )
    )

    /** Tous les modèles : catalogue de discussion puis spécialisés. */
    val all: List<ModelInfo> = glm + nvidiaChat + nvidiaSpecialises

    /** Retrouve une entrée du catalogue par son identifiant exact. */
    fun find(modelId: String): ModelInfo? {
        val id = modelId.trim()
        return all.firstOrNull { it.id == id }
    }

    /**
     * maj-083 — Paramètres de génération conseillés pour [modelId] (null si le
     * modèle est inconnu ou n'en définit aucun).
     */
    fun generationParamsPour(modelId: String): GenerationParams? = find(modelId)?.generationParams()

    /**
     * MàJ 101 — fournisseur enregistré qui héberge [modelId] : la clé de CE
     * fournisseur part avec la requête. On préfère un fournisseur actif ; à
     * défaut, on renvoie l'entrée même inactive (l'appelant affichera l'erreur).
     */
    fun fournisseurPourModele(modelId: String, fournisseurs: List<Fournisseur>): Fournisseur? {
        val info = find(modelId) ?: return null
        val cible = fournisseurs.filter { it.baseUrl == info.baseUrl }
        return cible.firstOrNull { it.actif } ?: cible.firstOrNull()
    }

    /**
     * MàJ 103 — chaîne de repli pour [modelId] : d'abord les autres modèles
     * conversationnels du même fournisseur (du plus rapide au plus lourd),
     * puis ceux des autres fournisseurs. Utilisée par le repli automatique.
     */
    fun chaineRepli(modelId: String): List<String> {
        val origine = find(modelId) ?: return emptyList()
        val memeFournisseur = all.filter {
            it.conversationnel && it.baseUrl == origine.baseUrl && it.id != origine.id
        }.sortedByDescending { it.speed }.map { it.id }
        val autres = all.filter {
            it.conversationnel && it.baseUrl != origine.baseUrl
        }.sortedByDescending { it.speed }.map { it.id }
        return memeFournisseur + autres
    }

    /** Liste des modèles conversationnels filtrée par capacité (null = tous), favoris en tête. */
    fun conversationnels(capacite: Capacite?, favoris: Set<String> = emptySet()): List<ModelInfo> {
        val base = all.filter { it.conversationnel }
            .filter { capacite == null || capacite in it.capacites }
        return base.sortedByDescending { it.id in favoris }
    }

    /**
     * URL à utiliser pour une requête vers [modelId].
     *  - modèle de catalogue avec fournisseur d'origine : la requête repart vers
     *    ce fournisseur, SAUF si l'utilisateur a branché une URL personnalisée
     *    qui n'est pas une URL « connue » (passerelle/proxy : on la respecte) ;
     *  - modèle inconnu : l'URL personnalisée, sinon z.ai par défaut.
     */
    fun urlPourModele(modelId: String, urlPersonnalisee: String?): String {
        val perso = urlPersonnalisee?.trim().orEmpty()
        val origine = find(modelId)?.baseUrl
        if (origine != null) {
            return if (perso.isEmpty() || perso in urlsConnues) origine else perso
        }
        return perso.ifEmpty { URL_ZAI }
    }
}

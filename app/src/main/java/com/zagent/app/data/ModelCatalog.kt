package com.zagent.app.data

// ─────────────────────────────────────────────────────────────────────────────
// Catalogue des modèles connus de ZAgent (MA59), étendu au fournisseur NVIDIA
// NIM (compte gratuit, format OpenAI : /chat/completions + Bearer + SSE).
//
// Chaque entrée porte l'URL d'origine de son fournisseur [ModelInfo.baseUrl] :
// la requête repart toujours vers le bon serveur même si le réglage global
// pointe ailleurs (voir [ModelCatalog.urlPourModele]). Les modèles
// « spécialisés » (modération, extraction, traduction, calibration) sont
// listés pour référence : ils ne sont pas adaptés à la conversation.
//
// Testés sur https://integrate.api.nvidia.com/v1 — les modèles en timeout
// (> 180 s) et ceux retirés du catalogue (404) ne sont pas intégrés.
// ─────────────────────────────────────────────────────────────────────────────

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
    val specialized: Boolean = false
)

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
            "128 k tokens", 3, true, baseUrl = URL_ZAI
        ),
        ModelInfo(
            "glm-4.7-flash", "GLM 4.7 Flash",
            "Plus récent, un peu plus capable",
            "128 k tokens", 2, false, baseUrl = URL_ZAI
        )
    )

    /** NVIDIA NIM — discussion et code (testés, compte gratuit). */
    val nvidiaChat = listOf(
        ModelInfo(
            "nvidia/nemotron-3-ultra-550b-a55b", "Nemotron 3 Ultra 550B",
            "Le plus puissant",
            "NVIDIA NIM", 1, false, baseUrl = URL_NVIDIA
        ),
        ModelInfo(
            "mistralai/mistral-large-4-1t-a52b", "Mistral Large 4 1T",
            "Généraliste, très gros",
            "NVIDIA NIM", 1, false, baseUrl = URL_NVIDIA
        ),
        ModelInfo(
            "nvidia/nemotron-3.5-super-vl-120b-a12b", "Nemotron 3.5 Super VL 120B",
            "Texte + image",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA
        ),
        ModelInfo(
            "nvidia/nemotron-3.5-lightning-30b-a3b", "Nemotron 3.5 Lightning 30B",
            "Rapide",
            "NVIDIA NIM", 3, false, baseUrl = URL_NVIDIA
        ),
        ModelInfo(
            "nvidia/nemotron-3.5-bolt-e4b", "Nemotron 3.5 Bolt E4B",
            "Très léger",
            "NVIDIA NIM", 3, false, baseUrl = URL_NVIDIA
        ),
        ModelInfo(
            "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning", "Nemotron 3 Nano Omni 30B",
            "Raisonnement",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA
        ),
        ModelInfo(
            "minimaxai/minimax-m3.1-flash", "MiniMax M3.1 Flash",
            "Rapide",
            "NVIDIA NIM", 3, false, baseUrl = URL_NVIDIA
        ),
        ModelInfo(
            "stepfun-ai/step-5-preview", "Step 5 Preview",
            "Généraliste",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA
        ),
        ModelInfo(
            "poolside/laguna-xs-2.1", "Laguna XS 2.1",
            "Code",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA
        ),
        ModelInfo(
            "meta/llama-3.2-90b-vision-instruct", "Llama 3.2 90B Vision",
            "Texte + image",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA
        ),
        ModelInfo(
            "meta/muse-glimmer-30b", "Muse Glimmer 30B",
            "Généraliste",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA
        ),
        ModelInfo(
            "google/diffusiongemma-26b-a4b-it", "DiffusionGemma 26B",
            "Gemma diffusion",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA
        )
    )

    /** NVIDIA NIM — spécialisés (modération, extraction, traduction) : pas pour discuter. */
    val nvidiaSpecialises = listOf(
        ModelInfo(
            "meta/llama-guard-4-12b", "Llama Guard 4 12B",
            "Modération",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA, specialized = true
        ),
        ModelInfo(
            "nvidia/llama-3.1-nemotron-safety-guard-8b-v3", "Nemotron Safety Guard 8B",
            "Modération",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA, specialized = true
        ),
        ModelInfo(
            "nvidia/nemotron-parse-2.0", "Nemotron Parse 2.0",
            "Extraction de documents",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA, specialized = true
        ),
        ModelInfo(
            "nvidia/riva-translate-4b-instruct-v2", "Riva Translate 4B",
            "Traduction",
            "NVIDIA NIM", 3, false, baseUrl = URL_NVIDIA, specialized = true
        ),
        ModelInfo(
            "nvidia/ising-calibration-1.5-31b", "Ising Calibration 31B",
            "Calibration",
            "NVIDIA NIM", 2, false, baseUrl = URL_NVIDIA, specialized = true
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

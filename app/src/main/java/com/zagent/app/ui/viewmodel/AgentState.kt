package com.zagent.app.ui.viewmodel

/**
 * corr-020 — Modélisation de l'état de l'agent avec une sealed class.
 *
 * Remplace progressivement les drapeaux booléens dispersés (`isLoading`,
 * `liveCommand`, `pendingWrite`…) par un état unique et exhaustif : impossible
 * de se retrouver « streaming ET en attente de confirmation », et les écrans
 * peuvent faire un `when` complet sur l'état (le compilateur garantit l'exhaustivité).
 *
 * La classe ne remplace pas encore les drapeaux (adoption progressive,
 * même politique que corr-008) : MainViewModel expose [MainViewModel.agentState]
 * dérivé des drapeaux existants, et les nouveaux écrans sont invités à lire
 * cette propriété unique.
 */
sealed class AgentState {
    /** Au repos : aucune requête ni commande en cours. */
    data object Idle : AgentState()

    /** Le modèle stream une réponse (texte et/ou raisonnement visibles). */
    data class Streaming(val texte: String, val raisonnement: String) : AgentState()

    /** Une commande lancée par l'agent s'exécute dans le terminal. */
    data class ExecutingTool(val commande: String, val id: String?) : AgentState()

    /** L'agent attend une décision de l'utilisateur (écriture, suppression, commande, question). */
    data class WaitingUser(val motif: Motif) : AgentState() {
        enum class Motif { ECRITURE, SUPPRESSION, COMMANDE, QUESTION }
    }

    /** Bloqué par une erreur (réseau, clé invalide, limite d'étapes). */
    data class Error(val message: String) : AgentState()
}

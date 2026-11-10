package com.zagent.app.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context

/**
 * corr-023 — Copie de texte sensible dans le presse-papiers.
 *
 * L'audit corr-021/corr-023 n'a trouvé aucun chemin qui copie une clé API
 * (les copies existantes : messages du chat, partage de conversation, sortie
 * du terminal). Cet utilitaire est néanmoins fourni pour tout futur bouton
 * « copier la clé » : sur Android 13+ (API 33) le clip est marqué
 * [ClipDescription.EXTRA_IS_SENSITIVE], ce qui empêche l'affichage dans le
 * presse-papiers système et les suggestions du clavier, et les écrans ne
 * doivent pas le conserver au-delà de l'action.
 *
 * Sur Android < 13, le drapeau est ignoré par le système (aucun crash).
 */
object ClipboardSensible {

    /** Copie [texte] en le marquant sensible si l'API le permet. */
    fun copier(context: Context, label: String, texte: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, texte)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            clip.description.extras = android.os.PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        cm.setPrimaryClip(clip)
    }
}

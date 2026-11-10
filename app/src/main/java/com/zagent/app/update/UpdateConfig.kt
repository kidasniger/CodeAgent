package com.zagent.app.update

/**
 * MA64 — Configuration du canal de mises à jour paramétrable au build.
 *
 * Les valeurs proviennent de `gradle.properties` (ou de -P sur la ligne de
 * commande) et sont injectées en BuildConfig : `ZAGENT_GITHUB_OWNER` et
 * `ZAGENT_GITHUB_REPO`. Les défauts correspondent au dépôt de publication.
 */
object UpdateConfig {
    val GITHUB_OWNER: String = com.zagent.app.BuildConfig.ZAGENT_GITHUB_OWNER
    val GITHUB_REPO: String = com.zagent.app.BuildConfig.ZAGENT_GITHUB_REPO
}

# Checklist de non-régression — ZAgent (err-100)

À relancer avant toute publication. Les points 1 à 3 tournent automatiquement
dans GitHub Actions (« Build & Release APK ») à chaque push.

## 1. Build

- [ ] `./gradlew assembleDebug` — compilation Kotlin + ressources sans erreur.
- [ ] Rapport « erreurs compilation » : aucune annotation `e: file://…` dans le run.
- [ ] `assembleRelease` signé (job release) : `ZAgent-v1.0-bN.apk` + `.sha256` publiés.

## 2. Tests

- [ ] `./gradlew :app:testDebugUnitTest` — tous verts
      (FileRepositoryTest, DiffUtilTest, AgentToolsPatchTest,
      **AgentToolsArgumentsLenientTest** (err-098), ModelCatalogTest,
      MoteurMultiFournisseursTest, UsageRepositoryTest, CorrRegressionTest,
      ReleaseParserTest, VersionComparatorTest).

## 3. Lint & sécurité

- [ ] `:app:lintDebug` — informatif, rapport HTML en artefact.
- [ ] Scan OSV (osv-scanner) — rapport `osv-report.json` en artefact.

## 4. Terminal embarqué

- [ ] Installation du système (bootstrap) sur un préfixe vierge.
- [ ] bash démarre dans un onglet ; `error=13` impossible (targetSdk 28, err-018).
- [ ] « Réparer » (err-074) : repose les droits et relance la configuration sans retélécharger.
- [ ] Commande inconnue → astuce `pkg install …` (err-065) ; git/ssh → consignes (err-075/076).
- [ ] Arrêt d'une commande → aucun processus orphelin (err-067).

## 5. Agent & réseau

- [ ] Sans réseau → arrêt immédiat avec message clair (err-021).
- [ ] 401/403/404 nomment le fournisseur (err-022/023/024) ; 529/1305 → bouton « Changer de modèle » (err-028).
- [ ] Retry-After respecté + backoff exponentiel avec jitter (err-026/027).
- [ ] Contexte dépassé (400) → consigne nouvelle conversation (err-039).
- [ ] Arguments d'outils tronqués réparés (err-034) ; outil inconnu → liste des outils (err-036).
- [ ] Boucle d'outils : 3 appels identiques → arrêt propre (err-037).
- [ ] Flux SSE coupé → contenu partiel signalé (err-033) ; réponse vide / max_tokens → message dédié (err-038).

## 6. Fichiers

- [ ] Binaire refusé en texte (err-050) ; encodage invalide signalé (err-051).
- [ ] Disque plein → échec tôt et clair, avant ET pendant l'écriture (err-048).
- [ ] Écriture atomique : aucun fichier tronqué après kill (err-054).
- [ ] Projet supprimé → nettoyage au démarrage + message (err-056).
- [ ] Conflit agent/éditeur → bandeau « Recharger / Garder le mien » (err-057).
- [ ] Archive zip corrompue → « Archive corrompue », pas de Zip Slip (err-060).

## 7. Système & récupération

- [ ] Crash → rapport écrit ; au redémarrage, message « Aide › Signaler un problème » (err-077/078).
- [ ] Rapport de diagnostic sans secrets (version, activité, journal réseau) (err-042/079).
- [ ] Restauration d'appareil → réglages réinitialisés + invite de ressaisie (err-085).
- [ ] Notifications refusées → repli in-app (err-086) ; micro refusé → réglages (err-087).
- [ ] Mise à jour : erreurs précises (téléchargement, SHA, signature, err-093/094).
- [ ] Conversations corrompues isolées en `.corrupt-*` (err-095) ; sauvegarde pré-migration (err-096).

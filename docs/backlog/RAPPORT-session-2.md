# Rapport de session 2 — catégorie « Corrections » (corr-001…corr-100)

Date : 2026-11-11 · Dépôt : CodeAgent (ZAgent) · Backlog : 600 prompts (6 × 100)

## Bilan de la catégorie Corrections : 100/100 traités

| État | Nombre |
| --- | --- |
| Implémentés cette session | **76** |
| Déjà conformes (vérifiés, aucune modification nécessaire) | 22 |
| Partiels documentés | 2 (corr-008, corr-060) |

## Refontes structurelles (corr-001 → corr-020)

- **corr-001** : cartographie complète du MainViewModel (3 104 lignes →
  `docs/backlog/CARTOGRAPHIE-ViewModel.md`).
- **corr-002/003/004** : extraction de **ChatViewModel** (messages, conversation
  courante, brouillons MA55, index MA56), **NavigationViewModel** (état de
  navigation) et **SettingsViewModel** (miroirs de SettingsManager). MainViewModel
  conserve son API publique par délégation — aucun écran modifié.
- **corr-005** : **TerminalViewModel** — onglets shell sortis de TerminalController.
- **corr-006** : **AppContainer** (DI) instancie les cinq dépôts ; corr-007 :
  interfaces AiRepositoryApi / FileRepositoryApi / ConversationRepositoryApi.
- **corr-009** : événements de navigation en `SharedFlow<NavEvent>` (replay 1 +
  `resetReplayCache`), remplaçant les drapeaux `navRouteRequest/navBackRequest`.
- **corr-010/011** : ChatScreen (890 lignes) splitte vers ChatRows.kt ;
  ZComponents (1 382 lignes) séparé en ZButtons / ZMenus / ZCards / ZCommon.
- **corr-012/013/014** : renommages fonctionnels (InputBarLot4 → ChatInputBar,
  Lot4Components → SheetsDialogs, Lot5Components → OverlayComponents),
  conventions de langue (`CONVENTIONS-CODE.md`), sous-packages
  `ui/components/chat/` et `common/` (78 symboles, imports réécrits).

## Sécurité (corr-021 → corr-049)

- **corr-019/021** : `util/ZLog.kt` masque les clés (secrets enregistrés +
  motifs Bearer/sk-/github_pat) ; logs verbeux coupés en release.
- **corr-023** : `ClipboardSensible` (EXTRA_IS_SENSITIVE, Android 13+).
- **corr-024** : conversations / usage / activité exclus des sauvegardes.
- **corr-025/026** : FileRepository — validation canonique (décodage
  percent-encoding, `..`/backslash/NUL), confinement via canonicalPath
  (liens symboliques), contexte nullable pour tests JVM purs.
- **corr-027/029/030/032/034** : équivalents destructeurs ajoutés, **mode
  lecture seule** (toggle + garde), confirmation des commandes qui quittent le
  projet, plafond de timeout applicatif, cadrage anti-injection du prompt
  système.
- **corr-035→038** : serveur d'aperçu — jeton aléatoire obligatoire (cookie +
  `?t=`), `X-Content-Type-Options`, CSP modérée, WebView durci.
- **corr-039/040/041/042/043** : deep links validés (chemin relatif), ApkVerifier
  avec **SHA-256 obligatoire**, HTTPS obligatoire hors local (message d'erreur
  visible), message TLS explicite, aucun TrustManager permissif (vérifié).
- **corr-044→047** : scan OSV en CI (+ artefact), `isDebuggable=false` explicite,
  `docs/SECRETS.md`, KEY_PASSWORD dérivé de KS_PASS.

## Fiabilité et données (corr-050 → corr-099)

- **corr-061/062** : EncryptedSharedPreferences construit hors thread principal
  (warm-up parallèle au démarrage) ; corr-063 : archive des conversations
  **débouncée** (2 s) avec flush immédiat au changement.
- **corr-069** : historique paginé (fenêtre 60 + « Afficher plus »).
- **corr-074/076/077** : arrêt immédiat de la commande en cours, limite d'étapes
  vérifiée, **update_todos fusionné** (mise à jour partielle sans perte) **et
  persisté par conversation** (`<id>.todos.json`, TodoItem @Serializable).
- **corr-082/083/085/086** : coût extrait dans `coutEstime()` testable,
  fournisseur désactivé → envoi bloqué avec message, `coerceInputValues`
  partout, versionnage du schéma local (`schema.json` + migrations).
- **corr-087/096/097** : barre d'onglets réaffichée au changement d'écran,
  interrupteurs de Permissions & sécurité réactifs (miroirs Compose +
  resynchronisation à l'ouverture), canaux de notification localisés et
  notification→conversation.
- **corr-099** : 17 tests de non-régression (fusion des tâches, placeholders,
  masquage des secrets, événements de navigation, coût).

## Tests ajoutés (4 fichiers, ~44 tests)

`FileRepositoryTest` (confinement ../, ..\, %2e%2e, symlink, UTF-8/BOM/CRLF,
écriture atomique) · `DiffUtilTest` (vides, CRLF, gros diff 600 lignes) ·
`UsageRepositoryTest` (coût, flux interrompu) · `CorrRegressionTest` (16 tests
des correctifs).

## Vérifications déjà conformes (22 prompts)

Mise en cache markdown/SSE (corr-052/053), cles LazyColumn + recompositions
(corr-050/051, MàJ 99), tampons terminal bornés (corr-057), repli Haze sans
flou < API 31 (corr-059), assainissement env des processus (corr-031), écriture
atomique (corr-072), timeout commande plafonné (mécanique), politeness des
raccourcis clavier/insets (corr-089), proportions du split editor/chat bornées
et persistées (corr-090), édition onglets double/confirmer (corr-092),
undo/redo borné (corr-093), recherche globale vide/binaire (corr-094),
RECORD_AUDIO au moment de la dictée (corr-048), composants exportés
(corr-049), pagination… (liste complète dans `state.json`, statut
`preexistant`).

## Risques restants (à valider par CI build+tests+lint)

1. **Aucun build Android n'a pu être lancé** (pas de SDK dans l'environnement
   d'audit) : la validation finale passe par le job GitHub `build` — attention
   notamment aux imports croisés des sous-packages de composants et aux
   délégations MainViewModel.
2. **R8** : les règles existantes doivent couvrir les nouveaux ViewModel
   (aucun réflexion connue dessus : risque faible).
3. **Jeton du serveur d'aperçu** : vérifier le comportement du CookieManager
   WebView sur tous les appareils de test (le `?t=` de secours couvre le
   premier chargement).
4. **Partiels** : corr-008 (GitUiState définie, migration des champs git à
   finir), corr-060 (redimensionnement image à brancher sur la future voie
   vision).

## État général du backlog

- Mises à jour (maj) : 76 complets + 8 partiels + 2 reportés + 14 préexistants
- **Corrections (corr) : 100/100 traités** (76 faits + 22 préexistants + 2 partiels)
- Prochaines sessions : **feat-001…** (Fonctionnalités), puis ui, ux, err.

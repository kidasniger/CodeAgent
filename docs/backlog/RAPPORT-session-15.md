# RAPPORT session 15 — plein contrôle GitHub pour l'IA

Date : 2026-11-12 · dépôt kidasniger/CodeAgent · base v1.0-b345 (f31407a) → cible v1.0-b346

## 1. Demande

« D'accord fait les et je veux que l'IA ait le plein contrôle avec mon token github
donc tout, puisque mon token a accès à tout » — l'agent embarqué doit pouvoir AGIR
sur GitHub de bout en bout avec le jeton chiffré de l'utilisateur, sans limite au
seul remote du projet ouvert.

## 2. Ce qui existait (session 12)

Deux outils : `github_actions` (lecture des runs) et `github_pr` (création d'une PR
depuis la branche courante). Le jeton était déjà injecté côté app, jamais exposé.

## 3. Le plein contrôle (ghc-001 → ghc-006)

### ghc-001 — `GitHubApi.kt` : client REST étendu (~450 lignes nouvelles)

Helper générique `appel()` (GET/POST/PUT/PATCH/DELETE, `Result<Pair<code, corps>>`),
formateur d'erreurs `erreurHttp()` (401/403/404/409/422 avec message GitHub), et
**18 fonctions métier** renvoyant un texte déjà formaté pour le modèle :

| Fonction | Endpoint |
|---|---|
| `utilisateur` | GET /user |
| `listeDepots` | GET /user/repos (visibility, sort=updated) |
| `creerDepot` | POST /user/repos (private, auto_init, description) |
| `lireFichier` | GET /contents/{path} (fichier décodé base64, ou listing dossier) |
| `ecrireFichier` | PUT /contents/{path} (sha auto-résolu → create ou update, commit unique) |
| `supprimerFichier` | DELETE /contents/{path} (sha auto-résolu) |
| `listerBranches` | GET /branches |
| `creerBranche` | GET /git/ref + POST /git/refs (depuis une ref ou la branche par défaut) |
| `listerIssues` / `creerIssue` / `commenterIssue` / `etatIssue` | /issues… (PR exclues du listing) |
| `listerPulls` / `fusionnerPull` (merge/squash/rebase) / `fermerPull` | /pulls… |
| `listerReleases` / `creerRelease` | /releases |
| `creerGist` | POST /gists (secret par défaut) |
| `rechercher` | /search/repositories ou /issues ou /code |
| `declencherWorkflow` | POST /actions/workflows/{w}/dispatches |

`depotApiBase()` accepte indifféremment « owner/repo » ou une URL github.com
(https/ssh) ; `actionsRuns` / `createPullRequest` / `branchParDefaut` passent par
cette fonction : tout outil GitHub marche désormais même sans projet ouvert,
en passant `repo: "owner/repo"`.

### ghc-002 — `AgentTools.kt` : 11 outils déclarés + `nomsOutils`

`github_user`, `github_repos`, `github_repo_create`, `github_file` (read/write/delete),
`github_branch` (list/create), `github_issues` (list/create/comment/close/reopen),
`github_pulls` (list/merge/close), `github_releases` (list/create), `github_gist`,
`github_search`, `github_dispatch`. Descriptions en français, contrat identique à la
session 12 : le jeton est injecté par l'app, l'IA ne doit jamais le demander.
Tous apparaissent individuellement dans Permissions & sécurité (Auto/Dem./Refus/Projet).

### ghc-003 — `MainViewModel` : `executerOutilGitHub()` centralisée

- Routage unique `tc.name.startsWith("github_")` ; jeton manquant → message dédié.
- **Lectures directes** ; **écritures barrées** par mode lecture seule (corr-029) et
  par interrupteur d'écriture de la conversation (feat-014), puis **confirmées**
  (`waitForCommandConfirmation`, pied de dialogue explicatif) sauf permission
  « autoriser » posée sur l'outil (`outilAutoriseForce`).
- **Incohérence corrigée** : `github_pr` prétendait « confirmé par l'utilisateur »
  depuis la session 12 mais ne l'était jamais en code — la porte est posée.
- Dépôt visé : paramètre `repo` sinon remote origin ; erreur explicite si aucun.
- Chaque appel journalisé (activité, durée, statut OK/ERREUR/Refusé) ; le résultat
  entier reste hors des journaux verbeux (première ligne tronquée à 160).

### ghc-004 — prompt système

Une phrase dédiée : liste des capacités github_*, règle « jeton injecté
automatiquement — ne le demande jamais », paramètre `repo`, écritures confirmées.

### ghc-005 — ressources fr/en

20 libellés `tool_github_*` (fr + en, apostrophes échappées `\'`, parité vérifiée,
699 → 719 clés). Étiquettes d'exécution (describeTool) câblées pour les 11 outils,
avec variantes par action (read/write/delete, create/list, merge/close…).

### ghc-006 — tests

`GitHubApiTest` (JVM pur, sans réseau) : parseOwnerRepo https/ssh/sans .git/invalide,
depotApiBase owner-repo/URL/superjeu de apiBase/tirets-points/invalide — 10 tests.

## 4. Gardes (scripts/garde15.py)

XML valide ×2, parité fr/en stricte (22 clés tool_github_*), chaque `R.string.tool_github_*`
référencé existe ×2, équilibre accolades/parenthèses hors chaînes identique à HEAD
(4 fichiers), **aucun jeton réel** dans sources/docs (git grep), 13 outils github_*
déclarés une seule fois, tous étiquetés et exécutés. Détail technique repéré et corrigé
avant commit : `buildJsonArray { add("workflow"), add("ref") }` (virgule entre
instructions) réécrit en deux `add`.

## 5. Sécurité — ce qui ne change pas

- Le jeton vit dans EncryptedSharedPreferences (`SettingsManager.jetonGitHub`).
- Il n'apparaît jamais dans les arguments de l'IA, ni dans les résultats d'outils,
  ni dans les journaux (`ZLog.masquer` couvre `github_pat_…` / `ghp_…` / `pat_…`).
- L'IA ne peut PAS lire le jeton (aucun outil ne le renvoie ; `github_user` ne
  renvoie que login/type/dépôts publics).
- Les opérations destructrices (delete fichier, merge, close, dispatch) restent
  confirmées ; l'utilisateur garde les refuse par outil dans Permissions & sécurité.

## 6. Restes tracés

- i18n-001/i18n-002, nettoyage-002 (hérités de la session 14, non touchés).
- La CI « Build & Release APK » reste la porte de compilation (pas de SDK Android
  local) ; publication v1.0-b346 si verte.

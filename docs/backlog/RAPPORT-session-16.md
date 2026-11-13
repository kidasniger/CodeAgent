# RAPPORT session 16 — un dépôt GitHub complet comme environnement, même hors ligne

Date : 2026-11-14 · dépôt kidasniger/CodeAgent · base v1.0-b346 (f37d20b) → cible v1.0-b347
Dépôt d'entraînement : https://github.com/kidasniger/zagent-playground (public, créé et rempli dans cette session)

## 1. Demande

« Ça serait possible de donner à l'agent un dépôt github complet comme
environnement, pour exécuter ses tâches même hors ligne » puis « D'accord ajoute
ça et tout les 100 améliorations ». Nom du dépôt public validé : `zagent-playground`.

## 2. Découverte qui a façonné la conception

L'app embarque un **vrai git** dans son terminal (`gitClone/gitPush/gitPull` de
feat-081/085 existent, jeton injecté dans l'URL https) et une **file d'attente
hors ligne du chat** (feat-096 : messages en file, bandeau, relance manuelle).
La bonne architecture est donc un **clone complet via le git du terminal**, pas
un tarball + API Git Data (moins robuste, sans historique) :

- clone = tout l'historique et toutes les branches, le dépôt devient un projet
  ordinaire de l'app (éditeur, navigateur, patchs, commits locaux) ;
- hors ligne, TOUT fonctionne déjà : lecture, édition, commits locaux ;
- au retour du réseau : fetch + pull + push groupés, conflits détectés.

## 3. Réalisations (env-001 → env-006)

### env-001 — clonage complet (`envClonerEtOuvrir`)

`git clone [--branch <b>] "<url-avec-jeton>" <projet>` (aucun `--depth` :
historique complet), dans un nouveau projet `~/projects/<repo>` sélectionné
aussitôt. Le jeton n'est jamais laissé en config (`credential.helper` désactivé
après sync, discipline ghc-004). Cœur suspend partagé UI/outil IA.

### env-002 — connexion en deux étapes + logique pure testable

- `EnvParse.kt` (data) : `depotDeOrigine` (https avec ou sans jeton dans l'URL —
  le jeton ne fuite jamais dans le résultat —, ssh `git@`, refus hors github.com),
  `compteConflits` (porcelaine v1 : UU AA DD UA AU DU UD), `compteModifs`,
  `compteRevList` (-1 = inconnu, jamais de faux « à jour »).
- Dialogue `EnvConnexionDialog` : liste des dépôts accessibles au jeton
  (`listeDepots`) → choix de branche (`listerBranches`), option URL github.com
  libre sans jeton, retour en arrière (`envRevenirDepots`).
- `EnvParseTest` : 15 tests JVM sans réseau (7 parsing origine, 4 conflits,
  3 modifs, 3 rev-list… répartis en 12 fonctions de test).

### env-003 — synchronisation groupée (`envSyncCore`)

fetch origin → compteurs avance/retard (`rev-list --count`) → `pull --no-edit`
si retard → `push -u` si travail local. Rapport parlant fr/en : à jour, N
récupérés, N poussés, conflits détectés, échec push. **Refus net hors ligne** :
rien ne part, rien ne se perd (`err_env_hors_ligne`).

### env-004 — carte « Environnement GitHub » dans l'écran Git

`EnvCarte` (GitEnvironnement.kt) : dépôt, branche, badges à jour / N en avance /
N en retard / N modifiés / N conflits / hors ligne, bouton Synchroniser
(désactivé hors ligne), rafraîchissement instantané **sans aucun réseau**.
Affichée aussi quand aucun dépôt n'existe (proposition de connexion au-dessus
de CloneCard). `refreshEnvStatut` au passage sur l'écran.

### env-005 — la file hors ligne repart toute seule

`registerDefaultNetworkCallback` (minSdk 26 ≥ 24) : au retour du réseau,
`reessayerHorsLigne()` vide la file du chat automatiquement — plus besoin de
toucher le bandeau (feat-096 complété).

### env-006 — l'IA contrôle l'environnement

3 outils nouveaux, même contrat de sécurité que `github_*` :

| Outil | Effet | Confirmation |
|---|---|---|
| `env_connect` | clone complet d'« owner/repo » → nouveau projet = environnement | oui (porte écriture) |
| `env_statut` | état lisible : dépôt, branche, avance/retard, modifs, conflits, en ligne/hors ligne | lecture directe |
| `env_sync` | pull + push groupés de la branche courante | oui (porte écriture) |

`executerOutilEnv` centralise l'exécution (lecture seule, écriture conversation,
confirmation, `outilAutoriseForce`), routage `startsWith("env_")`, étiquettes
`tool_env_*` fr/en, prompt système étendu. **Le jeton reste invisible** : ni
arguments, ni résultats, ni journaux.

## 4. Playground (dépôt public d'entraînement)

15 fichiers, 410 lignes : mini-projet `biblio` (Python 3.10+, type hints) avec
**6 bogues volontaires documentés** (B1 recherche sensible à la casse, B2 ISBN
dupliqué, B3 durée 0 jour, B4 retard négatif, B5 ZeroDivision, B6 CSV/JSON) et
**2 fonctionnalités à implémenter** (F1 livres_par_auteur, F2
livre_plus_emprunte) ; 12 tests pytest dont **9 échouent volontairement**
(vérifié avant push : `9 failed, 3 passed`) ; jeu de données, docs
d'architecture, licence MIT, CI pytest. Mission idéale pour l'agent : « lis
TODO.md, corrige les bogues, fais passer la suite ».

## 5. Gardes (scripts/garde16.py)

Parité fr/en stricte (**763 clés ×2**, 44 nouvelles), 673 références
`R.string` résolues ×2, équilibre accolades/parenthèses hors chaînes identique
à HEAD (19 fichiers .kt modifiés + nouveaux à (0,0)), aucun jeton réel dans le
code de production (fixtures de test exclues : faux jetons volontaires pour
`ZLog.masquer`), 3 outils `env_*` déclarés exactement une fois et listés dans
`nomsOutils`, points d'entrée VM présents. Portage Python de `depotDeOrigine`
validé sur 7 cas avant push.

Note de méthode : la garde v1 a signalé `CorrRegressionTest.kt` comme « jeton
réel » — c'était une **fausse alerte** (fixture volontaire du test de masquage) ;
la garde v2 scanne `app/src/main` + docs uniquement.

## 6. Sécurité — ce qui ne change pas

- Jeton dans EncryptedSharedPreferences, injecté au moment de l'exécution.
- Jamais dans les arguments IA, les résultats, les journaux (`ZLog.masquer`)
  ni la config du dépôt (`credential.helper` désactivé après chaque sync).
- `env_connect` et `env_sync` confirmés par l'utilisateur ; refus individuel
  par outil dans Permissions & sécurité ; lecture seule et « écriture
  désactivée pour la conversation » restent des remparts.
- Hors ligne : aucun envoi, aucun écrasement — conflits signalés, jamais
  fusionnés en silence.

## 7. Restes tracés

- i18n-001/i18n-002, nettoyage-002 (hérités de la session 14, non touchés).
- Les **100 améliorations** (PROPOSITIONS-session-12.md) : vagues suivantes,
  10-15 par session, chacune avec sa CI et sa release.
- Suite possible de l'environnement : historique des synchronisations,
  sélection multi-branches, badge hors ligne dans la barre du chat.

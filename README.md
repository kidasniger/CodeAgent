# ZAgent — agent de code sur téléphone

ZAgent (dépôt `CodeAgent`) est une application Android (Jetpack Compose) qui embarque
un agent de code IA : il discute, lit et modifie les fichiers du projet, exécute des
commandes dans un terminal Termux intégré, gère Git, s'auto-met à jour depuis les
releases GitHub et fonctionne avec plusieurs fournisseurs d'IA (z.ai GLM, NVIDIA NIM,
OpenRouter, Groq, passerelles personnalisées).

## Fonctionnalités principales

- **Chat agent avec outils** : `read_file`, `write_file`, `edit_file`, `run_command`,
  `list_dir`, `search_in_files`, `update_todos`… avec confirmations et diff avant écriture.
- **Terminal réel** : bootstrap Termux (`com.zagent.app`), onglets, ANSI, clavier étendu.
- **Éditeur de code** : coloration syntaxique, onglets, aperçu HTML local (PreviewServer).
- **Git intégré** : statut, diff, commit, écran dédié + raccourci.
- **Fournisseurs d'IA multiples** : une clé par fournisseur, détection automatique
  (`nvapi-`, `sk-or-`, `gsk_`), repli automatique en cas de surcharge.
- **Mises à jour in-app** : vérification des releases GitHub, vérification SHA-256 +
  certificat, téléchargement WorkManager (reprise sur interruption, option Wi-Fi),
  canal stable ou bêta.

## Compiler

```bash
./gradlew assembleDebug            # APK debug
./gradlew :app:testDebugUnitTest   # tests unitaires
./gradlew :app:lintDebug           # lint
./gradlew assembleRelease          # APK release signé (keystore requis)
```

Prérequis : JDK 17 (aligné avec le CI), Android SDK (compileSdk 37).
Le dépôt GitHub utilisé pour les mises à jour se surcharge via `gradle.properties` :

```properties
ZAGENT_GITHUB_OWNER=kidasniger
ZAGENT_GITHUB_REPO=CodeAgent
```

## CI et secrets

Le workflow `.github/workflows/build.yml` :
- **build** (toutes les branches) : APK debug, tests unitaires, lint — rapports en artefacts.
  Sans secrets, un keystore debug est généré à la volée.
- **release** (main uniquement, environnement GitHub « production ») : APK release signé,
  vérification apksigner, notes de version auto, `.sha256` publié avec l'APK.

Secrets attendus : `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`
(keystore de signature release — jamais commité). Le keystore debug par défaut
(`debug.keystore`, mot de passe « android ») ne sert qu'en local/debug.

`.env.example` ne contient aucun secret : les clés IA sont saisies par l'utilisateur
dans Réglages › Fournisseurs et stockées chiffrées sur l'appareil.

## Architecture

Voir [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) (couches data, ui, terminal, agent,
update) et [docs/README.md](docs/README.md) pour l'index complet de la documentation.

## Suivi du projet

Le développement suit un backlog de 600 prompts organisés en 6 catégories
(Mises à jour, Corrections, Fonctionnalités, UI, UX, Résolution d'erreurs) :
état d'avancement dans [docs/backlog/BACKLOG.md](docs/backlog/BACKLOG.md).

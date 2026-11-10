# Architecture ZAgent

## Vue d'ensemble

Application Android single-module (`:app`) en Jetpack Compose, organisée en couches :

```
com.zagent.app
├── MainActivity.kt         — point d'entrée, raccourcis, predictive back
├── data/                   — accès aux données et logique métier
│   ├── AiRepository.kt     — clients HTTP des fournisseurs (flux SSE)
│   ├── AgentTools.kt       — définition et exécution des outils de l'agent
│   ├── ConversationRepository.kt — historique local (JSON chiffré/persistance)
│   ├── FileRepository.kt   — fichiers du projet (SAF, home privé de l'appli)
│   ├── Fournisseurs.kt     — z.ai, NVIDIA NIM, OpenRouter, Groq + personnalisés
│   ├── ModelCatalog.kt     — modèles GLM/NIM (capacités, contexte, tarifs)
│   ├── SettingsManager.kt  — EncryptedSharedPreferences
│   └── UsageRepository.kt  — tokens/coûts, ActivityRepository — journal
├── agent/                  — AgentService (service au premier plan, dataSync),
│                             AgentNotifier (notifications)
├── terminal/               — TerminalController (sessions Termux)
├── ui/
│   ├── navigation/         — ZRoutes (routes typées), ZNavHost (deep links zagent://)
│   ├── screens/            — écrans chat, éditeur, terminal, git, réglages…
│   ├── components/         — bibliothèque de composants (ZComponents…)
│   ├── editor/             — EditorState, SyntaxHighlighter, PreviewServer/Pane
│   ├── theme/              — Theme, Color, Type, Glass (Liquid Glass)
│   └── viewmodel/          — MainViewModel
└── update/                 — vérification, téléchargement, vérification et
                              installation des releases GitHub (UpdateManager…)
```

## Décision de nommage du package (MA37)

`applicationId = "com.zagent"` alors que `namespace = "com.zagent.app"`.

- **applicationId** : identifiant d'installation immuable auprès de Google Play /
  Android. Il a été posé à `com.zagent` avant l'introduction du namespace ;
  le changer désinstallerait l'application de tous les appareils et briserait
  le canal de mise à jour (`UpdateConfig` + keystore).
- **namespace** : préfixe des classes générées (BuildConfig, R) et du code source
  (`com.zagent.app.*`). Il est libre et cohérent avec le reste du projet.

**Décision : conserver cette dissociation**, documentée ici ; elle n'a d'effet
que sur l'identité d'installation, pas sur le code.

## Flux principal

1. `MainActivity` installe le thème, l'AppCompat (langue par application) et le
   NavHost (`ZRoutes` : chat, files, editor?path, terminal, settings, settings/appearance).
2. Le chat envoie le contexte (`buildContextBlock` : pièces jointes + mentions
   `@fichier`) à `AiRepository`, qui diffuse la réponse SSE du fournisseur choisi.
3. Les appels d'outils passent par `AgentTools.executeTool` → `FileRepository` /
   `TerminalController`, avec confirmations (`ChatConfirmations`).
4. Les mises à jour : `UpdateManager.check` → `ReleaseParser` (drafts refusées,
   prereleases en canal bêta, asset `universal`/`arm64-v8a` préféré) →
   `UpdateDownloadWorker` (reprise par `Range`, Wi-Fi uniquement en option) →
   `ApkVerifier` (SHA-256 + certificat) → installateur système.

## Métriques du compilateur Compose (MA45, méthode)

Pour lister les composables instables après un build réel :

```bash
./gradlew :app:assembleDebug \
  -Pandroid.compose.compiler.metrics=build/compose-metrics \
  -Pandroid.compose.compiler.reports=build/compose-reports
```

Le rapport `*-composables.txt` liste les paramètres instables (par ex. `List<T>`
non immuable dans `ChatComponents.kt`) ; c'est la base du travail de stabilisation
(`remember`, `derivedStateOf`, clés stables) à mener en session de build.

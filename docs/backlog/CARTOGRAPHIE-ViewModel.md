# Cartographie du MainViewModel (corr-001)

Audit demandé par le prompt corr-001 : « MainViewModel.kt pèse 138 Ko : liste ses
responsabilités sans coder, avec le découpage proposé ». **Ce document est
l'inventaire ; les extractions correspondantes sont réalisées par corr-002 à
corr-008 (aucun changement de comportement, API publique conservée).**

État au moment de l'audit : `ui/viewmodel/MainViewModel.kt` — 3 104 lignes,
≈ 139 Ko, une seule classe `MainViewModel : AndroidViewModel`.

## Responsabilités recensées

| # | Domaine | Éléments | Lignes (approx.) |
| --- | --- | --- | --- |
| 1 | Navigation | `navRouteRequest`, `navBackRequest`, `currentScreen`, `navigateTo`, `navigateBack`, `consumeNavRouteRequest`, `syncScreenFromRoute`, `navigationPreviousScreen` | 100-159 |
| 2 | Réglages | `settingsManager`, miroirs d'état (thème, polices, verre, haptique, langue, onboarding, `maxStepsInput`, prix…) et leurs `update*` | 160-448, 628-650, 676-700 |
| 3 | Fournisseurs IA | `fournisseurs`, `clePourModele`, `majFournisseur`, `ajouterFournisseur`, `supprimerFournisseur`, tests de connexion, favoris, mode Duo, `chatModel` | 170-330, 662-700 |
| 4 | Conversation (chat) | `messages`, `conversationId`, brouillons, `conversationList`, `persistCurrentConversation`, `switchToConversation`, `deleteConversation`, `toggleConversationPin`, pièces jointes | 447-520, 2504-2620 |
| 5 | Boucle agent | `sendMessage`, mode Duo (`runDebat`), boucle d'outils, `stopAgent`, `retryLast`, limites d'étapes, erreurs réseau | 1355-1594, 1063-1110 |
| 6 | Confirmations d'outils | `pendingWrite`, `pendingDelete`, `pendingCommand`, `pendingAsk` + leurs `resolve*` | 535-592, 2289-2503 |
| 7 | État d'exécution | `liveCommand`, `liveOutput`, `liveText`, `liveReasoning`, `todos`, `summaryFiles/Commands`, `fileDiffCounts`, `agentUndo`, `console` | 552-620 |
| 8 | Git | `gitSheetOpen`, `gitChanges`, `gitLog`, `gitBranches`, commit/checkstade, message de commit généré | 701-764, 2687-2843 |
| 9 | Explorateur de fichiers | `browserFilePreview`, `browserChildren`, `expandedDirs`, `ensureLoaded`, `toggleDir`, création/renommage/suppression | 1138-1354 |
| 10 | Éditeur | `editor = EditorController(...)`, `openFilePath`, `requestCodeApply` | 1111-1120, 2060-2073 |
| 11 | Terminal | `terminal = TerminalController(...)`, `openTerminalInProject` | 1120, 2074+ |
| 12 | Projets | import ZIP/dossier, export, `listProjects`, `createProject`, `selectProject`, favoris | 765-1024 |
| 13 | Historique/recherche | `searchHistory`, `runGlobalSearch`, usage + activité (snapshots) | 2630-2686, 2865-2919 |
| 14 | Export conversation | markdown, PDF, `buildConversationMarkdown` | 2958-3077 |
| 15 | Surfaces UI (feuilles) | `isHistoryOpen`, `isSearchOpen`, `isGitScreenOpen`, `isTemplatesOpen`, `isShareSheetOpen`, `pendingOpenModels` | 3078-3104 |
| 16 | Aperçu | `previewRequest`, `openPreview`, `PreviewServer`, écran d'aperçu | 600-627, 2843-2853 |

## Découpage proposé (et appliqué)

1. **NavigationViewModel** (corr-003) — bloc 1. Les événements ponctuels
   (`navRouteRequest`, `navBackRequest`) deviennent un `SharedFlow<NavEvent>`
   (corr-009).
2. **SettingsViewModel** (corr-004) — bloc 2 hors fournisseurs : tous les miroirs
   d'état qui ne font que refléter `SettingsManager` + leurs `update*`.
3. **ChatViewModel** (corr-002) — blocs 4 (partie données : liste de messages,
   identifiant et index de conversation, brouillons). La boucle agent (bloc 5)
   reste dans MainViewModel, qui écrit via ChatViewModel.
4. **TerminalViewModel** (corr-005) — la gestion des onglets du terminal sort de
   TerminalController (voir `terminal/TerminalViewModel.kt`).
5. **AppContainer** (corr-006) — les dépôts (`SettingsManager`, `AiRepository`,
   `ConversationRepository`, `UsageRepository`, `ActivityRepository`) sont
   instanciés dans un conteneur unique, injecté dans MainViewModel.
6. **Interfaces de dépôts** (corr-007) — `AiRepositoryApi`, `FileRepositoryApi`,
   `ConversationRepositoryApi` pour les simulations de tests.
7. **UiState par écran** (corr-008) — regroupement progressif des états par
   écran ; commencé avec `FileBrowserUiState` et `GitUiState`.
8. Les blocs 5/6/7 (moteur agent + exécution d'outils) restent dans
   MainViewModel : ils forment le noyau de médiation entre l'IA, les outils et
   l'UI ; leur extraction (AgentViewModel/ToolViewModel) est l'étape suivante
   une fois les ViewModel ci-dessus stabilisés en CI.

## Compatibilité

MainViewModel conserve son API publique exacte : propriétés et fonctions
déléguent vers les nouveaux ViewModel (`val navigation`, `val settings`,
`val chat`). Aucun écran n'est modifié pour cette série.

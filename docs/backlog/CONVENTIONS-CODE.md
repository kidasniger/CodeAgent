# Conventions de code ZAgent (corr-013)

## Langue des identifiants : le français

**Décision** : les identifiants (fonctions, propriétés, classes, variables) sont
écrits **en français**, avec les exceptions listées plus bas. Le codebase était
mixte (anglais/Français) ; le français est dominant dans les fichiers les plus
vivants (`MainViewModel`, `ChatScreen`, `SettingsManager` : `sendMessage`,
`clePourModele`, `majFournisseur`, `pendingWrite`, `folderLabel`…) et le produit
est entièrement francisé.

Cette convention a été appliquée dans tous les fichiers touchés par la série
« Corrections » : nouveaux fichiers (`ChatViewModel`, `NavigationViewModel`,
`SettingsViewModel`, `TerminalViewModel`, `ChatRows`, `ZButtons`, `ZCards`,
`ZMenus`, `ZCommon`, `SheetsDialogs`, `OverlayComponents`, `ChatInputBar`,
`AppContainer`, `Repositories`, `UiState`, `ZLog`, `Constants`…) et
renommages (`Lot5*` → `Overlay*`, `InputBarLot4` → `ChatInputBar`).

### Exceptions assumées (anglais autorisé)

1. **API publiques tierces** : noms imposés (`onCreate`, `Composable`,
   `ViewModel`, `OkHttp`, `collectAsStateWithLifecycle`…).
2. **Vocabulaire technique sans équivalent français établi** : `ViewModel`,
   `StateFlow`, `SharedFlow`, `LazyColumn`, `Serializable`, `dispatchers`,
   `enum`…
3. **Protocoles et formats** : `baseUrl`, `apiKey`, `SSE`, `CSP`, `SHA-256`.
4. **Chaînes affichées à l'utilisateur** : suivre les ressources `strings.xml`
   (fr/en), pas les identifiants.

### Règles de style associées

- Noms de fonctions : verbe d'action en français (`charger`, `maj…`, `bascule…`,
  `resolve…` quand le terme est consacré).
- Booléens : préfixes `est`, `a`, `peut`, `doit` ou forme adjectivale
  (`isLoading`, `stepLimitReached` restent admis pour les API exposées).
- Commentaires et KDoc : français également.
- Ne pas renommer « en masse » des symboles stables sans prompt dédié : la
  convention s'applique aux fichiers que l'on touche (règle du prompt corr-013).

## Architecture (rappel corr-001 → corr-014)

- `ui/viewmodel/` : `MainViewModel` (façade) + `ChatViewModel`,
  `NavigationViewModel`, `SettingsViewModel` ; `di/AppContainer` pour les dépôts.
- `ui/components/chat/` : composants propres au fil de discussion ;
  `ui/components/common/` : boutons, cartes, menus, overlays, utilitaires.
  Les composants d'éditeur vivent dans `ui/editor/` et `ui/screens/Editor*.kt`,
  d'où l'absence de sous-package `editor/` dans `ui/components`.
- Un seul fichier par famille de composants, noms décrivant le rôle
  (pas de « Lot4/Lot5 », vestiges des lots de développement).

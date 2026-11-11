# LOT 6 · Navigation & mise en page (MA71 → MA78)

Résumé des mises à jour 71 à 78, leurs fichiers, et la note de migration
targetSdk 34 demandée par MA74.

## MA71 — Navigation Compose à la place des booléens

- **Nouveau** : `ui/navigation/ZRoutes.kt` (routes typées) et
  `ui/navigation/ZNavHost.kt` (hôte de navigation unique).
- `MainActivity.kt` : le `when` sur les écrans est remplacé par `ZNavHost` ;
  la double pression pour quitter est conservée sur l'écran racine (MA74).
- `MainViewModel.kt` : les booléens `isSettingsOpen`, `isTerminalOpen`,
  `isEditorOpen`, `isFileBrowserOpen` sont supprimés. L'API publique ne change
  pas : `navigateTo(AppScreen)`, `navigateBack()`, `currentScreen` continuent
  d'être appelés par tous les écrans, mais ils alimentent des requêtes de route
  (`navRouteRequest`, `navBackRequest`) exécutées par le NavHost, puis
  `currentScreen` est resynchronisé depuis la route réelle
  (`syncScreenFromRoute`). Le back stack, les deep links (`zagent://chat`,
  `zagent://terminal`, `zagent://editor?path=…`, `zagent://settings`,
  `zagent://settings/appearance`, `zagent://files`) et la restauration d'état
  (`saveState`/`restoreState`) sont gérés par Navigation Compose.
- Dépendance : `androidx.navigation:navigation-compose:2.8.9`
  (`libs.androidx.navigation.compose` — déjà déclarée dans
  `gradle/libs.versions.toml`, ajoutée à `app/build.gradle.kts`).
- Les feuilles du LOT 5 (historique, recherche, git, modèles, partage…) restent
  des superpositions modales au-dessus du NavHost : elles glissent du bas
  (MA72) et sont ouvertes depuis plusieurs écrans, ce qui correspond à un
  comportement de feuille plutôt que de destination.

## MA72 — Transitions entre écrans

Dans `ZNavHost.kt` : fondu + glissement horizontal de 24 dp en 250 ms avec
l'easing `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)` (« emphasizedDecelerate »)
pour les écrans de premier niveau ; glissement du bas pour `settings` et
`settings/appearance`. Tout est désactivé quand « Réduire les animations » est
actif — nouveau réglage : `SettingsManager.reduceAnimations` +
interrupteur dans **Réglages › Apparence › Animations**
(`AppearanceSettingsScreen.kt`).

## MA73 — Barre d'onglets adaptative

`ui/components/FloatingTabBar.kt` :

- se masque en glissant vers le bas au défilement vers le bas, remonte au
  défilement vers le haut (le détecteur de défilement reste dans
  `MainActivity`, la barre anime son propre décalage et son opacité) ;
- appui long sur un onglet : menu rapide en verre (MA78) —
  Chat → « Nouvelle conversation », Fichiers → « Changer de projet »,
  Terminal → « Nouveau terminal » (`terminal.addTab()`) ;
- badge numérique rouge sur l'onglet Chat s'il y a une confirmation en attente
  (`MainViewModel.pendingConfirmationCount` : écriture, question, suppression,
  commande).

## MA74 — Geste retour prédictif (Android 14) et migration targetSdk

- `AndroidManifest.xml` : `android:enableOnBackInvokedCallback="true"`.
- `MainActivity.kt` : `PredictiveBackHandler` sur l'écran racine uniquement
  (double pression pour quitter conservée). Sur les autres écrans, le back
  stack du NavHost est dépilé nativement par Navigation Compose, avec la
  transition `popEnter`/`popExit` de MA72.

### Impact d'un passage de targetSdk 28 à 34 (à NE PAS faire à la légère)

Le projet cible aujourd'hui **targetSdk 28**. `enableOnBackInvokedCallback`
n'est pris en compte par le système que si `targetSdk ≥ 33` : tant que
l'app cible 28, l'attribut est inoffensif et `PredictiveBackHandler` passe par
le dispatcher classique (`onBackPressed`), donc rien ne casse. Pour passer à
34 prudemment, dans cet ordre :

1. **Notifications (API 33+)** : `POST_NOTIFICATIONS` devient une permission à
   demander à l'exécution (`android.permission.POST_NOTIFICATIONS` est déjà
   déclarée). Ajouter une demande au premier lancement, sinon les notifications
   de l'agent (`AgentNotifier`) restent silencieuses.
2. **Stockage (API 30+)** : `READ_EXTERNAL_STORAGE`/`WRITE_EXTERNAL_STORAGE`
   ne suffisent plus ; scoped storage obligatoire — `READ_MEDIA_IMAGES` /
   `READ_MEDIA_VIDEO` (API 33+) ou `MANAGE_EXTERNAL_STORAGE` (rejeté par le
   Play Store). ZAgent travaille déjà via SAF et son home privé
   (`/data/data/com.zagent/files`), donc l'impact est faible : vérifier les
   lectures directes de `/sdcard` dans `FileRepository`.
3. **Foreground service (API 34)** : tout service en premier plan doit
   déclarer un type précis — pour `AgentService` ce serait
   `android:foregroundServiceType="dataSync"` + permission
   `FOREGROUND_SERVICE_DATA_SYNC`, et l'app ne peut plus démarrer un FGS
   depuis l'arrière-plan.
4. **Receivers enregistrés dynamiquement (API 34)** : préciser
   `RECEIVER_EXPORTED` / `RECEIVER_NOT_EXPORTED`.
5. **Geste retour (API 33+)** : avec `enableOnBackInvokedCallback=true`, les
   `OnBackPressedCallback` restent supportés, mais toute interception « brute »
   de la touche retour disparaît — c'est déjà le cas ici (Navigation Compose).
6. En pratique : passer d'abord à **33**, valider sur un appareil API 33/34,
   puis à **34** ; `minSdk 26` reste inchangé et le repli API 26–30 est
   conservé partout (voir MA72 et le flou verre du LOT 3).

## MA75 — Deux panneaux pour tablettes et pliables

- **Nouveau** : `ui/layout/AdaptiveScaffold.kt`.
- Dépendance : `androidx.compose.material3:material3-window-size-class`
  (`libs.androidx.compose.material3.window.size.class`, version gérée par le
  BOM) ajoutée à `app/build.gradle.kts`.
- En largeur **Medium/Expanded** (`calculateWindowSizeClass` dans
  `MainActivity`), l'écran Chat affiche un panneau **Historique** à gauche
  (liste des conversations, épinglées en premier) et la conversation à droite.
  En **Expanded**, la route Éditeur affiche **Éditeur + Chat** côte à côte.
  En Compact, comportement du téléphone inchangé.

## MA76 — Paysage : éditeur + chat côte à côte

Dans `AdaptiveScaffold.kt` (`SplitEditorChat`) : en paysage téléphone
(orientation `landscape` + largeur Compact), la route Éditeur divise l'écran
**55 / 45** (60/40 sur tablette), avec une **poignée de redimensionnement**
(glissement horizontal, proportion mémorisée pendant la session) et des
boutons « plein écran » pour chaque panneau (bouton de restauration en bas à
gauche). Éditeur à gauche, conversation à droite.

## MA77 — Raccourcis d'application

- `res/xml/shortcuts.xml` : **Nouvelle conversation**, **Terminal**,
  **Dernier projet** (icônes `res/drawable/ic_shortcut_*.xml`, libellés dans
  `res/values/strings.xml`).
- `AndroidManifest.xml` : meta-data `android.app.shortcuts` +
  `launchMode="singleTop"`.
- `MainActivity.kt` : `handleShortcutIntent` dans `onCreate` et `onNewIntent` ;
  actions traitées par `MainViewModel.handleAppShortcut` (nouvelle
  conversation / terminal / explorateur du dernier projet).

## MA78 — Menu « ⋯ » contextuel unifié

- `ui/components/ZComponents.kt` : **ZMenu** (conteneur en verre arrondi,
  construit sur `GlassDropdownMenu`), **ZMenuItem** (icône + libellé +
  raccourci à droite en mono + éléments destructifs en rouge) et
  **ZMenuSeparator**.
- `ui/screens/ChatScreen.kt` : le groupe d'actions de la barre du haut devient
  `Historique · Recherche · Partager · ⋯` ; le menu ⋯ regroupe Explorateur de
  fichiers, Git, Modèles, Modèles de prompts, séparateur, Réglages. Aucune
  fonction existante n'a été retirée, elles sont réorganisées.
- Le même ZMenu est utilisé par la barre d'onglets (MA73) et peut être repris
  tel quel dans l'éditeur et l'explorateur de fichiers.

## maj-040 — Deep links Git et Modèles

Deux nouveaux liens internes complètent le schéma `zagent://` :

- `zagent://git` — ouvre l'écran Git (état `isGitScreenOpen` du ViewModel).
- `zagent://models` — ouvre les Réglages avec la feuille Modèles en attente
  (`pendingOpenModels`), comme le raccourci d'application équivalent.

Ces deux cibles ne sont pas des routes du `ZNavHost` (Git est un état du
ViewModel, Modèles est une feuille du LOT 5 accrochée aux Réglages) : ils sont
traités au niveau de `MainActivity.handleShortcutIntent` (VIEW + dataString),
qui réutilise `MainViewModel.handleAppShortcut`. Le manifeste déclarait déjà le
schéma `zagent` en `VIEW/BROWSABLE`, aucune modification du manifeste n'a été
nécessaire. Liens à tester :

```
adb shell am start -a android.intent.action.VIEW -d "zagent://git"
adb shell am start -a android.intent.action.VIEW -d "zagent://models"
```

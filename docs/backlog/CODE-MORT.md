# Code mort détecté (corr-016)

Analyse statique faite sans toolchain Android (lint indisponible dans
l'environnement d'audit) : croisement de chaque déclaration top-level avec
toutes ses références dans le projet. Un symbole cité uniquement par sa
déclaration est listé ci-dessous. **Aucune suppression automatique n'a été
appliquée sans preuve compilable** : la liste doit être validée par un run
`./gradlew lint` avant suppression définitive (les KDoc référencées dans
CHANGELOG/docs peuvent fausser le compteur dans l'autre sens).

| Fichier | Type | Symbole | Références trouvées |
| `ui/components/chat/SheetsDialogs.kt` | val | `knownModelDescriptions` | 0 hors déclaration |
| `ui/components/common/Previews.kt` | fun | `ApercuBarreOnglets` | 0 hors déclaration |
| `ui/components/common/Previews.kt` | fun | `ApercuBoutons` | 0 hors déclaration |
| `ui/components/common/Previews.kt` | fun | `ApercuCartesVerreQualite` | 0 hors déclaration |
| `ui/components/common/Previews.kt` | fun | `ApercuCartesVerreRapide` | 0 hors déclaration |
| `ui/screens/SettingsScreen.kt` | fun | `ThemeSegmented` | 0 hors déclaration |
| `ui/theme/Theme.kt` | fun | `mouvementReduit` | 0 hors déclaration |
| `ui/theme/Type.kt` | val | `MonoSmall` | 0 hors déclaration |
| `ui/viewmodel/UiState.kt` | data class | `GitUiState` | 0 hors déclaration |
| `update/UpdateFileProvider.kt` | class | `UpdateFileProvider` | 0 hors déclaration |

## Sort appliqué (session corr)

| Symbole | Décision | Motif |
| --- | --- | --- |
| `knownModelDescriptions` | **supprimé** | map privée sans aucune référence |
| `ThemeSegmented` | **supprimé** | composable privé sans appel (remplacé par le sélecteur de Thème réglages) |
| `mouvementReduit` | **supprimé** | logique inlinée dans MainActivity (via `echelleAnimationsSysteme`) et ZTheme |
| `MonoSmall` | **supprimé** | style typographique jamais utilisé |
| `Apercu*` (Previews.kt) | **conservé** | fonctions `@Preview` : points d'entrée de l'outil Android Studio, pas du code mort |
| `UpdateFileProvider` | **conservé** | référencé par `AndroidManifest.xml` (provider), invisible de Kotlin |
| `GitUiState` | **conservé** | défini par corr-008 pour l'adoption progressive |

Relance recommandée : `./gradlew lint` en CI pour la détection exhaustive
(ressources inutilisées, classes issues des dépendances).

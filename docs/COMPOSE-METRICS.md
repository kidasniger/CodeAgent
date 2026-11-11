# maj-045 — Métriques du compilateur Compose

## Activer les rapports

Les rapports sont **opt-in** (aucun coût sur le build ordinaire) :

```bash
./gradlew assembleDebug -Pcompose.metrics=true
```

Sortie dans `app/build/reports/compose/` :
- `*-comps.csv` — un composable par ligne, avec le flag `skippable` (1 = skippable) ;
- `*-classes.csv` — stabilité des classes (`stable` / `runtime` / `unstable`).

La propriété est câblée dans `app/build.gradle.kts` (bloc `composeCompiler`),
activée uniquement quand `-Pcompose.metrics=true` est passé.

## Audit de ChatComponents.kt

Relevé des composables publics et de leur stabilité attendue **avant** correctif :

| Composable | Paramètres clés | Stabilité | Cause |
|---|---|---|---|
| `MarkdownText` | `text: String`, `highlight: String`, lambdas | Stable | tous types stables |
| `AnnotatedText` (privé) | `spans: List<MdSpan>` | **Instable** | `List<>` non stabilisé |
| `CodeBlock` | `code: String`, `language: String` | Stable | |
| `DiffPanel` | `diff: String` + lambdas | Stable | |
| `ToolChip` | `String` + lambdas | Stable | |
| `ToolGroupSummary` | `Int`, `Boolean`, lambda | Stable | |
| `AttachmentThumbsRow` (fichier AttachSheet.kt, même famille) | `List<ChatAttachment>` | **Instable** | `List<>` + `ChatAttachment` contenant `Uri?` |

### Causes racines

1. **`List<MdSpan>` / `List<MdBlock>`** : pour le compilateur, `List` est
   mutable en théorie (`ArrayList` implémente l'interface) → paramètre
   instable → le composable recompose à chaque recomposition parente même si
   rien n'a changé.
2. **`ChatAttachment`** : data class avec champ `Uri?`. `android.net.Uri` est
   traité comme instable (classe plateforme non annotée).

### Correctifs appliqués

- `MdSpan` et `MdBlock` (et leurs sous-classes) annotés `@Immutable` — l'arbre
  markdown est construit par `parseInlineSpans`/`parseMarkdownBlocks` puis
  **jamais muté** ; le contrat d'`@Immutable` est donc respecté. `AnnotatedText`
  et `MarkdownText` deviennent skippables quand le texte ne change pas.
- `MainViewModel.ChatAttachment` annoté `@Immutable` — l'Uri n'est remplacée
  qu'une seule fois (redimensionnement corr-060) via une nouvelle instance
  (`copy()`), jamais mutée en place. `AttachmentThumbsRow` skippe désormais
  pendant la saisie.

### Suites recommandées

- Relancer `assembleDebug -Pcompose.metrics=true` après chaque gros lot UI et
  vérifier que les composables « chauds » (fil de chat, onglets éditeur)
  restent `skippable=1`.
- Si un nouveau composable reçoit une `List` d'objets métier, annoter la classe
  métier `@Immutable` (données figées) ou `@Stable` (données observables mais
  comparables) plutôt que convertir en `ImmutableList` (kotlinx.collections.immutable).

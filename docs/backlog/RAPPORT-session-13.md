# Session 13 — Écrans adaptables pour n'importe quel téléphone

## Demande

« D'abord les écrans il faut qu'ils soient tous adaptables pour n'importe quel
téléphone » — campagne responsive sur l'intégralité de l'interface, après la
session 12 (historique réconcilié, outils GitHub à jeton interne, détection des
modèles, 100 propositions).

## Audit réalisé (33 écrans + composants)

Parcours systématique de tous les écrans (`ui/screens`, `ui/screens/settings`,
`ui/editor`, `ui/layout`, `ui/components`) avec quatre grille de lecture :
présence d'un défilement vertical, gestion des insets système/clavier, hauteurs
figées, textes tronqués sans ellipsis.

**Déjà conformes** (socle des sessions précédentes) :
- `MainActivity` : edge-to-edge + WindowSizeClass calculé et propagé.
- `AdaptiveScaffold` : tablettes/pliables (historique + chat côte à côte),
  paysage éditeur↔chat redimensionnable, posture « table à dessin » (ui-071/073).
- 12 écrans hébergés dans `OverlayScreen` (insets + scroll + ellipsis titres).
- `SettingsPageScaffold` : scroll natif ; Terminal : ime/navigationBars + scroll ;
  Editor : statusBars + ime + mode plein écran ; chat : imePadding ×2 ;
  FloatingTabBar : navigationBarsPadding.
- Blocs de code : horizontalScroll présent partout (15 fichiers).

**Défaillances trouvées et corrigées** (resp-001 à resp-009) :

| # | Problème | Correctif |
|---|----------|-----------|
| resp-001 | Panneaux à hauteur figée débordant en paysage/petit écran | Kit `ui/theme/Adaptive.kt` : `hauteurEcranDp()` + `hauteurBorneDp(cible, fraction)` |
| resp-002 | 3 dialogues FileBrowser figés 300/320/360 dp | Bornes dynamiques (0.55 × écran) |
| resp-003 | ShareSheet sans scroll (8 éléments + clavier) | `verticalScroll` |
| resp-004 | AttachSheet sans scroll | `verticalScroll` |
| resp-005 | Console éditeur figée 140 dp | `hauteurBorneDp(140.dp, 0.38f)` |
| resp-006 | Console JS preview figée 180 dp | `hauteurBorneDp(180.dp, 0.42f)` |
| resp-007 | Connecteurs de timeline figés (74/66 dp) — coupure visuelle à police ×2 | `IntrinsicSize.Min` + `weight(1f)` |
| resp-008 | 17 textes `maxLines=1` sans ellipsis (coupure nette sans « … ») | `TextOverflow.Ellipsis` partout |
| resp-009 | PrDialog : description longue inaccessible | Contenu scrollable |

## Garde-fous

- Vérification scriptée : imports requis par usage, accolades équilibrées
  (hors chaînes), zéro `maxLines=1` sans overflow restant.
- Aucune chaîne nouvelle : pas de modification strings.xml (fr/en inchangés).
- Comportement identique sur un téléphone portrait standard (les cibles
  historiques 300/320/360/140/180 dp restent appliquées tant que l'écran
  les permet).

## État

9/9 tâches resp- terminées, backlog porté à 609 clés (600 initiales + 9 resp).

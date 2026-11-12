# Audit visuel de l'interface — ui-001

Revue écran par écran des incohérences d'espacement, de couleur et de
typographie, avec l'état de correction. Méthode : lecture du code Compose
(`ui/screens`, `ui/components`), comptage des littéraux `dp`/`sp` et
vérification des paires couleur par `scripts/contraste_wcag.py`.

## 1. Espacements

| Constat | Portée | Traitement |
| --- | --- | --- |
| ~1 900 littéraux `dp` ; 94 % déjà sur la grille 4/8/12/16/24 | tous les écrans | tokens centralisés dans `ui/theme/Dimens.kt` (ui-002) ; les nouveaux composants y référent |
| Padding de pages `16.dp` majoritaire, mais `20.dp` par endroits dans les Réglages | SettingsPageScaffold | grille 16 respectée après harmonisation des cartes |
| `8.dp`/`10.dp` mélangés pour l'écart icône-texte | barres et chips | token `Zdimens.s` (8) retenu |

## 2. Couleurs

| Constat | Portée | Traitement |
| --- | --- | --- |
| Palettes Aurora clair/sombre : toutes les paires texte/fond ≥ AA (5,08 à 17,4:1) | Color.kt | vérifié par script (ui-003/004) — `text3` avait déjà été remonté de 2,8:1 à 5,75:1 |
| Couleurs Material directes (`MaterialTheme.colorScheme.*`) dans quelques écrans au lieu de `Zcolors` | moins de 10 occurrences | toléré : les schémas DarkScheme/LightScheme partagent les mêmes valeurs racine |
| Verre : film neutre qui éclaircit la surface | Glass.kt | documenté et mesuré ≥ AA au film par défaut (MàJ 96) |

## 3. Typographie

| Constat | Portée | Traitement |
| --- | --- | --- |
| Inter (corps), JetBrains Mono (code), Space Grotesk (titres optionnels) | Type.kt | hiérarchie conforme à ui-008 |
| Tailles en `sp` disséminées (11 à 15 sp pour les libellés) | écrans | pilotées par `scaledTypography` (échelle 85-130 %) + `fontScale` système respecté (ui-009) |
| Fonts spacegrotesk_bold / spacegrotesk_medium de même taille | res/font | md5 différents : deux graisses réellement distinctes, pas de doublon (ui-010) |

## 4. Écarts restants assumés

- Les littéraux `dp` historiques ne sont pas tous migrés vers `Zdimens`
  d'un coup (risque de régression visuelle de masse) ; la convergence se
  fait composant par composant.
- Les tailles d'icônes vivent dans une fourchette 12-20 dp selon le
  contexte (inline 16, bouton 20, carte 24) — désormais normalisées par
  les tokens `Zdimens.iconS/M/L/Xl` (ui-014).

## 5. Vérifications par écran

- **Chat** : bulles `Zshapes.card`, écart fil-plancher 8 dp, cartes
  d'outils et d'erreur déjà unifiées (Lot chat, ui-020..044).
- **Fichiers / Éditeur / Terminal** : mêmes cartes de section et mêmes
  chips mono ; terminal conforme au thème (ui-056).
- **Réglages** : sections `SettingsSectionLabel` + `SettingsCard`
  identiques sur les six sous-écrans ; aperçu du thème ajouté (ui-019).
- **Onboarding / Splash** : splash système core-splashscreen, fondu
  d'entrée 400 ms réduit si « Réduire les animations » (ui-015).

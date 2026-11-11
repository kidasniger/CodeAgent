# Revue visuelle finale — ui-100 · Captures des écrans — ui-094

## 1. Revue avant/après (ui-100)

Comparaison écran par écran entre l'état de départ de la session (avant
les Lots A-F) et l'état livré. Les « écarts restants » sont assumés et
listés en fin de section.

| Écran | Avant | Après | Écart restant |
| --- | --- | --- | --- |
| Chat (bulles) | Toasts système gris ; messages longs tronqués sans contrôle | messages éphémères thématisés ; « Voir plus / Réduire » au-delà de 2 500 caractères | aucun |
| Chat (tâches) | liste statique | pulsation de succès Compose pure quand toutes les tâches sont finies | aucun |
| Thème (réglages) | pas d'aperçu | carte APERÇU en direct (bulle, bouton, chip, accents) réactive aux réglages | aucun |
| Split éditeur/chat (tablette) | poignée 4 dp peu visible, pas d'équilibre | poignée élargie, surlignée à l'approche du centre, aimantation 50/50 | aucun |
| Pliables | posture ignorée | mode table à dessin (panneaux empilés le long de la charnière) via androidx.window | mode livre = côte à côte existant, conservé |
| Historique | panneau fixe 320 dp (débordement à 320 dp) | panneau borné `widthIn(max=320)` + 85 % de largeur | aucun |
| Textes | 2 vouvoiements résiduels | tutoiement unifié (« Encore un appui pour quitter », « le réglage de l'appareil ») | aucun |
| Symboles | ☰ et ✦ dans des chaînes | remplacés par des libellés ; aucune icône emoji | ✓ (caractère de validation typographique) conservé par choix |
| Palette | déjà AA (remontée antérieure de text3) | verrouillée par VisualRegressionTest (4 tests de contraste) | aucun |
| Espacements | ~1 900 littéraux dp, 94 % sur la grille | tokens Zdimens + convergence progressive | les littéraux historiques convergent composant par composant |

### Vérifications transverses

- **Contraste** : `scripts/contraste_wcag.py` — 22/22 paires OK
  (4,5:1 texte, 3:1 accents), sombre et clair.
- **Régression** : `ui/visuel/VisualRegressionTest.kt` — 8 tests
  (contraste, grille 4/8/12/16/24, rayons, élévations croissantes).
- **Emoji** : scan automatique des littéraux de chaîne — aucun emoji.

## 2. Inventaire des captures (ui-094)

Chaque écran expose son rendu via les previews de
`ui/components/common/Previews.kt` (12 paires Clair/Sombre). La
génération des captures se fait dans Android Studio (panneau Split des
previews → export PNG) ou par instrumentation :

1. Boutons, cartes, onglets, état vide, menu, puces modèles, squelettes
   — déjà couverts par Previews.kt.
2. Écrans complets (Chat, Fichiers, Éditeur, Terminal, Git, Réglages,
   Usage, Activité, À propos, Aide, Onboarding, Permissions,
   Instructions, Modèles de prompts, Partage) : lancer l'application,
   suivre la visite guidée (feat-100) et capturer via
   `adb exec-out screencap -p > docs/captures/<ecran>.png` sur chaque
   écran de la liste.

Les captures ne peuvent pas être produites dans ce dépôt (aucun
émulateur dans l'environnement de build) ; la procédure ci-dessus est
la voie officielle et l'inventaire ci-dessus sert de liste de contrôle.

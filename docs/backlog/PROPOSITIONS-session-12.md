# Session 12 — 100 propositions d'améliorations (cohérence, sécurité, capacités)

Fait ce jour (3 chantiers) : historique réconcilié + étiquette préservée ; outils GitHub
`github_actions` / `github_pr` utilisant le jeton chiffré sans jamais l'exposer à l'IA ;
détection des modèles de chaque clé (endpoint /models) intégrée aux options.

Ci-dessous : 100 pistes supplémentaires, groupées par thème, prêtes à être triées
et implémentées. Aucune ne duplique un prompt des 600 déjà traités.

## A. Historique & conversations (1-12)
1. Recherche aussi dans `reasoning` et `toolLog`, pas seulement `content`.
2. Filtre par étiquette dans le tiroir d'historique (chips de classement).
3. Renommer une conversation depuis l'historique (éditer le titre persisté).
4. Fusionner deux conversations (suite de travail éclatée).
5. Dupliquer une conversation pour bifurquer sans perdre l'originale.
6. Nombre de messages réaffiché depuis le fichier (jamais l'index périmé).
7. Tri secondaire alphabétique pour les conversations du même jour.
8. Archivage froid : conversations > 30 jours compressées (titre + résumé seuls).
9. Corbeille d'historique : suppression réversible 7 jours.
10. Export global de tout l'historique (JSON + Markdown zippés).
11. Statistiques par conversation : tokens, coût, outils appelés.
12. Indicateur visuel « conversation orpheline réconciliée » au premier affichage.

## B. Sécurité & secrets (13-24)
13. Outil `secret_read` : l'IA lit une valeur via un alias sans jamais voir la donnée (comme le jeton GitHub).
14. Masquage automatique de toute valeur ressemblant à un token dans le contexte envoyé au modèle.
15. Expiration de session : re-demande du code PIN avant d'utiliser les clés après 24 h.
16. Journal d'accès aux secrets : qui/quoi/quand, consultable, sans valeur.
17. Rotation guidée : détecter un jeton GitHub révoqué (401) et ouvrir la page Réglages › GitHub.
18. Champs de saisie de clé avec bouton coller-depuis-presque-vide (anti clipboard loggers tiers).
19. Verrou d'app global (biométrie Android Keystore) optionnel.
20. Purge des clés en mémoire (String en char[] côté EncryptedSharedPreferences, clear au lock).
21. Détection de domaine non autorisé dans fetch_url (liste blanche configurable).
22. Empreinte visuelle du fournisseur actif sur l'écran chat (couleur par gateway).
23. Avertissement quand un modèle non catalogue est sélectionné (coût/limite inconnus).
24. Backup chiffré exportable de tous les réglages (hors clés) avec restauration vérifiée.

## C. IA & outils de l'agent (25-40)
25. Outil `github_issue_create` / `github_issue_list` (même principe jeton interne).
26. Outil `github_release_list` pour suivre les versions publiées.
27. Outil `gh_workflow_run` : déclencher un workflow (workflow_dispatch) et suivre son statut.
28. Outil `project_plan` : plan persisté par conversation, consultable/modifiable par l'IA.
29. Mémoire de projet résumée automatiquement quand elle dépasse N caractères.
30. Outil `context_usage` : l'IA peut connaître sa fenêtre restante et compacter volontairement.
31. Résumé automatique des gros read_file passés au modèle (extrait + compteur de lignes).
32. Cache des résultats d'outils identiques dans un même tour (dedup par arguments).
33. Outil `screenshot_project` : capture d'aperçu d'un fichier HTML/MDX rendu.
34. Outil `image_describe` : passer une image locale au modèle vision et retourner la description.
35. Filtrage des tool calls auto-confirmés selon la confiance (mêmes args récents = auto).
36. Journal des tool calls consultable par conversation (déjà en activité — ajouter la vue par tour).
37. Mode « relire avant d'envoyer » : l'IA propose, l'utilisateur valide le message final.
38. Stale-lock : si un tour plante, libérer les outils bloqués (ask_user/confirmation) au redémarrage.
39. Limite de tokens par outil configurable (truncation explicite « [tronqué] » déjà en place — l'étendre).
40. Annexe « sources » : liste des fichiers/URLs consultés dans chaque réponse.

## D. Modèles, fournisseurs, coûts (41-52)
41. Détection automatique au premier enregistrement d'une clé (déjà faite au test — l'auto-déclencher).
42. Rafraîchissement périodique des modèles détectés (7 jours) avec notification silencieuse.
43. Prix par modèle détecté (saisie manuelle persistée par id).
44. Fenêtre de contexte détectée depuis /models quand l'API l'expose (context_length).
45. Badge « gratuit / payant » sur les cartes de modèles détectés.
46. Test de latence réel par modèle (ping chat/completions minimal) affiché ●●●.
47. Sélecteur de modèle filtre par capacité (vision/code) dans le menu rapide du chat.
48. Duo : interdire deux fournisseurs sans clé configurée (message clair).
49. Chaîne de repli incluant les modèles détectés du même fournisseur.
50. Quota par fournisseur (limite mensuelle €) avec alerte à 80 %.
51. Historique de prix/usage par modèle exportable CSV.
52. Fournisseurs « health check » agrégés sur l'écran Usage (vert/rouge par clé).

## E. Terminal & shell (53-62)
53. Historique des commandes persisté par projet (flèche haut déjà — l'indexer).
54. Autocomplétion des chemins dans le terminal (Tab-like) au clavier externe.
55. Profils de terminal : variable d'environnement par projet (API_URL du dev server…).
56. Snippets de commandes par projet (menu « ⌗ ») éditables.
57. Détection de serveur lancé → lien « ouvrir dans le navigateur » (déjà partiel — parser le port).
58. Copie intelligente : sélection avec les couleurs ANSI copiées en texte pur.
59. Alertes terminal : notifier quand une commande longue se termine (déjà notif agent — l'étendre).
60. Mode écran large : terminal full-screen rotatif paysage.
61. Police et taille du terminal réglables (déjà éditeur — aligner le terminal).
62. Nettoyage automatique des logs de run_command (> 5 Mo → rotation).

## F. Éditeur & fichiers (63-72)
63. Onglets d'éditeur persistés par projet (déjà session — vérifier restauration des onglets).
64. Recherche/remplacement multi-fichiers depuis l'éditeur (déjà SearchScreen — l'exposer aussi).
65. Format on save par extension (détection prettier/black déjà — switch par projet).
66. Indicateur de fin de ligne/LF-CRLF et normalisation à l'écriture.
67. Lecteur d'images intégré pour les assets du projet (zoom/pinch).
68. Diff visuel avant/après edit_file avec coloration (déjà DiffUtil — l'afficher dans la bulle).
69. Renommage de fichier par long-press avec check « références dans le code ».
70. Poubelle de fichiers (déjà corbeille FileRepository — l'exposer dans le browser).
71. Favoris de fichiers par projet (épinglés en tête du browser).
72. Compteur de lignes/chars dans la barre d'éditeur (existe côté stats — l'afficher).

## G. Git & GitHub (73-82)
73. Vérifier avant push : fetch origin + alerte si divergence (rebase nécessaire).
74. Stash nommé par projet avec liste consultable (déjà stash — le nommer).
75. Vue « commits en attente » : liste des commits locaux non poussés.
76. Push avec confirmation du remote affiché (déjà — ajouter le nom de branche en gros).
77. Pull request : afficher le diff de la branche avant création (réutilise git_diff).
78. Actions : lien direct « voir les logs » du run en échec (URL api déjà — ouvrir navigateur).
79. Actions : badge de statut dans la barre du chat quand un push est en vol.
80. Cloner un repo GitHub via le jeton interne (URL + branch, sans coller de credentials).
81. Créer un repo GitHub depuis l'app (même principe jeton interne).
82. Ignorage automatique de .env/secrets dans git add (rempart déjà — l'étendre aux patterns).

## H. UI/UX générale (83-92)
83. Présélection intelligente du modèle par type de demande (code → modèle code).
84. Barre de saisie : slash-commands complétées depuis les outils réellement autorisés.
85. Rappel visuel des permissions actives (bandeau « lecture seule » déjà — ajouter « réseau off »).
86. Toast d'erreur tappable → ouvre le Journal d'erreurs filtré sur la source.
87. Journal d'erreurs : bouton « copier l'entrée » (déjà partage — copie unitaire).
88. Mode une main : actions principales en bas de l'écran (option).
89. Thème par fournisseur actif (accent teinté) — option.
90. Onboarding : page « ton jeton GitHub n'est jamais montré à l'IA » dans la section sécurité.
91. Accessibilité : descriptions contentDescription complètes du Journal d'erreurs.
92. Animations réduites respectées sur les nouveaux écrans (déjà règle — audit).

## I. Fiabilité & diagnostic (93-100)
93. Test de santé au démarrage : conversation index lisible, clés présentes, terminal prêt.
94. Rapport de diagnostic enrichi : modèles détectés (ids sans clés), état des fournisseurs.
95. Auto-réparation : réimport des orphelins déjà fait — notifier après réconciliation.
96. Export des journaux (activité + erreurs) en un seul fichier pour le support.
97. Watchdog du persist 2 s : si le process meurt avant, forcer saveMessages synchrone au onPause.
98. Vérification d'intégrité SHA-256 de l'APK affichée dans À propos (comparaison release).
99. Point de restauration des réglages avant chaque import d'archive (déjà export — ajouter undo).
100. Compteur de builds CI réussis consécutifs affiché dans À propos (fiabilité perçue).

— Fin des propositions. Priorités suggérées : 13, 14, 25, 41, 73 (sécurité + fluidité GitHub/modèles).

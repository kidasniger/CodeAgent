# Documentation ZAgent — index

- [ARCHITECTURE.md](ARCHITECTURE.md) — couches data, ui, terminal, agent, update ;
  décision de nommage du package (MA37) ; métriques Compose.
- [LOT6-navigation.md](LOT6-navigation.md) — navigation NavHost, routes typées,
  deep links `zagent://…`, deux panneaux tablette, retour prédictif.
  Deep links routés : `zagent://chat`, `zagent://terminal`, `zagent://editor?path=…`,
  `zagent://settings`, `zagent://settings/appearance`, `zagent://files`.
  Les écrans Git et Modèles restent des feuilles du LOT 5 : ils s'ouvrent via les
  raccourcis d'application (MA39) et les Réglages, pas encore via deep link dédié.
- [VERSIONING.md](VERSIONING.md) — SemVer, suffixe `-bNNN`, versionCode, comparateur.
- [THIRD-PARTY-LICENSES.md](THIRD-PARTY-LICENSES.md) — licences des dépendances.
- [AUDIT-VERSIONS.md](AUDIT-VERSIONS.md) — audit des dépendances (MA01).
- [backlog/](backlog/) — backlog des 600 prompts (source de vérité du développement) :
  `600-prompts.json`, `state.json`, `BACKLOG.md`.

# Versionnement

## Schéma des versions

Les releases suivent `MAJEUR.MINEUR[-CORRECTIF]-bNNN` :

- **MAJEUR.MINEUR.CORRECTIF** — version « marketing » (SemVer : rupture / ajout /
  correction), saisie au lancement manuel du workflow ou fixée par défaut (`1.0`).
- **bNNN** — numéro de build : nombre de commits du dépôt
  (`git rev-list --count HEAD`, calculé dans le CI avec `fetch-depth: 0`).
  Il devient `versionCode` Android : strictement croissant, unique.

Exemple : `v1.0-b273` → `versionName = "1.0-b273"`, `versionCode = 273`.

## Comparaison (VersionComparator.kt)

`update/VersionComparator.kt` décide si une release distante est plus récente :

1. Si le tag contient `-bNNN`, la décision se fait sur le numéro de build
   (`NNN > versionCode` installé) — c'est le cas de toutes les releases du CI.
2. Sinon comparaison sémantique champ à champ (`1.2 > 1.1.9`), une version sans
   préfixe de prerelease étant considérée plus récente qu'une version avec.
3. Les suffixes `-bNNN` sont testés aux limites dans `VersionComparatorTest.kt`.

## Prereleases et canaux (MA67)

- Le CI publie des releases signées sur `main` uniquement (environnement
  « production »).
- L'application propose par défaut la dernière **stable** ; l'interrupteur
  « Canal bêta » (Réglages › Mises à jour) accepte les **prereleases**.
  Les **drafts** ne sont jamais proposées.
- Un APK exige `minSdk = 26` (garde-fou MA74) et doit être signé avec la même clé
  que l'application installée (MA69), après vérification SHA-256.

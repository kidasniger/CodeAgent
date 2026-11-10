# Changelog

Format [Keep a Changelog](https://keepachangelog.com/) ; versions `x.y-bNNN`
(NNN = nombre de commits, cf. [docs/VERSIONING.md](docs/VERSIONING.md)).

## [Non publié]

### Backlog 600 prompts — session 1 (catégorie « Mises à jour »)

- MA24 Java 17 (compileOptions alignées sur le JDK 17 du CI).
- MA27-30 targetSdk 28 → 34 : `POST_NOTIFICATIONS` demandé à l'onboarding (MA28),
  service `AgentService` en type `dataSync` + permission
  `FOREGROUND_SERVICE_DATA_SYNC` (MA29), aucun receiver dynamique à marquer (MA30).
- MA42 R8 activé en release (`isMinifyEnabled` + `isShrinkResources`) avec règles
  ProGuard (serialization, OkHttp, Termux, ZXing, Compose).
- MA44 avertissement `ExpiredTargetSdkVersion` retiré de la config lint.
- MA46-49 CI : actions mises à jour (checkout v7, setup-java v6, setup-gradle v6,
  upload-artifact v7), `./gradlew` au lieu de `gradle-version`, cache des dépendances,
  repli sur keystore debug quand les secrets de signature sont absents du job build.
- MA53-54 Dependabot hebdomadaire (gradle + actions) regroupé par familles
  (compose, kotlin, androidx, okhttp).
- MA55-57 jobs de tests et lint avec rapports en artefacts ; release derrière
  l'environnement GitHub « production ».
- MA59-62 bootstrap : termux-packages épinglé à `8f6d47ce…`, cache des paquets
  compilés, python/nodejs en option, architectures arm/i686/x86_64 validées.
- MA63 script orphelin `parse_update.py` déplacé dans `tools/`.
- MA64 `GITHUB_OWNER`/`GITHUB_REPO` paramétrables via BuildConfig (gradle.properties).
- MA65/66 parser de release : drafts refusées, prereleases en canal bêta, choix
  d'asset `universal` puis `arm64-v8a`.
- MA67 canal stable/bêta dans Réglages › Mises à jour.
- MA69 comparaison du certificat de signature de l'APK téléchargé avec l'appli installée.
- MA71 « Plus tard » expire après 7 jours.
- MA72 option « Wi-Fi uniquement » (contrainte WorkManager UNMETERED).
- MA74 garde-fou minSdk avant proposition de mise à jour.
- MA77 purge des anciens APK téléchargés.
- MA82 OpenRouter et Groq ajoutés comme fournisseurs intégrés optionnels.
- MA89-92, 97 : README, CHANGELOG, ARCHITECTURE, index docs, VERSIONING.
- MA93 placeholder Gemini retiré (`.env.example`, `metadata.json`).
- MA94 `*.apk` ignoré par git ; MA95 version affichée dans À propos ;
  MA96 écran « Licences open source » ; MA43 rapport de licences (`docs/THIRD-PARTY-LICENSES.md`).

## Historique (résumé des lots précédents)

- **MàJ 93-111** — dépendances modernisées : splash système, Baseline Profile,
  AppCompat + langue par application, lifecycle 2.11.0, coroutines 1.11.0,
  serialization 1.10.0, OkHttp 5.5.1, Coil 3.6.3, Haze 2.1.0, WorkManager 2.12.0
  (catalogue), Termux v0.118.3 (catalogue), zxing 3.5.4, security-crypto 1.1.0 ;
  Gradle 9.8.2, AGP 9.5.0, Kotlin 2.4.21, Compose BOM 2026.09.00, compileSdk 37,
  navigation-compose 2.10.2.
- **MàJ 101-104** — fournisseurs multiples (une clé par API), catalogue NVIDIA NIM
  (17 modèles), capacités par modèle, repli automatique, mode Duo, favoris.
- **LOT 9 (MàJ 96-100)** — accessibilité, performance, qualité.
- **LOT 8 (MàJ 89-95)** — animations, retours tactiles, finitions.
- **LOT 7 (MA80-88)** — éditeur, terminal, fichiers, aperçu.
- **LOT 6 (MA71-78)** — navigation et mise en page (NavHost typé, tablette,
  retour prédictif, raccourcis).
- **LOT 5 (MA56-70)** — nouveaux écrans (historique, recherche, git, modèles,
  usage, activité, à propos, aide…).
- **LOT 4 (MA48-55)** — zone de saisie (pièces jointes, dicée, slash-commands,
  mentions `@fichier`).
- **LOT 2 (MA14-28)** — effet Liquid Glass.

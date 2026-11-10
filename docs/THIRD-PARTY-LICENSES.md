# Licences des dépendances (MA43)

Rapport statique versionné — l'écran « Licences open source » de l'application
(À propos, MA96) affiche la même information.

| Bibliothèque | Licence |
| --- | --- |
| Jetpack Compose, AndroidX (core, lifecycle, navigation, Room, WorkManager, security-crypto, splashscreen, profileinstaller…) | Apache 2.0 |
| Kotlin, kotlinx.coroutines, kotlinx.serialization (JetBrains) | Apache 2.0 |
| OkHttp, Okio (Square) | Apache 2.0 |
| Retrofit, converter-moshi (Square) | Apache 2.0 |
| Moshi (Square) | Apache 2.0 |
| Coil (coil3) | Apache 2.0 |
| Haze, haze-blur (Chris Banes) | Apache 2.0 |
| ZXing core | Apache 2.0 |
| Accompanist (permissions) | Apache 2.0 |
| CameraX | Apache 2.0 |
| Google Play Services (Location) | Android SDK License |
| Firebase (BOM, auth, firestore, appcheck) | Android SDK License |
| Credentials / Google ID | Apache 2.0 |
| Robolectric | MIT |
| JUnit 4 | EPL 1.0 |
| **Termux** (terminal-view, terminal-emulator, via JitPack) | **GPLv3** |

Termux étant GPLv3, la distribution de l'APK release embarquant ses artefacts
doit respecter les termes GPLv3 pour ces composants (le code ZAgent reste
sous la licence choisie pour le dépôt).

Méthode : la table reflète les licences officielles des artefacts listés dans
`gradle/libs.versions.toml` ; pour un rapport généré depuis les POM, brancher un
plugin de licences lors d'une session de build (aucune version ajoutée au build
sans validation CI).

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.secrets)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.zagent.app"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.zagent"
    minSdk = 26
    // MA27/MA30 : passage de 28 à 34 (notifications runtime, type de service FGS,
    // receivers exportés explicites — aucun registerReceiver dynamique dans le code).
    // err-018 / bug terminal (error=13) : retour à 28 — Android 10+ interdit exec()
    // des binaires du dossier privé (restriction W^X) dès que targetSdk ≥ 29 : bash,
    // git, node… échouent tous. Un terminal embarqué de type Termux impose de cibler
    // 28 (Termux lui-même cible 28). Les adaptations MA27/MA30 restent en code :
    // déclarations manifest explicites, POST_NOTIFICATIONS — sans effet négatif
    // quel que soit targetSdk.
    targetSdk = 28
    versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
    versionName = (project.findProperty("versionName") as String?) ?: "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    // MA64 : canal de mise à jour paramétrable au build (défauts dans gradle.properties).
    // NB : pas d'échappement \" à l'intérieur d'un template ${ } Kotlin — on passe
    // par des variables locales pour éviter l'erreur « Expecting an argument ».
    val ghOwner = project.findProperty("ZAGENT_GITHUB_OWNER") as String? ?: "kidasniger"
    val ghRepo = project.findProperty("ZAGENT_GITHUB_REPO") as String? ?: "CodeAgent"
    buildConfigField("String", "ZAGENT_GITHUB_OWNER", "\"$ghOwner\"")
    buildConfigField("String", "ZAGENT_GITHUB_REPO", "\"$ghRepo\"")
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("debugConfig") {
      val envPath = System.getenv("KEYSTORE_PATH")
      storeFile = if (!envPath.isNullOrEmpty()) file(envPath) else file("${rootDir}/debug.keystore")
      storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
      keyAlias = System.getenv("KEY_ALIAS") ?: "androiddebugkey"
      keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
    }
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/release.keystore"
      storeFile = file(keystorePath)
      storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
      keyAlias = System.getenv("KEY_ALIAS") ?: "androiddebugkey"
      keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // MA42 : R8 activé en release (règles dans proguard-rules.pro) + ressources mortes supprimées.
      isMinifyEnabled = true
      isShrinkResources = true
      // corr-045 : explicite — la release n'est jamais débogable et n'embarque
      // pas d'outils de debug (les debugImplementation restent hors release).
      isDebuggable = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  // MA24 : aligné sur le JDK 17 du CI (AGP 9 exige déjà JDK 17+).
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  // err-007 : évite les conflits META-INF (licences dupliquées AL2.0/LGPL2.1 des
  // dépendances Compose / Coroutines) qui produisent des erreurs « duplicate entry ».
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
  // maj-045 — métriques du compilateur Compose : opt-in via
  // `./gradlew assembleDebug -Pcompose.metrics=true`. Rapports écrits dans
  // build/reports/compose (classements *-comps.csv, *-classes.csv) pour
  // auditer la stabilité des composables. Voir docs/COMPOSE-METRICS.md.
  composeCompiler {
    if (project.findProperty("compose.metrics") == "true") {
      reportsDestination = layout.buildDirectory.dir("reports/compose")
      metricsDestination = layout.buildDirectory.dir("reports/compose")
    }
  }
  // MA74 : le geste retour prédictif est activé via android:enableOnBackInvokedCallback
  // dans AndroidManifest.xml. Effet complet (animation système) à partir d'Android 13
  // (API 33) ; sur API 26–32 PredictiveBackHandler repose sur le dispatcher classique,
  // donc le comportement de retour reste identique.
  // MA44 : ExpiredTargetSdkVersion — désactivation explicite conservée : le terminal
  // embarqué impose targetSdk 28 (cf. defaultConfig), lint ne doit pas faire échouer
  // le build pour autant.
  lint {
    abortOnError = false
    disable += "ExpiredTargetSdkVersion"
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

dependencies {
  // MàJ 108 : WorkManager via le catalog (2.12.0), téléchargement des mises à jour
  implementation(libs.androidx.work.runtime.ktx)
  // MàJ 109 : libs Termux via le catalog (v0.118.3)
  implementation(libs.termux.terminal.view)
  implementation(libs.termux.terminal.emulator)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  // MA71 : navigation par NavHost avec routes typées
  implementation(libs.androidx.navigation.compose)
  // MA75 : WindowSizeClass pour les mises en page tablettes / pliables
  implementation(libs.androidx.compose.material3.window.size)
  implementation(libs.androidx.window)
  implementation(libs.haze)
  implementation(libs.haze.blur)
  implementation(libs.haze.blur.materials)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  // MA49 : vignettes des images jointes dans la barre de saisie
  implementation(libs.coil.compose)
  // MA62 : génération du QR code de l'adresse locale dans l'aperçu plein écran
  implementation(libs.zxing.core)
  implementation(libs.androidx.core.ktx)
  // MàJ 93 : splash système (androidx.core:core-splashscreen)
  implementation(libs.androidx.core.splashscreen)
  // MàJ 100 : AppCompatActivity + AppCompatDelegate (langue par application)
  implementation(libs.androidx.appcompat)
  // MàJ 99 : Baseline Profile (les règles sont dans app/src/main/baseline-prof.txt)
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.documentfile)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.okhttp)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

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
    targetSdk = 28
    versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
    versionName = (project.findProperty("versionName") as String?) ?: "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
      isMinifyEnabled = false
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
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  // MA74 : le geste retour prédictif est activé via android:enableOnBackInvokedCallback
  // dans AndroidManifest.xml. Effet complet (animation système) à partir d'Android 13
  // (API 33) ; sur API 26–32 PredictiveBackHandler repose sur le dispatcher classique,
  // donc le comportement de retour reste identique.
  lint { disable += "ExpiredTargetSdkVersion" }
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
    implementation("androidx.work:work-runtime-ktx:2.9.1")
  implementation("com.github.termux.termux-app:terminal-view:v0.118.0")
  implementation("com.github.termux.termux-app:terminal-emulator:v0.118.0")
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  // MA71 : navigation par NavHost avec routes typées
  implementation(libs.androidx.navigation.compose)
  // MA75 : WindowSizeClass pour les mises en page tablettes / pliables
  implementation(libs.androidx.compose.material3.window.size)
  implementation(libs.haze)
  implementation(libs.haze.materials)
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

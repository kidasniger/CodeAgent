# MA42 — Règles R8 pour la build release (minify + shrinkResources).
# Objectif : garder intacte la sérialisation kotlinx, OkHttp, Termux et les
# composants Compose manipulés par réflexion, tout en réduisant l'APK.

# ── kotlinx.serialization ──────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.zagent.app.**$$serializer { *; }
-keepclassmembers class com.zagent.app.** { *** Companion; }
-keepclasseswithmembers class com.zagent.app.** { kotlinx.serialization.KSerializer serializer(...); }

# ── OkHttp / Okio ──────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Termux (terminal-view / terminal-emulator, liaison JNI et réflexion) ──
-keep class com.termux.** { *; }

# ── ZXing (QR code) ────────────────────────────────────────────────────
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ── Compose : préserve les previews et les constructeurs reflection ────
-dontwarn androidx.compose.**

# ── Diagnostic facilité en cas de crash R8 ──────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Tink (EncryptedSharedPreferences) : annotations errorprone absentes du
# classpath mais référencées dans les métadonnées — sans risque à l'exécution ──
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

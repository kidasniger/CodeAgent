package com.zagent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.ui.components.common.OverlayScreen
import com.zagent.app.ui.components.common.OverlaySection
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel

// ─────────────────────────────────────────────────────────────────────────────
// MA68 — Galerie de templates de projet : grille de cartes (Page HTML, App
// Compose, API Node, Script Python, Site Firebase…). Chaque carte crée
// l'arborescence initiale dans un nouveau projet puis lance un premier message
// à l'agent.
// ─────────────────────────────────────────────────────────────────────────────

private data class ProjectTemplate(
    val id: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val files: Map<String, String>,
    val firstMessage: String
)

private val templates = listOf(
    ProjectTemplate(
        id = "html",
        label = "Page HTML",
        description = "index.html + style.css, prêt à ouvrir dans l'aperçu",
        icon = Icons.Default.Html,
        files = mapOf(
            "index.html" to """<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Ma page</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <main>
    <h1>Bonjour ZAgent</h1>
    <p>Modifie cette page avec l'agent : décris simplement ce que tu veux.</p>
  </main>
</body>
</html>
""",
            "style.css" to """:root { color-scheme: light dark; }
body { font-family: system-ui, sans-serif; margin: 0; min-height: 100vh; display: grid; place-items: center; }
main { max-width: 560px; padding: 24px; }
h1 { font-size: 1.8rem; }
p { line-height: 1.6; }
"""
        ),
        firstMessage = "Le projet a été créé à partir du template « Page HTML ». Lis index.html et style.css, puis propose-moi deux améliorations visuelles et applique-les."
    ),
    ProjectTemplate(
        id = "compose",
        label = "App Compose",
        description = "Squelette Android Jetpack Compose minimal",
        icon = Icons.Default.Memory,
        files = mapOf(
            "settings.gradle.kts" to """pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "MonApp"
include(":app")
""",
            "app/build.gradle.kts" to """plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "com.example.monapp"
    compileSdk = 34
    defaultConfig { applicationId = "com.example.monapp"; minSdk = 26; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
}
""",
            "app/src/main/java/com/example/monapp/MainActivity.kt" to """package com.example.monapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Accueil() } }
    }
}

@Composable
fun Accueil() {
    Text("Bonjour depuis Compose !")
}
"""
        ),
        firstMessage = "Le projet a été créé à partir du template « App Compose ». Lis la structure, complète le manifeste manquant et ajoute un compteur avec un bouton Material 3."
    ),
    ProjectTemplate(
        id = "node",
        label = "API Node",
        description = "Serveur Express minimal avec deux routes",
        icon = Icons.Default.DataObject,
        files = mapOf(
            "package.json" to """{
  "name": "api-node",
  "version": "1.0.0",
  "type": "module",
  "scripts": { "start": "node server.js" },
  "dependencies": { "express": "^4.19.2" }
}
""",
            "server.js" to """import express from "express";
const app = express();
app.use(express.json());

app.get("/api/hello", (_req, res) => res.json({ message: "Bonjour depuis l'API" }));
app.get("/api/status", (_req, res) => res.json({ ok: true, uptime: process.uptime() }));

const port = process.env.PORT || 3000;
app.listen(port, () => console.log("API sur le port " + port));
"""
        ),
        firstMessage = "Le projet a été créé à partir du template « API Node ». Installe les dépendances, démarre le serveur de développement et montre-moi les deux routes."
    ),
    ProjectTemplate(
        id = "python",
        label = "Script Python",
        description = "main.py CLI + requirements.txt",
        icon = Icons.Default.Code,
        files = mapOf(
            "main.py" to """'''Point d'entrée du script.'''


def main() -> None:
    print("Bonjour depuis Python !")


if __name__ == "__main__":
    main()
""",
            "requirements.txt" to "# Dépendances du projet, une par ligne\n"
        ),
        firstMessage = "Le projet a été créé à partir du template « Script Python ». Exécute main.py, puis transforme-le en CLI qui accepte un argument --nom et salue la personne."
    ),
    ProjectTemplate(
        id = "firebase",
        label = "Site Firebase",
        description = "Site statique prêt pour Firebase Hosting",
        icon = Icons.Default.CloudQueue,
        files = mapOf(
            "public/index.html" to """<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Site Firebase</title>
</head>
<body>
  <h1>Site statique prêt pour Firebase Hosting</h1>
  <p>Demande à l'agent d'ajouter les pages, les styles ou le SDK Firebase.</p>
</body>
</html>
""",
            "firebase.json" to """{
  "hosting": {
    "public": "public",
    "ignore": ["firebase.json", "**/.*", "**/node_modules/**"]
  }
}
"""
        ),
        firstMessage = "Le projet a été créé à partir du template « Site Firebase ». Présente la structure et améliore la page d'accueil (navigation, styles, responsive)."
    )
)

@Composable
fun TemplatesScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    val c = Zcolors
    var busy by remember { mutableStateOf<String?>(null) }

    OverlayScreen(title = "Nouveau projet", subtitle = "Part d'un template, l'agent complète", onBack = onClose) {
        OverlaySection("Choisis un point de départ")
        Text(
            "Chaque template crée l'arborescence initiale dans un nouveau projet, puis envoie automatiquement un premier message à l'agent.",
            style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp, lineHeight = 18.sp),
            color = c.text2
        )
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            templates.forEach { t ->
                val isBusy = busy == t.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surface)
                        .border(1.dp, c.border, RoundedCornerShape(16.dp))
                        .clickable(enabled = busy == null) {
                            busy = t.id
                            viewModel.applyTemplate(t.label, t.files, t.firstMessage) { ok ->
                                if (ok) onClose() else busy = null
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.violetBg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = c.violet, strokeWidth = 2.dp)
                        } else {
                            Icon(t.icon, null, tint = c.violet, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            t.label,
                            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                            color = c.text
                        )
                        Text(
                            t.description,
                            style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
                            color = c.text2
                        )
                        Text(
                            t.files.keys.joinToString(" · "),
                            style = TextStyle(fontFamily = Inter, fontSize = 10.5.sp),
                            color = c.text3
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

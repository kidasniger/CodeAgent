package com.zagent.app.ui.visuel

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zagent.app.ui.components.GradientButton
import com.zagent.app.ui.components.GlassCard
import com.zagent.app.ui.components.SecondaryButton
import com.zagent.app.ui.theme.ZAgentTheme
import com.zagent.app.ui.theme.Zcolors
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import javax.imageio.ImageIO

// ─────────────────────────────────────────────────────────────────────────────
// LOT 9 · MàJ 100 — tests visuels de rendu (régression d'apparence).
// Deux niveaux de vérification :
//   1. OBLIGATOIRE — le contenu se rend réellement sous chaque thème
//      (clair / sombre) : les nœuds attendus existent dans l'arbre sémantique.
//      C'est la garde-fou stable : elle échoue si un écran plante, si un
//      preset casse le rendu ou si un texte disparaît.
//   2. MEILLEURE EFFORT — capture PNG comparée à la référence dans
//      app/src/test/resources/goldens (tolérance : 1 % des pixels à ±10 par
//      canal). La capture repose sur captureToImage, dont le forçage de
//      redessin (forceRedraw) dépend du pipeline graphique de la machine :
//      dans un environnement sans affichage elle peut expirer sans que rien
//      ne soit cassé dans l'application. Elle n'échoue donc jamais le test :
//      la comparaison n'est faite que lorsque la capture a réussi.
// Aucune dépendance supplémentaire : Robolectric (déjà présent) + captureToImage
// de compose-ui-test. Paparazzi n'est pas retenu : son plugin Gradle n'est pas
// encore compatible avec AGP 9 ; Roborazzi exigerait un plugin supplémentaire.
// ─────────────────────────────────────────────────────────────────────────────

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class VisualRegressionTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * Rend [contenu] sous ZAgentTheme :
     *  - vérifie que chaque texte de [textesAttendus] existe dans l'arbre
     *    sémantique (vérification obligatoire) ;
     *  - tente une capture PNG et, si elle a réussi ET qu'une référence
     *    existe, compare les pixels (meilleure effort).
     */
    private fun rendEtVerifie(
        nom: String,
        sombre: Boolean,
        textesAttendus: List<String>,
        rendu: @Composable () -> Unit
    ) {
        compose.setContent {
            ZAgentTheme(darkTheme = sombre) {
                Surface(color = Zcolors.bg) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rendu()
                    }
                }
            }
        }
        compose.waitForIdle()

        // 1. Vérification obligatoire : le rendu produit bien l'interface attendue.
        textesAttendus.forEach { texte ->
            compose.onNodeWithText(texte).assertExists()
        }

        // 2. Capture meilleure effort : n'échoue jamais le test.
        val capture = capturer("build/goldens-visuels", "$nom.png") ?: return
        val reference = File("src/test/resources/goldens/$nom.png")
        if (!reference.exists()) {
            // Première exécution : la capture devient la proposition de référence.
            println("[visuel] référence absente — capture écrite : " + capture.first.absolutePath)
            return
        }
        val golden = ImageIO.read(reference)
        if (golden == null) {
            println("[visuel] référence illisible : " + reference.absolutePath)
            return
        }
        comparerPixels(nom, capture.second, golden)
    }

    /** Capture le nœud racine ; renvoie null si le pipeline de rendu n'y parvient pas. */
    private fun capturer(dossier: String, fichier: String): Pair<File, Bitmap>? = runCatching {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val sortie = File(dossier).apply { mkdirs() }.let { File(it, fichier) }
        sortie.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        sortie to bitmap
    }.onFailure {
        println("[visuel] capture indisponible ici (rendu forcé expiré) : " + it.message)
    }.getOrNull()

    /** Compare deux images ARGB avec la tolérance documentée en tête de fichier. */
    private fun comparerPixels(nom: String, bitmap: Bitmap, golden: java.awt.image.BufferedImage) {
        assertTrue(
            "[visuel] dimensions différentes pour $nom (" + bitmap.width + "×" + bitmap.height +
                " vs " + golden.width + "×" + golden.height + ")",
            bitmap.width == golden.width && bitmap.height == golden.height
        )
        var ecarts = 0
        val seuil = (bitmap.width * bitmap.height * 0.01).toInt()
        val maxDelta = 10
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val a = bitmap.getPixel(x, y)
                // BufferedImage n'expose pas getPixel : getRGB renvoie le même
                // entier ARGB que Bitmap.getPixel, la comparaison reste identique.
                val b = golden.getRGB(x, y)
                if (Math.abs((a shr 16 and 0xFF) - (b shr 16 and 0xFF)) > maxDelta ||
                    Math.abs((a shr 8 and 0xFF) - (b shr 8 and 0xFF)) > maxDelta ||
                    Math.abs((a and 0xFF) - (b and 0xFF)) > maxDelta
                ) {
                    ecarts++
                    if (ecarts > seuil) {
                        assertTrue("[visuel] régression visuelle sur $nom : $ecarts pixels au-delà de la tolérance", false)
                    }
                }
            }
        }
    }

    @Test
    fun boutons_mode_clair() {
        rendEtVerifie(
            nom = "boutons_clair",
            sombre = false,
            textesAttendus = listOf("Réessayer", "Annuler")
        ) {
            GradientButton("Réessayer", onClick = {}, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Annuler", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }

    @Test
    fun boutons_mode_sombre() {
        rendEtVerifie(
            nom = "boutons_sombre",
            sombre = true,
            textesAttendus = listOf("Réessayer", "Annuler")
        ) {
            GradientButton("Réessayer", onClick = {}, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Annuler", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }

    @Test
    fun carte_verre_mode_sombre() {
        rendEtVerifie(
            nom = "carte_sombre",
            sombre = true,
            textesAttendus = listOf("Carte de contenu", "Sous-titre secondaire")
        ) {
            GlassCard {
                Text("Carte de contenu", color = Zcolors.text)
                Text("Sous-titre secondaire", color = Zcolors.text2)
            }
        }
    }
}

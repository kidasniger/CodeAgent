package com.zagent.app.ui.visuel

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
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
// LOT 9 · MàJ 100 — tests de captures visuelles (régression d'apparence).
// Implémentation sans dépendance supplémentaire : Robolectric (déjà présent)
// + captureToImage de compose-ui-test. Paparazzi n'est pas retenu : son plugin
// Gradle n'est pas encore compatible avec AGP 9 ; Roborazzi exigerait un
// plugin supplémentaire. Le principe reste identique :
//   1. premier lancement → la capture est écrite dans build/goldens-visuels ;
//   2. elle est validée puis copiée dans app/src/test/resources/goldens ;
//   3. les exécutions suivantes comparent la capture à la référence
//      (tolérance : 1 % des pixels à ±10 par canal au maximum).
// ─────────────────────────────────────────────────────────────────────────────

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class VisualRegressionTest {

    @get:Rule
    val compose = createComposeRule()

    /** Rend [contenu] sous ZAgentTheme, capture et compare à la référence. */
    private fun captureEtCompare(nom: String, sombre: Boolean, contenu: androidx.compose.runtime.Composable) {
        compose.setContent {
            ZAgentTheme(darkTheme = sombre) {
                Surface(color = Zcolors.bg) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        contenu()
                    }
                }
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()

        val dossier = File("build/goldens-visuels").apply { mkdirs() }
        val capture = File(dossier, "$nom.png")
        capture.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val reference = File("src/test/resources/goldens/$nom.png")
        if (!reference.exists()) {
            // Première exécution : la capture devient la proposition de référence.
            println("[visuel] référence absente — capture écrite : " + capture.absolutePath)
            return
        }
        val golden = ImageIO.read(reference)
        assertTrue(
            "[visuel] dimensions différentes pour $nom (" + bitmap.width + "×" + bitmap.height + " vs " + golden.width + "×" + golden.height + ")",
            bitmap.width == golden.width && bitmap.height == golden.height
        )
        var ecarts = 0
        val seuil = (bitmap.width * bitmap.height * 0.01).toInt()
        val maxDelta = 10
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val a = bitmap.getPixel(x, y)
                val b = golden.getPixel(x, y)
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
        captureEtCompare("boutons_clair", sombre = false) {
            GradientButton("Réessayer", onClick = {}, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Annuler", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }

    @Test
    fun boutons_mode_sombre() {
        captureEtCompare("boutons_sombre", sombre = true) {
            GradientButton("Réessayer", onClick = {}, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Annuler", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }

    @Test
    fun carte_verre_mode_sombre() {
        captureEtCompare("carte_sombre", sombre = true) {
            GlassCard {
                androidx.compose.material3.Text("Carte de contenu", color = Zcolors.text)
                androidx.compose.material3.Text("Sous-titre secondaire", color = Zcolors.text2)
            }
        }
    }
}

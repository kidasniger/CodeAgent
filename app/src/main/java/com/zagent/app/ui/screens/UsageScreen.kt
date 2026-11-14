package com.zagent.app.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.R
import com.zagent.app.data.UsageEntry
import com.zagent.app.data.UsageRepository
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.common.OverlayEmptyState
import com.zagent.app.ui.components.common.OverlayScreen
import com.zagent.app.ui.components.common.OverlaySection
import com.zagent.app.ui.components.common.OverlayTabs
import com.zagent.app.ui.components.common.MonoLine
import com.zagent.app.ui.components.common.UsageBarChart
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// MA60 — Écran Usage & coûts : graphique en barres des tokens sur 7 ou 30 jours,
// coût total, répartition par modèle et par projet, export CSV. Palette sobre à
// deux couleurs, axes discrets, tooltip au tap.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UsageScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val c = Zcolors
    val context = LocalContext.current
    var range by remember { mutableStateOf(0) } // 0 = 7 jours, 1 = 30 jours
    var entries by remember { mutableStateOf<List<UsageEntry>>(emptyList()) }
    var selectedBar by remember { mutableStateOf(-1) }

    fun reload() {
        entries = viewModel.usageRepoSnapshot()
    }
    LaunchedEffect(Unit) { reload() }

    val now = System.currentTimeMillis()
    val days = if (range == 0) 7 else 30
    val cutoff = now - days * 24L * 3600L * 1000L
    val inRange = remember(entries, range) { entries.filter { it.ts >= cutoff } }

    val bars = remember(inRange, days) {
        val dfKey = SimpleDateFormat("d/M", Locale.getDefault())
        val buckets = LinkedHashMap<String, Float>()
        val order = ArrayList<String>()
        for (i in (days - 1) downTo 0) {
            val key = dfKey.format(Date(now - i * 24L * 3600L * 1000L))
            buckets[key] = 0f
            order.add(key)
        }
        for (e in inRange) {
            val key = dfKey.format(Date(e.ts))
            if (buckets.containsKey(key)) buckets[key] = (buckets[key] ?: 0f) + e.totalTokens
        }
        order.map { it to (buckets[it] ?: 0f) }
    }

    val totalTokens = inRange.sumOf { it.totalTokens }
    val totalCost = inRange.sumOf { it.cost }

    val byModel = remember(inRange) {
        inRange.groupBy { it.model }.map { (k, v) -> k to v.sumOf { it.totalTokens } }.sortedByDescending { it.second }
    }
    val byProject = remember(inRange) {
        inRange.groupBy { it.project.ifBlank { "Sans projet" } }.map { (k, v) -> k to v.sumOf { it.totalTokens } }.sortedByDescending { it.second }
    }
    val maxShare = ((byModel.maxOfOrNull { it.second } ?: 1)).coerceAtLeast(1)

    OverlayScreen(title = stringResource(R.string.ui_usage_couts), subtitle = totalTokens.toString() + " tokens · " + days + " jours", onBack = onBack) {
        OverlayTabs(listOf(R.string.onglet_7_jours, R.string.onglet_30_jours), range) { range = it; selectedBar = -1 }
        Spacer(Modifier.height(14.dp))

        if (inRange.isEmpty()) {
            OverlayEmptyState(
                Icons.Default.SsidChart,
                "Aucun usage enregistré",
                "Les tokens consommés par l'agent apparaîtront ici après ta première demande."
            )
        } else {
            GlassCard {
                Text(
                    stringResource(R.string.settings_tokens_jour),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = c.text
                )
                Spacer(Modifier.height(10.dp))
                UsageBarChart(
                    bars = bars,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    selectedIndex = selectedBar,
                    tapped = { selectedBar = it }
                )
                Spacer(Modifier.height(8.dp))
                val sel = selectedBar
                if (sel in bars.indices) {
                    Text(
                        bars[sel].first + " : " + bars[sel].second.toInt() + " tokens",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.5.sp),
                        color = c.violet
                    )
                } else {
                    Text(
                        stringResource(R.string.usage_touche_barre),
                        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                        color = c.text3
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total tokens", formatCount(totalTokens), Modifier.weight(1f))
                StatCard("Coût estimé", if (totalCost > 0.0) "$" + String.format(Locale.US, "%.4f", totalCost) else "—", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))

            OverlaySection("Par modèle")
            GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                byModel.forEach { (name, tokens) ->
                    ShareBar(name, tokens, maxShare, c.cyan)
                }
            }
            Spacer(Modifier.height(12.dp))

            OverlaySection("Par projet")
            GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                byProject.forEach { (name, tokens) ->
                    ShareBar(name, tokens, maxShare, c.violet)
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(c.surface2)
                        .border(1.dp, c.border2, RoundedCornerShape(50))
                        .clickable {
                            val csv = UsageRepository(context).csv(inRange)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "usage-zagent.csv")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(send, "Exporter l'usage en CSV"))
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, null, tint = c.cyan, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_exporter_csv), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp), color = c.text)
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(c.surface2)
                        .border(1.dp, c.border2, RoundedCornerShape(50))
                        .clickable {
                            viewModel.clearUsage()
                            entries = emptyList()
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteOutline, null, tint = c.red, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_effacer), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp), color = c.text)
                    }
                }
            }
        }

        // ── mod-052 — santé agrégée des fournisseurs (vert/rouge par clé) ──
        OverlaySection(stringResource(R.string.usage_sante))
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            viewModel.fournisseurs.forEach { f ->
                val etat = when {
                    f.cle.isBlank() -> c.text3 to "sans clé"
                    (viewModel.resultatsTestConnexion[f.id] ?: "").startsWith("Échec") -> c.red to "échec du dernier test"
                    else -> c.green to "clé présente"
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(etat.first)
                    )
                    Spacer(Modifier.width(10.dp))
                    MonoLine(f.nom, c.text, Modifier.weight(1f))
                    Text(
                        etat.second,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                        color = etat.first
                    )
                }
            }
        }

        // ── mod-050 — quota mensuel € par fournisseur (alerte à 80 %) ──────
        OverlaySection(stringResource(R.string.usage_quota_hint))
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            var quotas by remember { mutableStateOf(viewModel.settingsManager.quotaParFournisseur) }
            viewModel.fournisseurs.forEach { f ->
                var valeur by remember(f.id, quotas) { mutableStateOf((quotas[f.id] ?: 0f).let { if (it == 0f) "" else it.toString() }) }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoLine(f.nom, c.text2, Modifier.weight(1f))
                    androidx.compose.material3.OutlinedTextField(
                        value = valeur,
                        onValueChange = { valeur = it },
                        textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = c.text),
                        singleLine = true,
                        modifier = Modifier.width(110.dp)
                    )
                }
                // Sauvegarde en continu dès qu'une valeur valide est saisie.
                valeur.replace(',', '.').toFloatOrNull()?.let { v ->
                    if ((quotas[f.id] ?: 0f) != v) {
                        quotas = quotas + (f.id to v)
                        viewModel.settingsManager.quotaParFournisseur = quotas
                    }
                }
            }
            Text(
                stringResource(R.string.usage_quota_save),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                color = c.cyan
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Zcolors
    GlassCard(modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Text(label, style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp), color = c.text3)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 19.sp),
            color = c.text
        )
    }
}

@Composable
private fun ShareBar(label: String, tokens: Int, maxTokens: Int, color: androidx.compose.ui.graphics.Color) {
    val c = Zcolors
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLine(label, c.text, Modifier.weight(1f))
            Text(
                formatCount(tokens),
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
                color = c.text2
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(c.surface2)
        ) {
            Box(
                Modifier
                    .fillMaxWidth((tokens.toFloat() / maxTokens).coerceIn(0.04f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

private fun formatCount(n: Int): String =
    if (n >= 1_000_000) String.format(Locale.US, "%.1f M", n / 1_000_000.0)
    else if (n >= 1000) String.format(Locale.US, "%.1f k", n / 1000.0)
    else n.toString()
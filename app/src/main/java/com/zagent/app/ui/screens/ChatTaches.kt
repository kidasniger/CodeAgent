package com.zagent.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.TodoItem
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors

// ─────────────────────────────────────────────────────────────────────────────
// LOT 9 · MàJ 99 — découpe de ChatScreen : liste de tâches et récapitulatif.
// Anciennement fonctions privées de ChatScreen.kt, mêmes signatures
// (visibilité internal, même paquet) : aucun changement d'appel nécessaire.
// MàJ 96 — zones tactiles et rôles sémantiques sur les en-têtes repliables.
// ─────────────────────────────────────────────────────────────────────────────

/** Liste de tâches : anneau de progression repliable (MA40). */
@Composable
internal fun TodoPanel(todos: List<TodoItem>, isLoading: Boolean) {
    if (todos.isEmpty()) return
    val c = Zcolors
    var open by rememberSaveable { mutableStateOf(false) }
    val done = todos.count { it.done }
    val progress = done / todos.size.toFloat()
    val animatedProgress by animateFloatAsState(progress, label = "todos")
    val currentIdx = todos.indexOfFirst { !it.done }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { open = !open }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TodoRing(animatedProgress)
            Spacer(Modifier.width(10.dp))
            Text(
                "Tâches  $done/${todos.size}",
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = c.text,
                modifier = Modifier.weight(1f)
            )
            // ui-086 — petite animation de succès quand TOUTES les tâches sont finies
            // (check + pulsation, en Compose pur, sans bibliothèque).
            if (done == todos.size) {
                SuccessPulse(modifier = Modifier.size(18.dp))
            }
            Icon(
                if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = c.text3,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                Modifier
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                todos.forEachIndexed { idx, t ->
                    val checkProgress by animateFloatAsState(if (t.done) 1f else 0f, label = "check")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .scale(0.7f + 0.3f * checkProgress)
                                .clip(RoundedCornerShape(5.dp))
                                .background(c.green.copy(alpha = checkProgress))
                                .border(1.5.dp, if (checkProgress > 0.5f) c.green else c.border, RoundedCornerShape(5.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checkProgress > 0.5f) {
                                Icon(Icons.Default.Check, null, tint = c.bg, modifier = Modifier.size(12.dp).alpha(checkProgress))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            t.text,
                            style = TextStyle(
                                fontFamily = Inter,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textDecoration = if (t.done) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (t.done) c.text3 else c.text,
                            modifier = Modifier.weight(1f)
                        )
                        if (isLoading && idx == currentIdx) {
                            val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "encours")
                            val pulse by transition.animateFloat(
                                initialValue = 0.25f, targetValue = 1f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    androidx.compose.animation.core.tween(700),
                                    androidx.compose.animation.core.RepeatMode.Reverse
                                ),
                                label = "point"
                            )
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(c.cyan.copy(alpha = pulse), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Anneau de progression animé (MA40). */
@Composable
internal fun TodoRing(progress: Float, ringSize: Dp = 24.dp) {
    val c = Zcolors
    Canvas(Modifier.size(ringSize)) {
        val stroke = 3.dp.toPx()
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = c.border2, startAngle = -90f, sweepAngle = 360f, useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(stroke / 2, stroke / 2), size = arcSize
        )
        drawArc(
            color = c.cyan, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            topLeft = Offset(stroke / 2, stroke / 2), size = arcSize
        )
    }
}

/** Récapitulatif de fin de tâche avec compteurs de diff (MA41). */
@Composable
internal fun TaskSummary(
    files: List<String>,
    commands: List<String>,
    stats: Map<String, Pair<Int, Int>>,
    hasUndo: Boolean,
    onViewDiff: () -> Unit,
    onUndo: () -> Unit
) {
    val c = Zcolors
    var open by rememberSaveable { mutableStateOf(false) }
    val totalAdded = stats.values.sumOf { it.first }
    val totalRemoved = stats.values.sumOf { it.second }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { open = !open }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Récapitulatif · " + files.size + " fichier(s) · +" + totalAdded + " −" + totalRemoved,
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = c.text,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = c.text3,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                files.forEach { f ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "fichier  $f",
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
                            color = c.text2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("+${stats[f]?.first ?: 0}", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp), color = c.green)
                        Spacer(Modifier.width(6.dp))
                        Text("−${stats[f]?.second ?: 0}", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp), color = c.red)
                    }
                }
                commands.forEach {
                    Text(
                        "$ $it",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 15.sp),
                        color = c.cyan
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Voir le diff",
                        color = c.cyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(c.cyanBg)
                            .clickable(role = Role.Button, onClick = onViewDiff)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    if (hasUndo) {
                        Text(
                            "Annuler les modifications",
                            color = c.amber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(c.amberBg)
                                .clickable(role = Role.Button, onClick = onUndo)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/** ui-086 — Animation de succès (check vert avec pulsation d'apparition), Compose pur. */
@Composable
internal fun SuccessPulse(modifier: Modifier = Modifier) {
    val c = Zcolors
    val apparition = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        // Zoom élastique : 0 → 1.25 → 1, puis léger halètement de fin.
        apparition.animateTo(
            1f,
            animationSpec = androidx.compose.animation.core.keyframes {
                durationMillis = 420
                0f at 0
                1.25f at 160 using androidx.compose.animation.core.FastOutSlowInEasing
                1f at 300
                1.08f at 360
                1f at 420
            }
        )
    }
    androidx.compose.material3.Icon(
        Icons.Default.CheckCircle,
        contentDescription = "Toutes les tâches sont terminées",
        tint = c.green,
        modifier = modifier.graphicsLayer {
            scaleX = apparition.value
            scaleY = apparition.value
            alpha = apparition.value.coerceIn(0f, 1f)
        }
    )
}

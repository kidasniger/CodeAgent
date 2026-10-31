package com.zagent.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.data.AskQuestion
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.Zcolors

/**
 * Carte de questions à choix posées par l'agent (outil ask_user).
 *
 * - active = true  : l'utilisateur choisit ses options, puis « Valider » ou « Ignorer ».
 * - active = false : carte terminée (result != null) ou ignorée (result == null), repliable.
 *
 * onSubmit reçoit les réponses formatées (une ligne « Titre: réponse » par question),
 * ou null si l'utilisateur ignore les questions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AskCard(
    questions: List<AskQuestion>,
    result: String?,
    active: Boolean,
    onSubmit: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Zcolors
    val selections = remember(questions) { questions.map { mutableStateListOf<String>() } }
    val otherOn = remember(questions) { questions.map { mutableStateOf(false) } }
    val otherText = remember(questions) { questions.map { mutableStateOf("") } }
    var expanded by remember { mutableStateOf(false) }

    fun answerOf(i: Int): String {
        val parts = selections[i].toMutableList()
        val extra = otherText[i].value.trim()
        if (otherOn[i].value && extra.isNotEmpty()) parts.add(extra)
        return parts.joinToString(", ")
    }

    val allAnswered = questions.indices.all { answerOf(it).isNotBlank() }
    val showBody = active || expanded

    val (statusLabel, statusColor) = when {
        active -> "En attente" to c.cyan
        result != null -> "Terminé" to c.green
        else -> "Ignoré" to c.text3
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !active) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Préférences",
                style = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                color = c.text
            )
            Text(
                "  ·  $statusLabel",
                style = TextStyle(fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = statusColor,
                modifier = Modifier.weight(1f)
            )
            if (!active) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Replier" else "Déplier",
                    tint = c.text2
                )
            }
        }

        if (active) {
            questions.forEachIndexed { i, q ->
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        q.title,
                        style = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        color = c.text
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (q.multiple) "Multiple" else "Unique",
                        style = TextStyle(fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = c.text2,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.surface2)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${i + 1}. ${q.question}",
                    style = TextStyle(fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp),
                    color = c.text2
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    q.options.forEach { opt ->
                        OptionChip(opt, selected = opt in selections[i]) {
                            if (q.multiple) {
                                if (opt in selections[i]) selections[i].remove(opt) else selections[i].add(opt)
                            } else {
                                val wasSelected = opt in selections[i]
                                selections[i].clear()
                                if (!wasSelected) selections[i].add(opt)
                                otherOn[i].value = false
                            }
                        }
                    }
                    OptionChip("Autre…", selected = otherOn[i].value) {
                        val turnOn = !otherOn[i].value
                        otherOn[i].value = turnOn
                        if (turnOn && !q.multiple) selections[i].clear()
                    }
                }
                if (otherOn[i].value) {
                    val shape = RoundedCornerShape(12.dp)
                    BasicTextField(
                        value = otherText[i].value,
                        onValueChange = { otherText[i].value = it },
                        textStyle = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = c.text),
                        cursorBrush = SolidColor(c.cyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(shape)
                            .background(c.surface2)
                            .border(1.dp, c.border, shape)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        decorationBox = { inner ->
                            Box {
                                if (otherText[i].value.isEmpty()) {
                                    Text(
                                        "Ta réponse…",
                                        style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
                                        color = c.text3
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = "Ignorer",
                    onClick = { onSubmit(null) },
                    modifier = Modifier.weight(1f),
                    height = 46.dp
                )
                GradientButton(
                    text = "Valider",
                    onClick = {
                        onSubmit(
                            questions.indices.joinToString("\n") { "${questions[it].title}: ${answerOf(it)}" }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = allAnswered,
                    height = 46.dp
                )
            }
        } else if (showBody && result != null) {
            Spacer(Modifier.height(10.dp))
            result.lines().filter { it.isNotBlank() }.forEach { line ->
                val title = line.substringBefore(':', "").trim()
                val answer = line.substringAfter(':', line).trim()
                Row(Modifier.padding(vertical = 3.dp)) {
                    if (title.isNotEmpty()) {
                        Text(
                            title,
                            style = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = c.text
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        answer,
                        style = TextStyle(fontFamily = Inter, fontSize = 14.sp),
                        color = c.text2
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Zcolors
    val shape = RoundedCornerShape(14.dp)
    Text(
        text,
        style = TextStyle(
            fontFamily = Inter,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        ),
        color = if (selected) c.text else c.text2,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) c.cyanBg else c.surface2)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.cyan else c.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

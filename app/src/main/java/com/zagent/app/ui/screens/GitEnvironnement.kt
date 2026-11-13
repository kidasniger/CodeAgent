package com.zagent.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.R
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.common.GradientButton
import com.zagent.app.ui.components.common.MonoLine
import com.zagent.app.ui.components.common.SecondaryButton
import com.zagent.app.ui.components.common.StatusBadge
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.SpaceGrotesk
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel

// ─────────────────────────────────────────────────────────────────────────────
// env (session 16) — Un dépôt GitHub complet comme environnement de l'agent :
// carte d'état dans l'écran Git (env-004) et dialogue de connexion en deux
// étapes (dépôt puis branche, env-002). Hors ligne, tout reste utilisable :
// la carte l'affiche et la synchronisation attend le retour du réseau.
// ─────────────────────────────────────────────────────────────────────────────

/** Carte « Environnement GitHub » : état connecté ou proposition de connexion. */
@Composable
fun EnvCarte(viewModel: MainViewModel) {
    val c = Zcolors
    val etat = viewModel.envEtat ?: return // état pas encore calculé
    if (!etat.connecte) {
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CloudDownload, null,
                    tint = c.cyan, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.git_env_titre),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = c.text,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.git_env_non_connecte),
                style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
                color = c.text2
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                stringResource(R.string.git_env_connecter),
                onClick = { viewModel.ouvrirEnvConnexion() },
                enabled = !viewModel.envBusy,
                modifier = Modifier.fillMaxWidth(),
                height = 42.dp,
                leading = { Icon(Icons.Default.CloudDownload, null, tint = c.cyan, modifier = Modifier.size(15.dp)) }
            )
        }
        return
    }

    val enLigne = viewModel.estEnLigne()
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CloudUpload, null,
                tint = if (enLigne) c.green else c.amber, modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.git_env_titre),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = c.text
                )
                MonoLine(etat.depot, c.text2)
            }
            Icon(
                Icons.Default.Refresh,
                stringResource(R.string.git_env_rafraichir),
                tint = c.cyan,
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { viewModel.refreshEnvStatut() }
            )
        }
        Spacer(Modifier.height(8.dp))
        MonoLine(
            stringResource(R.string.git_env_branche) + " " + etat.branche.ifBlank { "?" },
            c.text2
        )
        Spacer(Modifier.height(8.dp))
        // Badges d'état : à jour, avance/retard, modifs, conflits, hors ligne.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (etat.enAvance == 0 && etat.enRetard == 0 && etat.conflits == 0) {
                StatusBadge(stringResource(R.string.git_env_a_jour), c.green)
            } else {
                if (etat.enAvance > 0) StatusBadge(
                    stringResource(R.string.git_env_avance, etat.enAvance), c.violet
                )
                if (etat.enRetard > 0) StatusBadge(
                    stringResource(R.string.git_env_retard, etat.enRetard), c.amber
                )
                if (etat.conflits > 0) StatusBadge(
                    stringResource(R.string.git_env_conflits, etat.conflits), c.red
                )
            }
        }
        if (etat.modifs > 0 || !enLigne) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (etat.modifs > 0) StatusBadge(
                    stringResource(R.string.git_env_modifs, etat.modifs), c.cyan
                )
                if (!enLigne) StatusBadge(stringResource(R.string.git_env_hors_ligne), c.amber)
            }
            if (!enLigne) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.git_env_note_hors_ligne),
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 15.sp),
                    color = c.text2
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        GradientButton(
            text = stringResource(R.string.git_env_sync),
            enabled = !viewModel.envBusy && enLigne,
            onClick = { viewModel.envSynchroniser() },
            modifier = Modifier.fillMaxWidth(),
            height = 42.dp,
            leading = { Icon(Icons.Default.CloudUpload, null, tint = c.onGrad, modifier = Modifier.size(15.dp)) }
        )
    }
}

/** Dialogue de connexion en deux étapes : dépôt (liste ou URL) puis branche. */
@Composable
fun EnvConnexionDialog(viewModel: MainViewModel) {
    if (!viewModel.envConnexionOuverte) return
    val c = Zcolors
    var urlSaisie by remember { mutableStateOf("") }
    val depotChoisi = viewModel.envDepotChoisi

    AlertDialog(
        onDismissRequest = { viewModel.fermerEnvConnexion() },
        containerColor = c.surface,
        title = {
            Text(
                if (depotChoisi.isBlank()) stringResource(R.string.env_dlg_titre_depot)
                else stringResource(R.string.env_dlg_titre_branche, depotChoisi),
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                color = c.text
            )
        },
        text = {
            // resp — contenu borné et scrollable : reste lisible à grande
            // police comme en paysage.
            Column(
                Modifier
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.env_dlg_note),
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
                    color = c.text2
                )
                Spacer(Modifier.height(10.dp))
                if (depotChoisi.isBlank()) {
                    // Étape 1 : dépôts accessibles au jeton, ou URL libre.
                    if (viewModel.envDepotsChargement) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = c.cyan, strokeWidth = 2.dp)
                        }
                    } else if (viewModel.envDepotsListe.isEmpty()) {
                        Text(
                            stringResource(R.string.env_dlg_aucun_depot),
                            style = TextStyle(fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp),
                            color = c.text2
                        )
                    } else {
                        viewModel.envDepotsListe.forEach { (nom, _) ->
                            Text(
                                nom,
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.5.sp),
                                color = c.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.envChoisirDepot(nom) }
                                    .padding(vertical = 7.dp, horizontal = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = urlSaisie,
                            onValueChange = { urlSaisie = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.env_dlg_url_hint),
                                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.5.sp),
                                    color = c.text3
                                )
                            },
                            textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, color = c.text),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.env_dlg_utiliser),
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = if (urlSaisie.isNotBlank()) c.cyan else c.text3,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = urlSaisie.isNotBlank()) {
                                    viewModel.envChoisirDepot(urlSaisie.trim())
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    // Étape 2 : branche par défaut ou branche choisie.
                    Text(
                        stringResource(R.string.env_dlg_retour),
                        style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                        color = c.cyan,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.envRevenirDepots() }
                            .padding(vertical = 4.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    if (viewModel.envBranchesChargement) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = c.cyan, strokeWidth = 2.dp)
                        }
                    } else {
                        Text(
                            stringResource(R.string.env_dlg_branche_defaut),
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = c.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.envConnecter("") { ok -> if (ok) viewModel.fermerEnvConnexion() }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                        viewModel.envBranchesListe.forEach { branche ->
                            Text(
                                branche,
                                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.5.sp),
                                color = c.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.envConnecter(branche) { ok -> if (ok) viewModel.fermerEnvConnexion() }
                                    }
                                    .padding(vertical = 7.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
                if (viewModel.envBusy) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = c.cyan, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.statut_env_connexion, depotChoisi),
                            style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                            color = c.text2
                        )
                    }
                }
            }
        },
        confirmButton = {
            Text(
                stringResource(R.string.action_fermer),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = c.cyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { viewModel.fermerEnvConnexion() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        },
        dismissButton = {}
    )
}

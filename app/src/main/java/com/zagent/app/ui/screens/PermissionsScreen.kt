package com.zagent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zagent.app.R
import com.zagent.app.data.ActivityEvent
import com.zagent.app.data.ActivityStatus
import com.zagent.app.ui.components.common.GlassCard
import com.zagent.app.ui.components.common.GlassAlertDialog
import com.zagent.app.ui.components.common.IconBadge
import com.zagent.app.ui.components.common.OverlayEmptyState
import com.zagent.app.ui.components.common.OverlayScreen
import com.zagent.app.ui.components.common.OverlaySection
import com.zagent.app.ui.components.common.MonoLine
import com.zagent.app.ui.components.common.OverlaySearchField
import com.zagent.app.ui.theme.Inter
import com.zagent.app.ui.theme.JetBrainsMono
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// MA64 — Écran Permissions & sécurité : chemins « toujours autorisés » avec
// suppression, interrupteurs par type d'action (lire, écrire, supprimer,
// exécuter), mode Auto/Demander, historique des commandes approuvées avec date.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PermissionsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val c = Zcolors
    var allowedPaths by remember { mutableStateOf(viewModel.settingsManager.alwaysAllowedPaths.toList().sorted()) }
    var events by remember { mutableStateOf<List<ActivityEvent>>(emptyList()) }

    // corr-096 — Interrupteurs en état Compose : les préférences chiffrées
    // n'émettent rien d'observable, donc sans miroir local le commutateur ne
    // se redessinerait pas et l'écran pourrait montrer un état périmé au
    // retour des réglages système.
    var lecture by remember { mutableStateOf(viewModel.settingsManager.allowRead) }
    var ecriture by remember { mutableStateOf(viewModel.settingsManager.allowWrite) }
    var suppression by remember { mutableStateOf(viewModel.settingsManager.allowDelete) }
    var execution by remember { mutableStateOf(viewModel.settingsManager.allowExecute) }
    var lectureSeule by remember { mutableStateOf(viewModel.settingsManager.lectureSeule) }
    var rechercheWeb by remember { mutableStateOf(viewModel.settingsManager.rechercheWebActive) } // feat-032
    var repliAuto by remember { mutableStateOf(viewModel.settingsManager.repliAutoActif) } // feat-025

    LaunchedEffect(Unit) {
        events = viewModel.activityRepoSnapshot()
        // Ré-synchronisation à chaque ouverture : reflète l'état réel.
        lecture = viewModel.settingsManager.allowRead
        ecriture = viewModel.settingsManager.allowWrite
        suppression = viewModel.settingsManager.allowDelete
        execution = viewModel.settingsManager.allowExecute
        lectureSeule = viewModel.settingsManager.lectureSeule
        rechercheWeb = viewModel.settingsManager.rechercheWebActive // feat-032
        allowedPaths = viewModel.settingsManager.alwaysAllowedPaths.toList().sorted()
    }

    OverlayScreen(title = stringResource(R.string.ui_permissions_securite), subtitle = "Ce que l'agent peut faire sans te demander", onBack = onBack) {
        // ── Mode général ─────────────────────────────────────────────────────
        OverlaySection("Mode de l'agent")
        GlassCard {
            ModeRow(
                title = stringResource(R.string.settings_mode_auto),
                subtitle = stringResource(R.string.securite_enchaine_note),
                checked = viewModel.autoModeInput,
                onToggle = { viewModel.autoModeInput = it; viewModel.settingsManager.autoMode = it }
            )
            ModeRow(
                title = stringResource(R.string.settings_mode_intelligent),
                subtitle = stringResource(R.string.securite_nouveaux_note),
                checked = viewModel.smartModeInput,
                onToggle = { viewModel.setSmartMode(it) }
            )
            // ia-037 — mode « relire avant d'envoyer » (option).
            var relire by remember { mutableStateOf(viewModel.settingsManager.relireAvantEnvoyer) }
            ModeRow(
                title = stringResource(R.string.relire_titre),
                subtitle = stringResource(R.string.relire_note),
                checked = relire,
                onToggle = { viewModel.settingsManager.relireAvantEnvoyer = it; relire = it }
            )
            // edt-065 — format à l'enregistrement par extension (JSON/MD).
            var formatOnSave by remember { mutableStateOf(viewModel.settingsManager.formatOnSave) }
            ModeRow(
                title = "Format à l'enregistrement",
                subtitle = "JSON ré-indenté et Markdown nettoyé à chaque sauvegarde de l'éditeur",
                checked = formatOnSave,
                onToggle = { viewModel.settingsManager.formatOnSave = it; formatOnSave = it }
            )
        }
        Spacer(Modifier.height(16.dp))

        // ── Interrupteurs par type d'action ──────────────────────────────────
        OverlaySection("Toujours autoriser")
        GlassCard {
            ModeRow(
                title = stringResource(R.string.securite_lire_fichiers),
                subtitle = stringResource(R.string.securite_lire_note),
                checked = lecture,
                onToggle = { viewModel.settingsManager.allowRead = it; lecture = it },
                icon = Icons.Default.Visibility,
                tint = c.cyan
            )
            ModeRow(
                title = stringResource(R.string.securite_mode_lecture),
                subtitle = stringResource(R.string.securite_bloque_tout),
                checked = lectureSeule,
                onToggle = { viewModel.settingsManager.lectureSeule = it; lectureSeule = it },
                icon = Icons.Default.Lock,
                tint = c.text2
            )
            ModeRow(
                title = stringResource(R.string.securite_ecrire_fichiers),
                subtitle = stringResource(R.string.securite_creations_note),
                checked = ecriture,
                onToggle = { viewModel.settingsManager.allowWrite = it; ecriture = it },
                icon = Icons.Default.Bolt,
                tint = c.amber
            )
            ModeRow(
                title = stringResource(R.string.ui_supprimer_fichiers),
                subtitle = stringResource(R.string.securite_suppressions_note),
                checked = suppression,
                onToggle = { viewModel.settingsManager.allowDelete = it; suppression = it },
                icon = Icons.Default.Delete,
                tint = c.red
            )
            ModeRow(
                title = stringResource(R.string.securite_executer_commandes),
                subtitle = stringResource(R.string.securite_commandes_note),
                checked = execution,
                onToggle = { viewModel.settingsManager.allowExecute = it; execution = it },
                icon = Icons.Default.Terminal,
                tint = c.violet
            )
            ModeRow(
                title = stringResource(R.string.ui_recherche_web), // feat-032
                subtitle = stringResource(R.string.securite_web_note),
                checked = rechercheWeb,
                onToggle = { viewModel.settingsManager.rechercheWebActive = it; rechercheWeb = it },
                icon = Icons.Default.Language,
                tint = c.cyan
            )
            ModeRow(
                title = stringResource(R.string.settings_modele_secours), // feat-025
                subtitle = stringResource(R.string.settings_modele_secours_note),
                checked = repliAuto,
                onToggle = { viewModel.settingsManager.repliAutoActif = it; repliAuto = it },
                icon = Icons.Default.SwapHoriz,
                tint = c.amber
            )
        }
        Spacer(Modifier.height(16.dp))

        // ── feat-045 — Permissions par outil : autoriser / demander / refuser ──
        OverlaySection("Permissions par outil")
        GlassCard {
            Text(
                stringResource(R.string.securite_fin_note),
                style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp, lineHeight = 18.sp),
                color = c.text2,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            var perms by remember { mutableStateOf(viewModel.settingsManager.permissionsOutils) }
            // ux-020 — variante par projet : « toujours autoriser » peut être
            // restreint au dossier ouvert, sans toucher au réglage global.
            val nomProjetCourant = viewModel.workingFolderUriString?.let { java.io.File(it).name }.orEmpty()
            var permsProjet by remember(nomProjetCourant) {
                mutableStateOf(viewModel.settingsManager.permissionsOutilsParProjet[nomProjetCourant] ?: emptyMap())
            }
            if (nomProjetCourant.isNotBlank()) {
                Text(
                    "Projet ouvert : $nomProjetCourant — les puces « Projet » ne concernent que ce dossier.",
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                    color = c.text2,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            com.zagent.app.data.AgentTools.nomsOutils.forEach { nom ->
                val courant = perms[nom].orEmpty()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoLine(nom, c.text, Modifier.weight(1f))
                    PuceOutil("Auto", courant == "autoriser", c.green) {
                        viewModel.settingsManager.reglerPermissionOutil(nom, if (courant == "autoriser") "" else "autoriser")
                        perms = viewModel.settingsManager.permissionsOutils
                    }
                    PuceOutil("Dem.", courant == "" , c.cyan) {
                        viewModel.settingsManager.reglerPermissionOutil(nom, "")
                        perms = viewModel.settingsManager.permissionsOutils
                    }
                    PuceOutil("Refus", courant == "refuser", c.red) {
                        viewModel.settingsManager.reglerPermissionOutil(nom, if (courant == "refuser") "" else "refuser")
                        perms = viewModel.settingsManager.permissionsOutils
                    }
                    // ux-020 — autorisation restreinte à ce projet (si un dossier est ouvert).
                    if (nomProjetCourant.isNotBlank()) {
                        val proj = permsProjet[nom].orEmpty()
                        PuceOutil("Projet", proj == "autoriser", c.violet) {
                            viewModel.settingsManager.reglerPermissionOutilProjet(
                                nomProjetCourant, nom, if (proj == "autoriser") "" else "autoriser"
                            )
                            permsProjet = viewModel.settingsManager.permissionsOutilsParProjet[nomProjetCourant] ?: emptyMap()
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Session 17 / vague B — sécurité renforcée (sec-013..024) ─────────

        // sec-017 — bannière de rotation guidée si un 401 GitHub a été détecté.
        val jetonRevoqueLe = viewModel.settingsManager.jetonRevoqueDetecteLe
        if (jetonRevoqueLe > 0) {
            GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Default.Security, c.red)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.sec_rotation_titre),
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
                            color = c.red
                        )
                        Text(
                            stringResource(R.string.sec_rotation_body),
                            style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                            color = c.text2
                        )
                        Text(
                            SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()).format(Date(jetonRevoqueLe)) +
                                " — " + stringResource(R.string.history_restaurer),
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp),
                            color = c.text3,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { viewModel.settingsManager.jetonRevoqueDetecteLe = 0L }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // sec-013 — coffre d'alias secrets : ajout / suppression, jamais de lecture.
        OverlaySection(stringResource(R.string.sec_coffre_titre))
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
            Text(
                stringResource(R.string.sec_coffre_hint),
                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                color = c.text2,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            var coffre by remember { mutableStateOf(viewModel.settingsManager.secretsAlias) }
            if (coffre.isEmpty()) {
                Text(
                    stringResource(R.string.sec_coffre_vide),
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                    color = c.text3
                )
            }
            coffre.forEach { (alias, _) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = c.green, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    MonoLine(alias, c.text, Modifier.weight(1f))
                    Text(
                        stringResource(R.string.action_retirer),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = c.red,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                viewModel.settingsManager.supprimerSecret(alias)
                                coffre = viewModel.settingsManager.secretsAlias
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            var coffreOuvert by remember { mutableStateOf(false) }
            if (coffreOuvert) {
                SecreDialog(viewModel) { coffreOuvert = false; coffre = viewModel.settingsManager.secretsAlias }
            }
            Text(
                stringResource(R.string.sec_coffre_ajouter),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                color = c.cyan,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable { coffreOuvert = true }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(16.dp))

        // sec-015/019 — PIN, expiration de session et verrou biométrique.
        OverlaySection(stringResource(R.string.sec_pin_titre))
        GlassCard {
            var pinExige by remember { mutableStateOf(viewModel.settingsManager.exigePinCles) }
            ModeRow(
                title = stringResource(R.string.sec_pin_exige),
                subtitle = stringResource(R.string.sec_jetons_masques_hint),
                checked = pinExige,
                onToggle = {
                    if (viewModel.settingsManager.pinActif) {
                        viewModel.settingsManager.exigePinCles = it
                        pinExige = it
                    }
                },
                icon = Icons.Default.Lock,
                tint = c.amber
            )
            var pinOuvert by remember { mutableStateOf(false) }
            if (pinOuvert) {
                PinDialog(viewModel) { pinOuvert = false }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { pinOuvert = true }
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(Icons.Default.Bolt, c.cyan)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (viewModel.settingsManager.pinActif) stringResource(R.string.sec_pin_change)
                    else stringResource(R.string.sec_pin_definir),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = c.text
                )
            }
            var verrouBio by remember { mutableStateOf(viewModel.settingsManager.verrouBiometrie) }
            ModeRow(
                title = stringResource(R.string.sec_verrou_bio),
                subtitle = stringResource(R.string.sec_jetons_masques_hint),
                checked = verrouBio,
                onToggle = { viewModel.settingsManager.verrouBiometrie = it; verrouBio = it },
                icon = Icons.Default.Lock,
                tint = c.violet
            )
        }
        Spacer(Modifier.height(16.dp))

        // sec-021 — liste blanche de domaines pour fetch_url.
        OverlaySection(stringResource(R.string.sec_domaines_titre))
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
            Text(
                stringResource(R.string.sec_domaines_hint),
                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                color = c.text2,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            var domaines by remember { mutableStateOf(viewModel.settingsManager.domainesAutorises.joinToString(", ")) }
            OverlaySearchField(domaines, { domaines = it }, "github.com, api.z.ai…")
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.action_enregistrer),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                color = c.cyan,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable {
                        viewModel.settingsManager.domainesAutorises =
                            domaines.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(16.dp))

        // sec-024/020 — sauvegarde vérifiée + purge mémoire.
        OverlaySection(stringResource(R.string.sec_backup_export))
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
            val exportBackup = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
            ) { uri -> if (uri != null) viewModel.exporterReglagesVerifie(uri) { } }
            val importBackup = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
            ) { uri -> if (uri != null) viewModel.importerReglagesVerifie(uri) { n, err -> } }
            Text(
                stringResource(R.string.sec_backup_export),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                color = c.cyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { exportBackup.launch("zagent-reglages.json") }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            Text(
                stringResource(R.string.sec_backup_import),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                color = c.cyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { importBackup.launch(arrayOf("application/json")) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            var purgeMessage by remember { mutableStateOf("") }
            Text(
                stringResource(R.string.sec_purger_memoire),
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
                color = c.amber,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable {
                        viewModel.purgerMemoireCles { purgeMessage = "ok" }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            if (purgeMessage.isNotBlank()) {
                Text(
                    stringResource(R.string.sec_purger_ok),
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                    color = c.text2
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // sec-016 — journal d'accès aux secrets (horodaté, sans valeurs).
        OverlaySection(stringResource(R.string.sec_journal_titre))
        val journal = remember { viewModel.settingsManager.lireJournalSecrets().take(12) }
        if (journal.isEmpty()) {
            GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
                Text(
                    stringResource(R.string.sec_journal_vide),
                    style = TextStyle(fontFamily = Inter, fontSize = 12.sp),
                    color = c.text3
                )
            }
        } else {
            GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
                journal.forEach { ligne ->
                    Text(
                        ligne,
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.5.sp),
                        color = c.text2,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Chemins toujours autorisés ───────────────────────────────────────
        OverlaySection("Fichiers toujours autorisés")
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
            if (allowedPaths.isEmpty()) {
                Text(
                    stringResource(R.string.chat_toujours_liste_vide),
                    style = TextStyle(fontFamily = Inter, fontSize = 12.5.sp, lineHeight = 18.sp),
                    color = c.text2
                )
            }
            allowedPaths.forEach { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = c.green, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(10.dp))
                    MonoLine(p, c.text, Modifier.weight(1f))
                    Text(
                        stringResource(R.string.action_retirer),
                        style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = c.red,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                viewModel.removeAlwaysAllowedPath(p)
                                allowedPaths = viewModel.settingsManager.alwaysAllowedPaths.toList().sorted()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Historique des commandes approuvées ──────────────────────────────
        OverlaySection("Commandes approuvées récemment")
        val approved = remember(events) {
            events.filter { it.kind == "commande" && (it.status == ActivityStatus.OK || it.status == ActivityStatus.ERREUR) }.take(15)
        }
        if (approved.isEmpty()) {
            OverlayEmptyState(
                Icons.Default.Security,
                "Aucune commande approuvée",
                "Les commandes que tu valides (ou que le mode Auto exécute) apparaîtront ici avec leur date."
            )
        } else {
            val df = remember { SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()) }
            GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
                approved.forEach { e ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            MonoLine(e.label, c.text)
                            Text(
                                df.format(Date(e.ts)) + " · " + e.detail.ifBlank { e.status },
                                style = TextStyle(fontFamily = Inter, fontSize = 10.5.sp),
                                color = c.text3,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            e.status,
                            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = if (e.status == ActivityStatus.OK) c.green else c.amber
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    tint: androidx.compose.ui.graphics.Color = Zcolors.cyan
) {
    val c = Zcolors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = c.text
            )
            Text(
                subtitle,
                style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                color = c.text2
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.cyan,
                checkedThumbColor = c.onGrad,
                uncheckedTrackColor = c.surface3,
                uncheckedThumbColor = c.text3
            )
        )
    }
}

/** feat-045 — Petit choix d'état de permission (Auto / Dem. / Refus) par outil. */
@Composable
private fun PuceOutil(label: String, activee: Boolean, teinte: Color, onClick: () -> Unit) {
    val c = Zcolors
    Text(
        label,
        style = TextStyle(fontFamily = Inter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
        color = if (activee) c.bg else c.text2,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (activee) teinte else c.surface3)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp)
    )
}
/** sec-013 — dialogue d'ajout d'un secret sous alias (alias + valeur). */
@Composable
private fun SecreDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val c = Zcolors
    var alias by remember { mutableStateOf("") }
    var valeur by remember { mutableStateOf("") }
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sec_coffre_ajouter), fontFamily = com.zagent.app.ui.theme.SpaceGrotesk, color = c.text) },
        text = {
            Column {
                Text(
                    stringResource(R.string.sec_coffre_hint),
                    style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = c.text2
                )
                Spacer(Modifier.height(8.dp))
                OverlaySearchField(alias, { alias = it }, stringResource(R.string.sec_coffre_alias))
                Spacer(Modifier.height(6.dp))
                OverlaySearchField(valeur, { valeur = it }, stringResource(R.string.sec_coffre_valeur))
                Spacer(Modifier.height(6.dp))
                // sec-018 — coller puis PURGER le presse-papiers : limite les
                // loggers tiers qui scruteraient le presse-papiers système.
                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                Text(
                    stringResource(R.string.sec_coller_purger),
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = c.cyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(c.surface2)
                        .clickable {
                            val texte = clipboard.getText()?.text.orEmpty()
                            if (texte.isNotBlank()) {
                                valeur = texte
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(""))
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (alias.isNotBlank() && valeur.isNotBlank()) {
                    viewModel.settingsManager.definirSecret(alias, valeur)
                }
                onDismiss()
            }) { Text(stringResource(R.string.action_enregistrer), color = c.cyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_annuler), color = c.text2) }
        }
    )
}

/** sec-015 — dialogue de définition / saisie du code PIN. */
@Composable
private fun PinDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val c = Zcolors
    var saisie by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var messageOk by remember { mutableStateOf(false) }
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sec_pin_titre), fontFamily = com.zagent.app.ui.theme.SpaceGrotesk, color = c.text) },
        text = {
            Column {
                OverlaySearchField(saisie, { saisie = it }, stringResource(R.string.sec_pin_saisir))
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        message,
                        style = TextStyle(fontFamily = Inter, fontSize = 11.5.sp),
                        color = if (messageOk) c.green else c.red
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = viewModel.settingsManager
                when {
                    saisie.trim().length < 4 -> {
                        messageOk = false
                        message = "PIN trop court (4 caractères minimum)."
                    }
                    m.pinActif && m.codePin == saisie.trim() -> {
                        // Déblocage manuel : marque les clés débloquées 24 h.
                        m.verifierPin(saisie)
                        messageOk = true
                        message = "PIN validé : accès aux clés débloqué 24 h."
                    }
                    else -> {
                        // Définition / changement du PIN.
                        m.codePin = saisie
                        m.horodatageClesDebloquees = System.currentTimeMillis()
                        messageOk = true
                        message = "PIN défini."
                    }
                }
            }) { Text(stringResource(R.string.action_enregistrer), color = c.cyan) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_fermer), color = c.text2) }
        }
    )
}

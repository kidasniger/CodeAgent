import os
B='app/src/main/java/com/zagent/app/'
def rd(p): return open(B+p).read()
def wr(p,s): open(B+p,'w').write(s)
def sub(s,a,b,label):
    assert a in s, "ancre introuvable : "+label
    return s.replace(a,b,1)

if 'fun deletePath(' in rd('data/FileRepository.kt'):
    raise SystemExit("déjà appliqué")

# 1) FileRepository : créer / supprimer / renommer
s=rd('data/FileRepository.kt')
s=sub(s,'    fun makeDir(path: String): String {','''    fun createFile(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        if (resolveDocument(root, path) != null) return "Erreur: Existe déjà: $path"
        val lastSlash = path.lastIndexOf('/')
        if (lastSlash != -1) getOrCreateDocumentPath(root, path.substring(0, lastSlash), true)
        val created = getOrCreateDocumentPath(root, path, false)
        return if (created != null) "Succès: Fichier créé $path" else "Erreur: Impossible de créer $path"
    }

    fun deletePath(path: String): String {
        if (!validatePath(path)) return "Erreur: Chemin invalide (.. interdit)"
        if (path == "." || path.isEmpty() || path == "/") return "Erreur: Impossible de supprimer la racine."
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveDocument(root, path) ?: return "Erreur: Introuvable: $path"
        return try {
            if (target.delete()) "Succès: Supprimé $path" else "Erreur: Suppression impossible: $path"
        } catch (e: Exception) {
            "Erreur: ${e.localizedMessage}"
        }
    }

    fun renamePath(path: String, newName: String): String {
        if (!validatePath(path) || newName.contains('/') || newName.contains("..")) return "Erreur: Nom invalide"
        val root = getRootFolder() ?: return "Erreur: Aucun dossier de travail sélectionné."
        val target = resolveDocument(root, path) ?: return "Erreur: Introuvable: $path"
        val parentPath = if (path.contains('/')) path.substringBeforeLast('/') else "."
        val parent = resolveDocument(root, parentPath) ?: return "Erreur: Dossier parent introuvable"
        if (parent.findFile(newName) != null) return "Erreur: Existe déjà: $newName"
        return try {
            if (target.renameTo(newName)) "Succès: Renommé en $newName" else "Erreur: Renommage impossible"
        } catch (e: Exception) {
            "Erreur: ${e.localizedMessage}"
        }
    }

    fun makeDir(path: String): String {''','FileRepository')
wr('data/FileRepository.kt',s)

# 2) EditorState : onglets concernés par suppression / renommage
s=rd('ui/editor/EditorState.kt')
s=sub(s,'    fun clear() {\n        tabs.clear()','''    fun hasDirtyUnder(path: String): Boolean =
        tabs.any { it.dirty && (it.path == path || it.path.startsWith("$path/")) }

    /** Ferme les onglets d'un fichier (ou des fichiers d'un dossier) supprimé ou renommé. */
    fun closeUnder(path: String) {
        for (i in tabs.indices.reversed()) {
            val p = tabs[i].path
            if (p == path || p.startsWith("$path/")) close(i)
        }
    }

    fun clear() {
        tabs.clear()''','EditorState')
wr('ui/editor/EditorState.kt',s)

# 3) MainViewModel
s=rd('ui/viewmodel/MainViewModel.kt')
old=s[s.index('    fun refreshBrowser() {'):]
old=old[:old.index('\n    }\n')+7]
assert 'browserChildren.clear()' in old and 'listEntries(".")' in old, "refreshBrowser inattendu"
s=s.replace(old,'''    /** Recharge l'arbre en conservant les dossiers dépliés. */
    fun refreshBrowser() {
        refreshFolderLabel()
        browserLoading = true
        val dirs = listOf(".") + expandedDirs.keys.toList()
        viewModelScope.launch(Dispatchers.IO) {
            val repo = getFileRepo()
            val loaded = dirs.map { it to repo.listEntries(it) }
            withContext(Dispatchers.Main) {
                for ((p, list) in loaded) browserChildren[p] = list
                browserChildren.keys.toList().forEach { k ->
                    if (k != "." && !expandedDirs.containsKey(k)) browserChildren.remove(k)
                }
                expandedDirs.keys.toList().forEach { d ->
                    val parent = if (d.contains('/')) d.substringBeforeLast('/') else "."
                    val siblings = browserChildren[parent]
                    if (siblings == null || siblings.none { it.path == d && it.isDir }) {
                        expandedDirs.remove(d)
                        browserChildren.remove(d)
                    }
                }
                browserCount = (browserChildren["."] ?: emptyList()).count { !it.isDir }
                browserLoading = false
            }
        }
    }
''',1)

a='''            withContext(Dispatchers.Main) {
                browserChildren.remove(parentPath)
                browserChildren.remove(".")
                ensureLoaded(".")
                if (parentPath != ".") ensureLoaded(parentPath)
            }'''
s=sub(s,a,'''            withContext(Dispatchers.Main) {
                expandParents(full)
                refreshBrowser()
            }''','createBrowserFolder')

s=sub(s,'    fun openBrowserFile(path: String) {','''    private fun expandParents(path: String) {
        val parts = path.split("/").filter { it.isNotEmpty() }
        var acc = ""
        for (i in 0 until parts.size - 1) {
            acc = if (acc.isEmpty()) parts[i] else acc + "/" + parts[i]
            expandedDirs[acc] = true
        }
    }

    private fun forgetPath(path: String) {
        modifiedPaths.keys.toList().filter { it == path || it.startsWith("$path/") }.forEach { modifiedPaths.remove(it) }
        expandedDirs.keys.toList().filter { it == path || it.startsWith("$path/") }.forEach { expandedDirs.remove(it) }
        editor.closeUnder(path)
    }

    fun createBrowserFile(path: String) {
        browserError = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = getFileRepo().createFile(path)
            withContext(Dispatchers.Main) {
                if (res.startsWith("Succès")) {
                    expandParents(path)
                    refreshBrowser()
                    editor.open(path) { isEditorOpen = true }
                } else {
                    browserError = res
                }
            }
        }
    }

    fun deleteBrowserPath(path: String) {
        if (editor.hasDirtyUnder(path)) {
            browserError = "Enregistre ou ferme les onglets modifiés avant de supprimer."
            return
        }
        browserError = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = getFileRepo().deletePath(path)
            withContext(Dispatchers.Main) {
                if (res.startsWith("Succès")) {
                    forgetPath(path)
                    refreshBrowser()
                } else {
                    browserError = res
                }
            }
        }
    }

    fun renameBrowserPath(path: String, newName: String) {
        if (newName.isBlank() || newName.contains('/') || newName.contains("..")) {
            browserError = "Nom invalide."
            return
        }
        if (newName == path.substringAfterLast('/')) return
        if (editor.hasDirtyUnder(path)) {
            browserError = "Enregistre ou ferme les onglets modifiés avant de renommer."
            return
        }
        browserError = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = getFileRepo().renamePath(path, newName)
            withContext(Dispatchers.Main) {
                if (res.startsWith("Succès")) {
                    forgetPath(path)
                    refreshBrowser()
                } else {
                    browserError = res
                }
            }
        }
    }

    fun openBrowserFile(path: String) {''','VM fonctions')
wr('ui/viewmodel/MainViewModel.kt',s)

# 4) FileBrowserScreen
s=rd('ui/screens/FileBrowserScreen.kt')
s=sub(s,'import androidx.compose.material3.CircularProgressIndicator\n','''import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.remember
''','imports')
s=sub(s,'    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }\n','''    var createKind by rememberSaveable { mutableStateOf<String?>(null) }
    var createPrefix by rememberSaveable { mutableStateOf("") }
    var menuNew by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileRepository.Entry?>(null) }
    var deleteTarget by remember { mutableStateOf<FileRepository.Entry?>(null) }
''','états')
s=sub(s,'    LaunchedEffect(Unit) { viewModel.refreshBrowser() }','''    // Recharge à l'ouverture, après chaque sauvegarde de l'éditeur et après chaque écriture de l'agent.
    LaunchedEffect(viewModel.editor.saveCount, viewModel.modifiedPaths.size) { viewModel.refreshBrowser() }''','effet')
s=sub(s,'                CircleIconButton(Icons.Default.CreateNewFolder, "Nouveau dossier", onClick = { showNewFolderDialog = true })\n','''                Box {
                    CircleIconButton(Icons.Default.Add, "Nouveau", onClick = { menuNew = true })
                    DropdownMenu(
                        expanded = menuNew,
                        onDismissRequest = { menuNew = false },
                        containerColor = c.bg2
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nouveau fichier", color = c.text) },
                            onClick = { menuNew = false; createPrefix = ""; createKind = "file" }
                        )
                        DropdownMenuItem(
                            text = { Text("Nouveau dossier", color = c.text) },
                            onClick = { menuNew = false; createPrefix = ""; createKind = "folder" }
                        )
                    }
                }
''','bouton +')
s=sub(s,'                        BrowserRow(row, viewModel)\n','''                        BrowserRow(row, viewModel) { action, e ->
                            val prefix = if (e.isDir) e.path + "/" else ""
                            when (action) {
                                "newfile" -> { createPrefix = prefix; createKind = "file" }
                                "newfolder" -> { createPrefix = prefix; createKind = "folder" }
                                "rename" -> renameTarget = e
                                "delete" -> deleteTarget = e
                            }
                        }
''','appel BrowserRow')
s=sub(s,'private fun BrowserRow(row: BrowserRow, viewModel: MainViewModel) {','private fun BrowserRow(\n    row: BrowserRow,\n    viewModel: MainViewModel,\n    onAction: (String, FileRepository.Entry) -> Unit\n) {','signature')
a='''                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FileIconBadge'''
s=sub(s,a,'''                modifier = Modifier.size(18.dp)
            )
        }
        RowMenu(e) { action -> onAction(action, e) }
    }
}

@Composable
private fun FileIconBadge''','menu ligne')

start=s.index('    if (showNewFolderDialog) {')
end=s.index('\n}\n\n// ─',start)
s=s[:start]+'''    createKind?.let { kind ->
        var name by remember(kind, createPrefix) { mutableStateOf(createPrefix) }
        AlertDialog(
            onDismissRequest = { createKind = null },
            containerColor = c.bg2,
            title = {
                Text(
                    if (kind == "file") "Nouveau fichier" else "Nouveau dossier",
                    fontFamily = SpaceGrotesk, color = c.text
                )
            },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = {
                        Text(
                            if (kind == "file") "src/index.html" else "src/utils",
                            fontFamily = JetBrainsMono, color = c.text3
                        )
                    },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = name.trim().trim('/')
                    if (n.isNotEmpty()) {
                        if (kind == "file") viewModel.createBrowserFile(n) else viewModel.createBrowserFolder(n)
                    }
                    createKind = null
                }) { Text("Créer", color = c.cyan) }
            },
            dismissButton = {
                TextButton(onClick = { createKind = null }) { Text("Annuler", color = c.text2) }
            }
        )
    }

    renameTarget?.let { e ->
        var name by remember(e.path) { mutableStateOf(e.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = c.bg2,
            title = { Text("Renommer", fontFamily = SpaceGrotesk, color = c.text) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(fontFamily = JetBrainsMono, color = c.text),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameBrowserPath(e.path, name.trim())
                    renameTarget = null
                }) { Text("Renommer", color = c.cyan) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Annuler", color = c.text2) }
            }
        )
    }

    deleteTarget?.let { e ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = c.bg2,
            title = { Text("Supprimer ?", fontFamily = SpaceGrotesk, color = c.text) },
            text = {
                Text(
                    e.path + if (e.isDir) "\\n(le dossier et tout son contenu)" else "",
                    fontFamily = JetBrainsMono, fontSize = 13.sp, color = c.text2
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBrowserPath(e.path)
                    deleteTarget = null
                }) { Text("Supprimer", color = c.red) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Annuler", color = c.text2) }
            }
        )
    }'''+s[end:]

s+='''
@Composable
private fun RowMenu(e: FileRepository.Entry, onAction: (String) -> Unit) {
    val c = Zcolors
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .clickable { open = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MoreVert, "Actions", tint = c.text3, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = c.bg2
        ) {
            if (e.isDir) {
                DropdownMenuItem(
                    text = { Text("Nouveau fichier ici", color = c.text) },
                    onClick = { open = false; onAction("newfile") }
                )
                DropdownMenuItem(
                    text = { Text("Nouveau dossier ici", color = c.text) },
                    onClick = { open = false; onAction("newfolder") }
                )
            }
            DropdownMenuItem(
                text = { Text("Renommer", color = c.text) },
                onClick = { open = false; onAction("rename") }
            )
            DropdownMenuItem(
                text = { Text("Supprimer", color = c.red) },
                onClick = { open = false; onAction("delete") }
            )
        }
    }
}
'''
assert 'showNewFolderDialog' not in s, "référence restante"
wr('ui/screens/FileBrowserScreen.kt',s)
print("ok")

package com.zagent.app.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.zagent.app.data.FileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val INDENT = "  "

/** Un onglet : texte, sélection, historique annuler/refaire. */
class EditorTab(val path: String, initial: String, val crlf: Boolean) {
    val lang: Lang = SyntaxHighlighter.langFor(path)
    val name: String = path.substringAfterLast('/')

    var value by mutableStateOf(TextFieldValue(initial))
        private set
    var savedText by mutableStateOf(initial)
        private set
    /** Le fichier a été modifié par l'agent alors qu'il avait des changements non sauvegardés. */
    var externalConflict by mutableStateOf(false)

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var lastEditTime = 0L

    var undoCount by mutableIntStateOf(0)
        private set
    var redoCount by mutableIntStateOf(0)
        private set

    val dirty: Boolean get() = value.text != savedText

    private fun maxDepth() = if (value.text.length > 100_000) 20 else 150

    private fun pushUndo(old: TextFieldValue) {
        undoStack.addLast(old)
        while (undoStack.size > maxDepth()) undoStack.removeFirst()
        redoStack.clear()
        undoCount = undoStack.size
        redoCount = 0
    }

    /** Saisie utilisateur. Regroupe les frappes rapprochées en une seule étape d'annulation. */
    fun onChange(new: TextFieldValue) {
        val old = value
        if (new.text == old.text) {
            value = new
            return
        }
        val now = System.currentTimeMillis()
        val fixed = autoIndent(old, new) ?: new
        if (now - lastEditTime > 700 || undoStack.isEmpty()) pushUndo(old)
        lastEditTime = now
        value = fixed
    }

    /** Modification programmatique (remplacer, insérer) : toujours une étape d'annulation propre. */
    fun applyEdit(new: TextFieldValue) {
        if (new.text == value.text) {
            value = new
            return
        }
        pushUndo(value)
        lastEditTime = 0L
        value = new
    }

    fun setSelection(range: TextRange) {
        value = value.copy(selection = range)
    }

    fun insert(text: String) {
        val sel = value.selection
        val s = minOf(sel.start, sel.end)
        val e = maxOf(sel.start, sel.end)
        val t = value.text.replaceRange(s, e, text)
        applyEdit(TextFieldValue(t, TextRange(s + text.length)))
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(value)
        value = undoStack.removeLast()
        lastEditTime = 0L
        undoCount = undoStack.size
        redoCount = redoStack.size
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(value)
        value = redoStack.removeLast()
        lastEditTime = 0L
        undoCount = undoStack.size
        redoCount = redoStack.size
    }

    fun markSaved(text: String) {
        savedText = text
        externalConflict = false
    }

    /** Remplace tout le contenu (rechargement après écriture de l'agent). */
    fun reload(text: String) {
        pushUndo(value)
        value = TextFieldValue(text, TextRange(minOf(value.selection.start, text.length)))
        savedText = text
        externalConflict = false
        lastEditTime = 0L
    }

    /** Entrée : reprend l'indentation de la ligne précédente (+2 espaces après { [ ( ou :). */
    private fun autoIndent(old: TextFieldValue, new: TextFieldValue): TextFieldValue? {
        if (new.text.length != old.text.length + 1) return null
        if (!old.selection.collapsed) return null
        val pos = new.selection.start
        if (pos < 1 || pos > new.text.length || new.text[pos - 1] != '\n') return null
        val prevLineStart = new.text.lastIndexOf('\n', pos - 2) + 1
        val prevLine = new.text.substring(prevLineStart, pos - 1)
        val base = prevLine.takeWhile { it == ' ' || it == '\t' }
        val last = prevLine.trimEnd().lastOrNull()
        val extra = if (last == '{' || last == '[' || last == '(' || (last == ':' && lang == Lang.PYTHON)) INDENT else ""
        val add = base + extra
        if (add.isEmpty()) return null
        val t = new.text.substring(0, pos) + add + new.text.substring(pos)
        return TextFieldValue(t, TextRange(pos + add.length))
    }
}

/** Gère les onglets ouverts, le chargement et la sauvegarde. */
class EditorController(
    private val scope: CoroutineScope,
    private val repo: () -> FileRepository,
    /** Appelé à chaque changement d'onglets (chemins ouverts + index actif) pour les mémoriser (MA79). */
    private val persistSession: (List<String>, Int) -> Unit = { _, _ -> }
) {
    val tabs = mutableStateListOf<EditorTab>()
    var activeIndex by mutableIntStateOf(0)
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var info by mutableStateOf<String?>(null)
    /** Incrémenté à chaque sauvegarde réussie : déclenche le rechargement de l'aperçu. */
    var saveCount by mutableIntStateOf(0)
        private set

    fun readResource(path: String): ByteArray? = repo().readBytes(path)

    val active: EditorTab? get() = tabs.getOrNull(activeIndex)

    private var restoreJob: Job? = null

    private fun persist() {
        persistSession(tabs.map { it.path }, activeIndex)
    }

    /** Rouvre les onglets mémorisés (MA79) sans changer d'écran ; les fichiers disparus sont ignorés. */
    fun restore(paths: List<String>, active: Int) {
        if (paths.isEmpty()) return
        restoreJob?.cancel()
        restoreJob = scope.launch(Dispatchers.IO) {
            val loaded = ArrayList<Triple<String, String, Boolean>>()
            for (p in paths) {
                repo().readFileForEdit(p).onSuccess { raw ->
                    val crlf = raw.contains("\r\n")
                    loaded.add(Triple(p, if (crlf) raw.replace("\r\n", "\n") else raw, crlf))
                }
            }
            withContext(Dispatchers.Main) {
                if (loaded.isEmpty()) return@withContext
                val wasEmpty = tabs.isEmpty()
                for ((p, text, crlf) in loaded) {
                    if (tabs.none { it.path == p }) tabs.add(EditorTab(p, text, crlf))
                }
                if (wasEmpty) {
                    val activePath = paths.getOrNull(active)
                    activeIndex = tabs.indexOfFirst { it.path == activePath }.coerceAtLeast(0)
                }
                persist()
            }
        }
    }

    fun open(path: String, onOpened: () -> Unit = {}) {
        openAtLine(path, null, onOpened)
    }

    /** Ligne à atteindre après la prochaine ouverture (MA46) ; -1 = aucune. */
    var pendingFocusLine by mutableIntStateOf(-1)

    /** Ouvre un fichier et fait défiler jusqu'à la ligne demandée si fournie (MA46). */
    fun openAtLine(path: String, line: Int?, onOpened: () -> Unit = {}) {
        pendingFocusLine = line ?: -1
        val existing = tabs.indexOfFirst { it.path == path }
        if (existing >= 0) {
            activeIndex = existing
            persist()
            onOpened()
            return
        }
        if (loading) return
        loading = true
        error = null
        scope.launch(Dispatchers.IO) {
            val result = repo().readFileForEdit(path)
            withContext(Dispatchers.Main) {
                loading = false
                result.onSuccess { raw ->
                    val crlf = raw.contains("\r\n")
                    val text = if (crlf) raw.replace("\r\n", "\n") else raw
                    tabs.add(EditorTab(path, text, crlf))
                    activeIndex = tabs.lastIndex
                    persist()
                    onOpened()
                }.onFailure {
                    error = it.message ?: "Impossible d'ouvrir $path"
                }
            }
        }
    }

    fun select(index: Int) {
        if (index in tabs.indices) {
            activeIndex = index
            persist()
        }
    }

    /**
     * Ferme tous les onglets sauf [keep] (MA79). Les onglets modifiés sont conservés pour ne
     * rien perdre ; renvoie leur nombre.
     */
    fun closeOthers(keep: EditorTab): Int {
        val kept = tabs.count { it !== keep && it.dirty }
        tabs.removeAll { it !== keep && !it.dirty }
        activeIndex = tabs.indexOf(keep).coerceAtLeast(0)
        persist()
        return kept
    }

    fun close(index: Int) {
        if (index !in tabs.indices) return
        tabs.removeAt(index)
        activeIndex = when {
            tabs.isEmpty() -> 0
            activeIndex > index -> activeIndex - 1
            activeIndex >= tabs.size -> tabs.lastIndex
            else -> activeIndex
        }
        persist()
    }

    fun save(tab: EditorTab, onDone: (Boolean) -> Unit = {}) {
        val text = tab.value.text
        val out = if (tab.crlf) text.replace("\n", "\r\n") else text
        scope.launch(Dispatchers.IO) {
            val res = repo().writeFile(tab.path, out)
            withContext(Dispatchers.Main) {
                val ok = res.startsWith("Succès")
                if (ok) {
                    tab.markSaved(text)
                    saveCount++
                    info = "Enregistré · ${tab.name}"
                } else {
                    error = res
                }
                onDone(ok)
            }
        }
    }

    /** Appelé quand l'agent écrit un fichier : recharge l'onglet ou signale un conflit. */
    fun onExternalWrite(path: String, content: String) {
        val tab = tabs.firstOrNull { it.path == path } ?: return
        val text = content.replace("\r\n", "\n")
        if (tab.value.text == text) {
            tab.markSaved(text)
        } else if (tab.dirty) {
            tab.externalConflict = true
        } else {
            tab.reload(text)
        }
    }

    /** Recharge le contenu disque dans l'onglet (après un conflit avec l'agent). */
    fun reloadFromDisk(tab: EditorTab) {
        scope.launch(Dispatchers.IO) {
            val result = repo().readFileForEdit(tab.path)
            withContext(Dispatchers.Main) {
                result.onSuccess { raw -> tab.reload(raw.replace("\r\n", "\n")) }
                    .onFailure { error = it.message }
            }
        }
    }

    fun hasDirtyUnder(path: String): Boolean =
        tabs.any { it.dirty && (it.path == path || it.path.startsWith("$path/")) }

    /** Ferme les onglets d'un fichier (ou des fichiers d'un dossier) supprimé ou renommé. */
    fun closeUnder(path: String) {
        for (i in tabs.indices.reversed()) {
            val p = tabs[i].path
            if (p == path || p.startsWith("$path/")) close(i)
        }
    }

    /** Vide les onglets sans toucher à la session mémorisée (changement de projet). */
    fun clear() {
        restoreJob?.cancel()
        tabs.clear()
        activeIndex = 0
    }
}

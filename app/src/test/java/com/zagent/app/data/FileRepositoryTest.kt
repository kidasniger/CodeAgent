package com.zagent.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * corr-025/026 — Tests de confinement du chemin dans FileRepository :
 * traversées (../, ..\, %2e%2e), liens symboliques, écriture/lecture UTF-8.
 * Pure JVM : FileRepository accepte un contexte null (seul fileUri l'utilise).
 */
class FileRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var repo: FileRepository
    private lateinit var outside: File

    @Before
    fun setUp() {
        outside = tmp.newFolder("hors-projet")
        File(outside, "secret.txt").writeText("SECRET")
        val root = tmp.newFolder("projet")
        repo = FileRepository(null, root.absolutePath)
    }

    @Test
    fun `traversee ../ rejetee`() {
        val res = repo.readFile("../hors-projet/secret.txt")
        assertTrue(res.startsWith("Erreur"))
        assertFalse(res.contains("SECRET"))
    }

    @Test
    fun `traversee ..\\ rejetee`() {
        val res = repo.readFile("..\\hors-projet\\secret.txt")
        assertTrue(res.startsWith("Erreur"))
        assertFalse(res.contains("SECRET"))
    }

    @Test
    fun `traversee %2e%2e encodee rejetee`() {
        assertTrue(repo.readFile("%2e%2e/hors-projet/secret.txt").startsWith("Erreur"))
        assertTrue(repo.readFile("%2E%2E%2Fhors-projet%2Fsecret.txt").startsWith("Erreur"))
        assertTrue(repo.readFile("sous/%2e%2e/%2e%2e/hors-projet/secret.txt").startsWith("Erreur"))
    }

    @Test
    fun `caractere nul rejeté`() {
        assertTrue(repo.readFile("a\u0000b.txt").startsWith("Erreur"))
    }

    @Test
    fun `lien symbolique sortant rejeté`() {
        val projetDir = tmp.root.resolve("projet")
        val lien = File(projetDir, "lien")
        try {
            java.nio.file.Files.createSymbolicLink(
                lien.toPath(),
                outside.toPath()
            )
        } catch (e: Exception) {
            // systèmes sans permission symlink : test ignoré
            return
        }
        assertNull(repo.readBytes("lien/secret.txt"))
        assertTrue(repo.readFile("lien/secret.txt").startsWith("Erreur"))
    }

    @Test
    fun `chemin valide fonctionne toujours`() {
        File(tmp.root, "projet/a.txt").writeText("ok")
        assertEquals("ok", repo.readFile("a.txt"))
    }

    @Test
    fun `écriture puis relecture UTF-8`() {
        val res = repo.writeFile("src/éà.txt", "contenu éà€ — 🎉")
        assertTrue(res.startsWith("Succès"))
        assertEquals("contenu éà€ — 🎉", repo.readFile("src/éà.txt"))
    }

    @Test
    fun `écriture hors projet rejetée`() {
        val res = repo.writeFile("../fuite.txt", "nope")
        assertTrue(res.startsWith("Erreur"))
        assertFalse(File(outside, "fuite.txt").exists())
    }

    @Test
    fun `BOM retiré à la lecture éditeur`() {
        val f = File(tmp.root, "projet/bom.md")
        f.writeBytes(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "# titre".toByteArray())
        val contenu = repo.readFileForEdit("bom.md").getOrThrow()
        assertEquals("# titre", contenu)
    }

    @Test
    fun `CRLF préservé à la lecture éditeur`() {
        File(tmp.root, "projet/crlf.txt").writeText("l1\r\nl2\r\n")
        assertEquals("l1\r\nl2\r\n", repo.readFileForEdit("crlf.txt").getOrThrow())
    }

    @Test
    fun `dossier racine non supprimable`() {
        assertTrue(repo.deletePath(".").startsWith("Erreur"))
        assertTrue(repo.deletePath("").startsWith("Erreur"))
    }
}

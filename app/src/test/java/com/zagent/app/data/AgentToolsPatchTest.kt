package com.zagent.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * feat-029 — Tests du parsing de apply_patch (formats diff unifié et
 * bloc *** Begin Patch). Pure logique : aucun contexte Android requis.
 */
class AgentToolsPatchTest {

    @Test
    fun `bloc aider - update add delete`() {
        val patch = """
            *** Begin Patch
            *** Update File: src/A.kt
            @@
             fun a() {}
            -fun b() {}
            +fun b2() {}
            *** Add File: src/Nouveau.kt
            +val x = 1
            *** Delete File: src/Vieux.kt
            *** End Patch
        """.trimIndent()

        val hunks = AgentTools.parsePatch(patch)

        assertEquals(3, hunks.size)
        val update = hunks[0]
        assertEquals("update", update.action)
        assertEquals("src/A.kt", update.path)
        assertTrue(update.hunkOld.contains("fun a() {}"))
        assertTrue(update.hunkOld.contains("fun b() {}"))
        assertTrue(update.hunkNew.contains("fun a() {}"))
        assertTrue(update.hunkNew.contains("fun b2() {}"))
        assertTrue(!update.hunkNew.contains("fun b()\n"))

        val add = hunks[1]
        assertEquals("add", add.action)
        assertEquals("src/Nouveau.kt", add.path)
        assertEquals("", add.hunkOld)
        assertEquals("val x = 1\n", add.hunkNew)

        val delete = hunks[2]
        assertEquals("delete", delete.action)
        assertEquals("src/Vieux.kt", delete.path)
    }

    @Test
    fun `diff unifie - un seul hunk`() {
        val patch = """
            --- a/app/main.py
            +++ b/app/main.py
            @@ -1,3 +1,3 @@
             import os
            -print("old")
            +print("new")
             print("fin")
        """.trimIndent()

        val hunks = AgentTools.parsePatch(patch)

        assertEquals(1, hunks.size)
        assertEquals("update", hunks[0].action)
        assertEquals("app/main.py", hunks[0].path)
        assertEquals("import os\nprint(\"old\")\nprint(\"fin\")\n", hunks[0].hunkOld)
        assertEquals("import os\nprint(\"new\")\nprint(\"fin\")\n", hunks[0].hunkNew)
    }

    @Test
    fun `patch illisible - liste vide`() {
        assertTrue(AgentTools.parsePatch("du texte quelconque sans marqueur").isEmpty())
        assertTrue(AgentTools.parsePatch("").isEmpty())
    }
}

package com.zagent.app.data

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * err-098 — Tests du parsing tolérant des arguments d'outils
 * (argumentsLenients) : JSON valide, fences markdown, JSON tronqué,
 * texte autour de l'objet, virgule finale, entrée vide.
 */
class AgentToolsArgumentsLenientTest {

    @Test
    fun `json valide reste intact`() {
        val o = AgentTools.argumentsLenients("""{"path":"a.txt","content":"x"}""")
        assertEquals("a.txt", o["path"]?.jsonPrimitive?.content)
        assertEquals("x", o["content"]?.jsonPrimitive?.content)
    }

    @Test
    fun `fence markdown retiré`() {
        val o = AgentTools.argumentsLenients("```json\n{\"path\":\"a.txt\"}\n```")
        assertEquals("a.txt", o["path"]?.jsonPrimitive?.content)
    }

    @Test
    fun `json tronqué en pleine chaîne est rééquilibré`() {
        val o = AgentTools.argumentsLenients("{\"path\":\"src/ma")
        assertEquals("src/ma", o["path"]?.jsonPrimitive?.content)
    }

    @Test
    fun `json tronqué avec accolades non fermées`() {
        val o = AgentTools.argumentsLenients("{\"a\":{\"b\":\"c\"")
        assertEquals("c", o["a"]!!.jsonObject["b"]?.jsonPrimitive?.content)
    }

    @Test
    fun `texte avant l'accolade accepté`() {
        val o = AgentTools.argumentsLenients("Voici les arguments : {\"n\": 1, \"path\":\"b\"}")
        assertEquals("b", o["path"]?.jsonPrimitive?.content)
    }

    @Test
    fun `virgule finale tolérée`() {
        val o = AgentTools.argumentsLenients("{\"a\":1,}")
        assertTrue(o.containsKey("a"))
    }

    @Test
    fun `entrée vide renvoie un objet vide`() {
        assertTrue(AgentTools.argumentsLenients("").isEmpty())
        assertTrue(AgentTools.argumentsLenients("   ").isEmpty())
    }

    @Test
    fun `entrée non JSON renvoie un objet vide sans planter`() {
        assertTrue(AgentTools.argumentsLenients("???").isEmpty())
    }
}

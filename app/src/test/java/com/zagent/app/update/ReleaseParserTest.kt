package com.zagent.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParserTest {
    private val sha = "a".repeat(64)

    private val fullRelease = """
    {"tag_name":"v1.0-b7","body":"## Notes\n- Correction **chat**\n- Nouveau `agent`","assets":[
      {"name":"ZAgent-v1.0-b7.apk","size":1234,"browser_download_url":"https://example.com/a.apk","digest":"sha256:$sha"},
      {"name":"ZAgent-v1.0-b7.apk.sha256","size":100,"browser_download_url":"https://example.com/a.sha256"}
    ]}
    """.trimIndent()

    @Test
    fun parsesApkDigestAndNotes() {
        val result = ReleaseParser.parse(fullRelease) as ParseResult.Success
        assertEquals("v1.0-b7", result.info.tag)
        assertEquals("ZAgent-v1.0-b7.apk", result.info.apk.name)
        assertEquals(1234L, result.info.apk.size)
        assertEquals(sha, result.info.apk.sha256)
        assertNotNull(result.info.sha256Url)
        assertTrue(result.info.notes.contains("• Correction chat"))
        assertTrue(result.info.notes.contains("• Nouveau agent"))
    }

    @Test
    fun releaseWithoutApkIsRejected() {
        val json = """{"tag_name":"v1.0-b1","assets":[{"name":"notes.txt","size":1,"browser_download_url":"https://example.com/n.txt"}]}"""
        assertTrue(ReleaseParser.parse(json) is ParseResult.Failure)
    }

    @Test
    fun invalidJsonIsRejected() {
        assertTrue(ReleaseParser.parse("not json") is ParseResult.Failure)
    }

    @Test
    fun missingTagIsRejected() {
        assertTrue(ReleaseParser.parse("""{"assets":[]}""") is ParseResult.Failure)
    }

    @Test
    fun nullBodyAndMissingDigestAreHandled() {
        val json = """{"tag_name":"v1.0-b2","body":null,"assets":[{"name":"a.apk","size":10,"browser_download_url":"https://example.com/a.apk"}]}"""
        val result = ReleaseParser.parse(json) as ParseResult.Success
        assertNull(result.info.apk.sha256)
        assertNull(result.info.sha256Url)
        assertEquals("• Corrections et améliorations.", result.info.notes)
    }

    @Test
    fun invalidDigestIsIgnored() {
        assertNull(ReleaseParser.normalizeSha("pas-un-hash"))
        assertEquals(sha, ReleaseParser.normalizeSha(sha.uppercase()))
    }
}
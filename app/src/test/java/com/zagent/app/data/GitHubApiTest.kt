package com.zagent.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Session 15 (ghc-006) — tests des fonctions pures de GitHubApi : extraction
 * owner/repo des remotes et normalisation « owner/repo » / URL → base API.
 * Aucun réseau : on ne teste que la logique d'adresse, déterministe.
 */
class GitHubApiTest {

    // ── parseOwnerRepo (existant, feat-088/089) ────────────────────────────

    @Test
    fun parseOwnerRepo_urlHttps() {
        val r = GitHubApi.parseOwnerRepo("https://github.com/kidasniger/CodeAgent.git")
        assertEquals("kidasniger" to "CodeAgent", r)
    }

    @Test
    fun parseOwnerRepo_urlSsh() {
        val r = GitHubApi.parseOwnerRepo("git@github.com:kidasniger/CodeAgent.git")
        assertEquals("kidasniger" to "CodeAgent", r)
    }

    @Test
    fun parseOwnerRepo_sansSuffixe() {
        val r = GitHubApi.parseOwnerRepo("https://github.com/owner/repo")
        assertEquals("owner" to "repo", r)
    }

    @Test
    fun parseOwnerRepo_invalide() {
        assertNull(GitHubApi.parseOwnerRepo("https://gitlab.com/owner/repo"))
        assertNull(GitHubApi.parseOwnerRepo(""))
    }

    // ── depotApiBase (nouveau, session 15 : accepte aussi « owner/repo ») ──

    @Test
    fun depotApiBase_ownerRepo() {
        assertEquals(
            "https://api.github.com/repos/kidasniger/CodeAgent",
            GitHubApi.depotApiBase("kidasniger/CodeAgent")
        )
    }

    @Test
    fun depotApiBase_urlHttps() {
        assertEquals(
            "https://api.github.com/repos/kidasniger/CodeAgent",
            GitHubApi.depotApiBase("https://github.com/kidasniger/CodeAgent.git")
        )
    }

    @Test
    fun depotApiBase_urlSsh() {
        assertEquals(
            "https://api.github.com/repos/owner/repo",
            GitHubApi.depotApiBase("git@github.com:owner/repo.git")
        )
    }

    @Test
    fun depotApiBase_invalide() {
        assertNull(GitHubApi.depotApiBase(""))
        assertNull(GitHubApi.depotApiBase("pasunrepo"))
        assertNull(GitHubApi.depotApiBase("https://gitlab.com/owner/repo"))
    }

    @Test
    fun depotApiBase_superjeuDeApiBase() {
        // toute URL acceptée par apiBase l'est aussi par depotApiBase (session 15)
        val url = "https://github.com/owner/repo/"
        assertEquals(GitHubApi.apiBase(url), GitHubApi.depotApiBase(url))
        assertNotNull(GitHubApi.depotApiBase(url))
        assertTrue(GitHubApi.depotApiBase(url)!!.endsWith("/repos/owner/repo"))
    }

    @Test
    fun depotApiBase_tiretsEtPoints() {
        assertEquals(
            "https://api.github.com/repos/mon-orga/mon.repo_1",
            GitHubApi.depotApiBase("mon-orga/mon.repo_1")
        )
    }
}

package com.zagent.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun newerBuildNumberIsDetected() {
        assertTrue(VersionComparator.isNewer("v1.0-b12", "1.0-b11", 11L))
    }

    @Test
    fun sameBuildNumberIsNotNewer() {
        assertFalse(VersionComparator.isNewer("v1.0-b11", "1.0-b11", 11L))
    }

    @Test
    fun olderBuildNumberIsNotNewer() {
        assertFalse(VersionComparator.isNewer("v1.0-b5", "1.0-b11", 11L))
    }

    @Test
    fun buildNumberIsComparedToVersionCodeNotToName() {
        assertTrue(VersionComparator.isNewer("v2.0-b3", "1.0", 2L))
        assertFalse(VersionComparator.isNewer("v2.0-b3", "1.0", 3L))
    }

    @Test
    fun emptyOrGarbageTagIsNeverNewer() {
        assertFalse(VersionComparator.isNewer("", "1.0", 1L))
        assertFalse(VersionComparator.isNewer("v", "1.0", 1L))
        assertFalse(VersionComparator.isNewer("abc", "1.0", 1L))
    }

    @Test
    fun semanticVersionsAreComparedNumerically() {
        assertTrue(VersionComparator.isNewer("1.2.0", "1.1.9", 1L))
        assertTrue(VersionComparator.isNewer("1.10.0", "1.9.0", 1L))
        assertTrue(VersionComparator.isNewer("v2", "1.9.9", 1L))
        assertFalse(VersionComparator.isNewer("1.0.0", "1.0", 1L))
    }

    @Test
    fun preReleaseIsOlderThanFinalRelease() {
        assertFalse(VersionComparator.isNewer("1.2.0-rc1", "1.2.0", 1L))
        assertTrue(VersionComparator.isNewer("1.2.0", "1.2.0-rc1", 1L))
    }

    @Test
    fun buildNumberIsExtractedFromTag() {
        assertEquals(12L, VersionComparator.buildNumber("v1.0-b12"))
        assertNull(VersionComparator.buildNumber("1.0"))
        assertNull(VersionComparator.buildNumber("1.0-beta2"))
    }
}
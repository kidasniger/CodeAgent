package com.zagent.app.update

/**
 * Compare la version distante (tag GitHub) à la version installée.
 * Les tags de release ont la forme v1.0-b12 : le numéro après "-b" est le versionCode,
 * c'est lui qu'Android utilise pour accepter ou refuser une mise à jour.
 */
object VersionComparator {
    private val BUILD_SUFFIX = Regex("-b(\\d+)$")

    fun buildNumber(tag: String): Long? =
        BUILD_SUFFIX.find(tag.trim())?.groupValues?.get(1)?.toLongOrNull()

    fun isNewer(remoteTag: String, localVersionName: String, localVersionCode: Long): Boolean {
        val remote = remoteTag.trim().removePrefix("v").removePrefix("V")
        if (remote.isEmpty()) return false
        val build = buildNumber(remote)
        if (build != null) return build > localVersionCode
        return compareSemantic(remote, localVersionName.trim().removePrefix("v").removePrefix("V")) > 0
    }

    fun compareSemantic(a: String, b: String): Int {
        val (coreA, preA) = split(a)
        val (coreB, preB) = split(b)
        val size = maxOf(coreA.size, coreB.size)
        for (i in 0 until size) {
            val x = coreA.getOrElse(i) { 0L }
            val y = coreB.getOrElse(i) { 0L }
            if (x != y) return x.compareTo(y)
        }
        return when {
            preA == null && preB == null -> 0
            preA == null -> 1
            preB == null -> -1
            else -> preA.compareTo(preB)
        }
    }

    private fun split(version: String): Pair<List<Long>, String?> {
        val main = version.substringBefore('+')
        val core = main.substringBefore('-')
        val pre = if (main.contains('-')) main.substringAfter('-') else null
        val numbers = core.split('.').map { part -> part.filter { it.isDigit() }.toLongOrNull() ?: 0L }
        return Pair(numbers, pre)
    }
}
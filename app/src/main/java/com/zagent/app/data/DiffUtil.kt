package com.zagent.app.data

/**
 * Mini-diff ligne à ligne (LCS) pour l'aperçu de confirmation d'écriture.
 * Borné : au-delà de 300 lignes par côté, on retombe sur un diff naïf.
 */
object DiffUtil {

    enum class Kind { CTX, ADD, DEL }

    data class Line(
        val kind: Kind,
        val oldNum: Int?,
        val newNum: Int?,
        val text: String
    )

    data class DiffResult(
        val lines: List<Line>,
        val added: Int,
        val removed: Int,
        val isUpdate: Boolean
    )

    fun diff(old: String?, new: String): DiffResult {
        if (old == null) {
            val newLines = new.lines()
            return DiffResult(
                lines = newLines.mapIndexed { i, l -> Line(Kind.ADD, null, i + 1, l) },
                added = newLines.size,
                removed = 0,
                isUpdate = false
            )
        }

        val a = old.lines()
        val b = new.lines()

        val ops = if (a.size <= 300 && b.size <= 300) lcsOps(a, b) else naiveOps(a, b)

        val out = mutableListOf<Line>()
        var oldNo = 0
        var newNo = 0
        var added = 0
        var removed = 0
        for (op in ops) {
            when (op.kind) {
                OpKind.EQ -> {
                    oldNo++; newNo++
                    out.add(Line(Kind.CTX, oldNo, newNo, op.text))
                }
                OpKind.DEL -> {
                    oldNo++; removed++
                    out.add(Line(Kind.DEL, oldNo, null, op.text))
                }
                OpKind.ADD -> {
                    newNo++; added++
                    out.add(Line(Kind.ADD, null, newNo, op.text))
                }
            }
        }
        return DiffResult(out, added, removed, isUpdate = true)
    }

    private enum class OpKind { EQ, ADD, DEL }
    private data class Op(val kind: OpKind, val text: String)

    /** LCS classique, retourne la liste d'opérations dans l'ordre du fichier. */
    private fun lcsOps(a: List<String>, b: List<String>): List<Op> {
        val n = a.size
        val m = b.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1
                else maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
        val ops = mutableListOf<Op>()
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                a[i] == b[j] -> { ops.add(Op(OpKind.EQ, a[i])); i++; j++ }
                dp[i + 1][j] >= dp[i][j + 1] -> { ops.add(Op(OpKind.DEL, a[i])); i++ }
                else -> { ops.add(Op(OpKind.ADD, b[j])); j++ }
            }
        }
        while (i < n) { ops.add(Op(OpKind.DEL, a[i])); i++ }
        while (j < m) { ops.add(Op(OpKind.ADD, b[j])); j++ }
        return ops
    }

    /** Fichiers très longs : tout l'ancien devient supprimé, tout le nouveau ajouté. */
    private fun naiveOps(a: List<String>, b: List<String>): List<Op> {
        val ops = mutableListOf<Op>()
        a.forEach { ops.add(Op(OpKind.DEL, it)) }
        b.forEach { ops.add(Op(OpKind.ADD, it)) }
        return ops
    }
}

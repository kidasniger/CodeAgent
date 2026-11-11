package com.zagent.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * corr-060 — Redimensionnement des images jointes avant envoi.
 *
 * Les photos d'appareil font souvent 4000 px de côté (8 à 12 Mo en JPEG).
 * Les envoyer telles quelles saturerait le contexte du modèle et ralentirait
 * l'upload mobile. On décode donc l'image avec `inSampleSize` (mémoire
 * maîtrisée : jamais le bitmap plein en RAM), on la remet à l'échelle pour que
 * le grand côté ne dépasse pas [MAX_SIDE] px, puis on l'écrit en JPEG qualité
 * [JPEG_QUALITY] dans un cache privé. L'Uri d'origine n'est jamais modifiée.
 */
object ImageResize {

    /** Grand côté maximal de l'image envoyée, en pixels. */
    const val MAX_SIDE = 1600

    /** Qualité JPEG du fichier redimensionné (0-100). */
    const val JPEG_QUALITY = 82

    /**
     * Redimensionne l'image pointée par [uri] et renvoie l'Uri du fichier
     * redimensionné (cache privé de l'appli). Renvoie [uri] inchangée si
     * l'image est déjà plus petite que [MAX_SIDE] ou si le décodage échoue.
     * Appeler hors du thread principal (décodage disque).
     */
    fun resizeForSending(context: Context, uri: Uri): Uri {
        return try {
            val resolver = context.contentResolver

            // 1) Dimensions réelles sans décoder les pixels.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return uri
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return uri

            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            if (maxSide <= MAX_SIDE) return uri // déjà raisonnable, rien à faire

            // 2) Sous-échantillonnage grossier par puissances de 2 (mémoire).
            var sample = 1
            while (maxSide / (sample * 2) >= MAX_SIDE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return uri

            // 3) Mise à l'échelle fine du grand côté à MAX_SIDE.
            val w = bitmap.width
            val h = bitmap.height
            val scale = MAX_SIDE.toFloat() / maxOf(w, h)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, (w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1), true)
            } else bitmap
            if (scaled !== bitmap) bitmap.recycle()

            // 4) Écriture JPEG dans le cache : fichiers réutilisables par nom stable.
            val out = File(context.cacheDir, "attachments/resized_${System.currentTimeMillis()}.jpg")
            out.parentFile?.mkdirs()
            out.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            scaled.recycle()
            Uri.fromFile(out)
        } catch (e: Exception) {
            ZLog.e("ImageResize", "redimensionnement impossible, image d'origine conservée", e)
            uri
        }
    }

    /** Poids approximatif du fichier pointé, en octets (-1 si inconnu). */
    fun sizeOf(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        } catch (e: Exception) {
            -1L
        }
    }
}

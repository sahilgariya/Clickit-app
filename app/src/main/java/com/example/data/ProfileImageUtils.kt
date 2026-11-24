package com.example.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val MAX_AVATAR_DIMENSION = 320
private const val MAX_PHOTO_DATA_URI_LENGTH = 190_000

suspend fun encodeProfileImageToDataUri(
    contentResolver: ContentResolver,
    uri: Uri
): String = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    } ?: throw IllegalArgumentException("Could not open selected image.")

    val rawWidth = bounds.outWidth
    val rawHeight = bounds.outHeight
    if (rawWidth <= 0 || rawHeight <= 0) {
        throw IllegalArgumentException("Invalid image format.")
    }

    var sampleSize = 1
    while (rawWidth / sampleSize > MAX_AVATAR_DIMENSION * 2 || rawHeight / sampleSize > MAX_AVATAR_DIMENSION * 2) {
        sampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOpts)
    } ?: throw IllegalArgumentException("Could not decode selected image.")

    val scaled = try {
        val largestSide = max(decoded.width, decoded.height)
        if (largestSide > MAX_AVATAR_DIMENSION) {
            val ratio = MAX_AVATAR_DIMENSION.toFloat() / largestSide.toFloat()
            val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
        } else {
            decoded
        }
    } finally {
        // Scaled bitmap may alias decoded; handled below
    }

    try {
        var quality = 82
        var dataUri = ""
        while (quality >= 40) {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            dataUri = "data:image/jpeg;base64,$b64"
            if (dataUri.length <= MAX_PHOTO_DATA_URI_LENGTH) break
            quality -= 14
        }
        require(dataUri.length <= 200_000) { "Selected image is too large. Please pick a smaller photo." }
        dataUri
    } finally {
        if (scaled !== decoded && !scaled.isRecycled) {
            scaled.recycle()
        }
        if (!decoded.isRecycled) {
            decoded.recycle()
        }
    }
}

fun decodeDataUriToImageBitmap(dataUri: String): ImageBitmap? {
    if (!dataUri.startsWith("data:image/")) return null
    val commaIndex = dataUri.indexOf(',')
    if (commaIndex <= 0 || commaIndex >= dataUri.length - 1) return null
    return try {
        val bytes = Base64.decode(dataUri.substring(commaIndex + 1), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: IllegalArgumentException) {
        null
    }
}

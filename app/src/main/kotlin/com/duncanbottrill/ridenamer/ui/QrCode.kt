package com.duncanbottrill.ridenamer.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

/** Renders [text] as a QR-code [ImageBitmap], or null if encoding fails. */
fun generateQrCode(text: String, size: Int = 600): ImageBitmap? = runCatching {
    val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val offset = y * size
        for (x in 0 until size) {
            pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        .apply { setPixels(pixels, 0, size, 0, 0, size, size) }
        .asImageBitmap()
}.getOrNull()

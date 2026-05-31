package com.example.snapmind.core.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log

object BitmapDecoder {
    private const val TAG = "BitmapDecoder"

    fun decodeSampled(
        contentResolver: ContentResolver,
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    ): Bitmap? {
        return try {
            // 1st pass: bounds only. Stream is consumed and closed here.
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }

            if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                Log.w(TAG, "Cannot determine bounds for $uri")
                return null
            }

            opts.inSampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, targetWidth, targetHeight)
            opts.inJustDecodeBounds = false
            opts.inPreferredConfig = config

            // 2nd pass: fresh stream for actual decode.
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM decoding $uri", oom)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding $uri", e)
            null
        }
    }

    private fun calculateSampleSize(rawW: Int, rawH: Int, targetW: Int, targetH: Int): Int {
        var size = 1
        while (rawW / (size * 2) >= targetW && rawH / (size * 2) >= targetH) {
            size *= 2
        }
        return size
    }
}

package com.rumahsehat.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun compressImage(inputFile: File, outputFile: File) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(inputFile.absolutePath, options)

        val maxWidth = 1024
        val maxHeight = 768
        var inSampleSize = 1

        if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                inSampleSize *= 2
            }
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath, options)

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, true)
        
        FileOutputStream(outputFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
        }
    }
}

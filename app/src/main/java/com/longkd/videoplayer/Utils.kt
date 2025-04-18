package com.longkd.videoplayer

import android.content.Context
import java.io.File
import java.io.FileOutputStream

fun copyAssetToInternalStorage(context: Context, assetName: String): String {
    val file = File(context.filesDir, assetName)
    if (!file.exists()) {
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
    return file.absolutePath
}

package com.pdam.report.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

private const val FILENAME_FORMAT = "dd-MM-yyyy"
private const val MAXIMAL_SIZE = 1000000

fun getCurrentTimeStamp(): String {
    return SimpleDateFormat(
        FILENAME_FORMAT,
        Locale.US
    ).format(System.currentTimeMillis())
}

fun createCustomTempFile(context: Context): File {
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(getCurrentTimeStamp(), ".jpg", storageDir)
}

//fun uriToFile(selectedImg: Uri, context: Context): File {
//    val contentResolver: ContentResolver = context.contentResolver
//    val myFile = createCustomTempFile(context)
//    contentResolver.openInputStream(selectedImg)?.use { inputStream ->
//        FileOutputStream(myFile).use { outputStream ->
//            inputStream.copyTo(outputStream)
//        }
//    }
//    return myFile
//}

fun reduceFileImage(file: File): File {
    val bitmap = BitmapFactory.decodeFile(file.path)
    var compressQuality = 100
    var streamLength: Int

    do {
        val bmpStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
        val bmpPicByteArray = bmpStream.toByteArray()
        streamLength = bmpPicByteArray.size
        compressQuality -= 5
    } while (streamLength > MAXIMAL_SIZE)

    FileOutputStream(file).use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, outputStream)
    }

    return file
}

suspend fun File.reduceFileImageInBackground(): File = withContext(Dispatchers.Default) {
    return@withContext reduceFileImage(this@reduceFileImageInBackground)
}

fun parsingNameImage(url: String): String {
    val startIndex = url.indexOf("%2F")
    if (startIndex != -1) { // Pastikan "%2F" ditemukan dalam URL
        val endIndex = url.indexOf(".jpg") + 4 // Mencari posisi akhir ekstensi ".jpg" dan menambahkan 4 karakter untuk menyertakan ".jpg"
        if (endIndex != -1) { // Pastikan ekstensi ".jpg" ditemukan dalam URL
            return url.substring(startIndex + 3, endIndex) // Mengambil potongan string dari "%2F" hingga ekstensi ".jpg" (termasuk ".jpg")
        }
    }
    return ""
}


fun milisToDate(milis: Long): String {
    val formatter = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
    return formatter.format(milis)
}

fun milisToDateTime(milis: Long): String {
    val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)
    return formatter.format(milis)
}

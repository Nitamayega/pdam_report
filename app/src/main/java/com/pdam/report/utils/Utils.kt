package com.pdam.report.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.TimeInfo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Locale

private const val FILENAME_FORMAT = "dd-MM-yyyy"
private const val MAXIMAL_SIZE = 1_000_000

fun getCurrentTimeStamp(): String {
    return SimpleDateFormat(
        FILENAME_FORMAT,
        Locale.US
    ).format(System.currentTimeMillis())
}

// Panggil fungsi ini untuk mendapatkan waktu NTP di latar belakang
fun getNetworkTimeInBackground(): Long {
    val timeServers = listOf(
        "0.id.pool.ntp.org",
        "1.id.pool.ntp.org",
        "2.id.pool.ntp.org",
        "3.id.pool.ntp.org"
        )

    for (server in timeServers) {
        try {
            val client = NTPUDPClient()
            client.defaultTimeout = 5000
            val address = InetAddress.getByName(server)
            val info: TimeInfo = client.getTime(address)
            return info.message.transmitTimeStamp.time
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return 0 // Mengembalikan 0 jika semua server gagal
}

suspend fun getNetworkTime(): Long = withContext(Dispatchers.Default) {
    return@withContext getNetworkTimeInBackground()
}

fun createCustomTempFile(context: Context): File {
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(getCurrentTimeStamp(), ".jpg", storageDir)
}

fun uriToFile(selectedImg: Uri, context: Context): File {
    val contentResolver: ContentResolver = context.contentResolver
    val myFile = createCustomTempFile(context)
    contentResolver.openInputStream(selectedImg)?.use { inputStream ->
        FileOutputStream(myFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    return myFile
}

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

fun intentSetting(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", "com.pdam.report", null)
    intent.data = uri
    context.startActivity(intent)
}


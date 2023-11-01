package com.pdam.report.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
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

// Mendapatkan timestamp saat ini
fun getCurrentTimeStamp(): String {
    return SimpleDateFormat(
        FILENAME_FORMAT,
        Locale.US
    ).format(System.currentTimeMillis())
}

// Fungsi untuk mendapatkan waktu dari server NTP di background menggunakan coroutine
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

// Fungsi untuk mendapatkan waktu dari server NTP menggunakan coroutine
suspend fun getNetworkTime(): Long = withContext(Dispatchers.Default) {
    return@withContext getNetworkTimeInBackground()
}

// Membuat file sementara di direktori Pictures
fun createCustomTempFile(context: Context): File {
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(getCurrentTimeStamp(), ".jpg", storageDir)
}

// Mengubah Uri menjadi File
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

// Mengurangi ukuran file gambar menjadi ukuran yang ditentukan
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

// Fungsi yang menjalankan pengurangan ukuran file gambar di background menggunakan coroutine
suspend fun File.reduceFileImageInBackground(): File = withContext(Dispatchers.Default) {
    return@withContext reduceFileImage(this@reduceFileImageInBackground)
}

// Parsing nama file gambar dari URL
fun parsingNameImage(url: String): String {
    val startIndex = url.indexOf("%2F")
    if (startIndex != -1) {
        val endIndex = url.indexOf(".jpg") + 4
        if (endIndex != -1) {
            return url.substring(startIndex + 3, endIndex)
        }
    }
    return ""
}

// Mengonversi timestamp ke tanggal
fun milisToDate(milis: Long): String {
    val formatter = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
    return formatter.format(milis)
}

// Mengonversi timestamp ke tanggal dan waktu
fun milisToDateTime(milis: Long): String {
    val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)
    return formatter.format(milis)
}

// Mengarahkan ke pengaturan aplikasi
fun intentSetting(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", "com.pdam.report", null)
    intent.data = uri
    context.startActivity(intent)
}

// Fungsi untuk mengunggah file ke Firebase Storage dan mendapatkan URL-nya
suspend fun uploadImageAndGetUrl(ref: StorageReference, file: File?): String? {
    return try {
        if (file != null) {
            val downloadUrl = ref.putFile(Uri.fromFile(file)).continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                ref.downloadUrl
            }.await()
            downloadUrl.toString()
        } else {
            null
        }
    } catch (e: Exception) {
        // Handle error
        null
    }
}




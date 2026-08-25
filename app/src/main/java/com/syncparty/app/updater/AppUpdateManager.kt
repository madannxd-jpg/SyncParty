package com.syncparty.app.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.syncparty.app.data.local.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String = "",
    val isMandatory: Boolean = false
)

class AppUpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val sessionManager = UserSessionManager.getInstance(context)

    fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdates(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val checkUrl = sessionManager.updateUrlFlow.firstOrNull()
                ?: "https://raw.githubusercontent.com/madannxd-jpg/SyncParty/main/version.json"

            val request = Request.Builder().url(checkUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP Error: ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty update response"))
            val updateInfo = gson.fromJson(body, AppUpdateInfo::class.java)

            if (updateInfo.versionCode > getCurrentVersionCode()) {
                Result.success(updateInfo)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            Result.failure(e)
        }
    }

    suspend fun downloadAndInstallApk(
        updateInfo: AppUpdateInfo,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(updateInfo.apkUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty download body"))
            val contentLength = body.contentLength()

            val updateDir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
            val apkFile = File(updateDir, "SyncParty_v${updateInfo.versionName}.apk")

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            val progress = ((totalRead * 100) / contentLength).toInt()
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main) {
                installApk(apkFile)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update APK", e)
            Result.failure(e)
        }
    }

    fun installApk(apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
        }
    }

    companion object {
        private const val TAG = "AppUpdateManager"
    }
}

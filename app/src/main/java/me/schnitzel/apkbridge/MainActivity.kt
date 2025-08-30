package me.schnitzel.apkbridge

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.schnitzel.apkbridge.ui.theme.ApkBridgeTheme
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import uy.kohesive.injekt.Injekt
import java.io.File

@JvmField
var pm: PackageManager? = null

@JvmField
var instance: MainActivity? = null

@SuppressLint("StaticFieldLeak")
@JvmField
var preferenceManager: PreferenceManager? = null

fun log(message: String?) {
    log(message, LogLevel.INFO)
}

fun log(message: String?, level: LogLevel) {
    instance?.model?.log(message, level)
}

class MainActivity : ComponentActivity() {
    val model: AppViewModel = AppViewModel()
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                val install = Intent(Intent.ACTION_VIEW)
                if (context != null && model.apkName.isNotBlank()) {
                    Log.d("Downloaded", model.apkName)
                    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.setDataAndType(
                        FileProvider.getUriForFile(
                            context, "${context.applicationContext.packageName}.provider", File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                model.apkName
                            )
                        ), "application/vnd.android.package-archive"
                    )
                    startActivity(install)
                }
            } catch (e: Exception) {
                e.message?.let { Log.e("Downloaded", it) }
            }
        }
    }
    private var networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val manager = getSystemService(
                ConnectivityManager::class.java
            )
            val prop = manager.getLinkProperties(network)
            val ipv4 =
                prop!!.linkAddresses.firstOrNull { la -> la.address.hostAddress?.indexOf(':')!! < 0 }
            val anyIp = prop.linkAddresses.firstOrNull()
            model.addressText = if (ipv4 != null) {
                ipv4.address.hostAddress?.toString() ?: "Failed to fetch IP"
            } else if (anyIp != null) {
                anyIp.address.hostAddress?.toString() ?: "Failed to fetch IP"
            } else {
                "Failed to fetch IP"
            }
        }

        override fun onLost(network: Network) {
            model.addressText = "No internet connection"
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        preferenceManager = PreferenceManager(applicationContext)
        Injekt.importModule(AppModule(this))
        val connectivityManager =
            applicationContext.getSystemService(ConnectivityManager::class.java)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        askNotificationPermission(registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> })
        val channel = NotificationChannel(
            "ApkBridgeServiceChannelId",
            "ApkBridge Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(
                broadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        } else {
            registerReceiver(
                broadcastReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                RECEIVER_EXPORTED
            )
        }

        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            val client = OkHttpClient()
            checkUpdate(applicationContext, client)
        }

        pm = packageManager
        enableEdgeToEdge()
        val context = this
        val currentVersion =
            context.packageManager.getPackageInfo(context.packageName, 0).versionName

        setContent {
            ApkBridgeTheme {
                ApkBridge(
                    context = this,
                    currentVersion = currentVersion,
                    model = model,
                    requestDownload = {
                        coroutineScope.launch {
                            val client = OkHttpClient()
                            requestDownload(
                                applicationContext, model.updateResponse, client
                            )
                        }
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        val connectivityManager =
            applicationContext.getSystemService(ConnectivityManager::class.java)
        connectivityManager.unregisterNetworkCallback(networkCallback)
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    private fun askNotificationPermission(requestPermission: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermission.launch(POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkUpdate(context: Context, client: OkHttpClient) {
        makeGetRequest(client,
            "https://github.com/Schnitzel5/ApkBridge/releases/latest",
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body.string()
                    runOnUiThread {
                        if (isNewUpdateAvailable(context, responseData) == true) {
                            model.updateResponse = responseData
                            model.updateText =
                                "v${getCurrentVersion(context)} -> v${getLatestVersion(responseData)}"
                            model.showNewUpdate = true
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Request Failure.")
                }
            })
    }

    private fun isNewUpdateAvailable(context: Context, response: String): Boolean? {
        val latestVersion = getLatestVersion(response)
        val currentVersionName = getCurrentVersion(context)
        if (latestVersion != null) return latestVersion != currentVersionName
        return null
    }

    private fun getCurrentVersion(context: Context): String? {
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    private fun getLatestVersion(response: String): String? {
        return Regex("[0-9]+\\.[0-9]+").find(response)?.value
    }

    private fun requestDownload(context: Context, response: String, client: OkHttpClient) {
        val src =
            Regex("\"http.+?expanded.+?\"").find(response)?.value?.removeSurrounding("\"") ?: return
        makeGetRequest(client, src, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val res = response.body.string()
                val apkLink =
                    Regex("\".+?\\.apk\"").find(res)?.value?.removeSurrounding("\"") ?: return
                val title =
                    Regex(">.+?\\.apk<").find(res)?.value?.removeSurrounding(">", "<") ?: return
                val request = DownloadManager.Request(Uri.parse("https://github.com/$apkLink"))
                val downloadManager =
                    context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                model.apkName = title
                request.setTitle(title)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title)
                downloadManager.enqueue(request)
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Request Failure.")
            }
        })
    }

    private fun makeGetRequest(client: OkHttpClient, url: String, callback: Callback) {
        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)
        call.enqueue(callback)
    }
}

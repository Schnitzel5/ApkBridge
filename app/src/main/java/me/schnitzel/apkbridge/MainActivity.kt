package me.schnitzel.apkbridge

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.schnitzel.apkbridge.ui.theme.ApkBridgeTheme
import me.schnitzel.apkbridge.web.service.WebService
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import uy.kohesive.injekt.Injekt

@JvmField
var pm: PackageManager? = null

class MainActivity : ComponentActivity() {
    private var addressText by mutableStateOf("No info")
    private var updateResponse by mutableStateOf("")
    private var updateText by mutableStateOf("")
    private var showNewUpdate by mutableStateOf(false)
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val install = Intent(Intent.ACTION_VIEW)
            install.setDataAndType(Uri.parse(DownloadManager.COLUMN_LOCAL_URI), "MIME-TYPE")
            startActivity(install)
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
            addressText = if (ipv4 != null) {
                ipv4.address.hostAddress?.toString() ?: "Failed to fetch IP"
            } else if (anyIp != null) {
                anyIp.address.hostAddress?.toString() ?: "Failed to fetch IP"
            } else {
                "Failed to fetch IP"
            }
        }

        override fun onLost(network: Network) {
            addressText = "No internet connection"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injekt.importModule(AppModule(this))
        val connectivityManager =
            applicationContext.getSystemService(ConnectivityManager::class.java)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        val channel = NotificationChannel(
            "ApkBridgeServiceChannelId",
            "ApkBridge Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(broadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        } else {
            registerReceiver(broadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_NOT_EXPORTED)
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
                Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
                    Text(
                        text = "Current version: v$currentVersion",
                        modifier = Modifier
                            .padding(8.dp)
                            .alpha(0.5f),
                        fontSize = 12.sp
                    )
                }) { innerPadding ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            showNewUpdate -> {
                                NewUpdateDialog(
                                    updateText = updateText,
                                    onDismissRequest = { showNewUpdate = false },
                                    onConfirmation = {
                                        coroutineScope.launch {
                                            val client = OkHttpClient()
                                            requestDownload(applicationContext, updateResponse, client)
                                        }
                                    },
                                )
                            }
                        }
                        Greeting(
                            name = "ApkBridge", modifier = Modifier.padding(innerPadding)
                        )
                        ServerContent(
                            context = context, addressText = addressText
                        )
                    }
                }
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

    private fun checkUpdate(context: Context, client: OkHttpClient) {
        makeGetRequest(
            client,
            "https://github.com/Schnitzel5/ApkBridge/releases/latest",
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body.string()
                    runOnUiThread {
                        if (isNewUpdateAvailable(context, responseData) == true) {
                            updateResponse = responseData
                            updateText = "v${getCurrentVersion(context)} -> v${getLatestVersion(responseData)}"
                            showNewUpdate = true
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
                    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        fontSize = 46.sp
    )
}

@Composable
fun ServerContent(context: Context, addressText: String) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var status by rememberSaveable { mutableStateOf("Stopped") }
    var addressButton by rememberSaveable { mutableStateOf("Show Server IP") }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "State: $status",
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        if (status != "Running") {
            Button(
                onClick = {
                    status = "Starting..."
                    Intent(context.applicationContext, WebService::class.java).also {
                        it.action = WebService.Actions.START.toString()
                        context.applicationContext.startService(it)
                        status = "Running"
                    }
                    Toast.makeText(context, "Starting server...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(bottom = 16.dp),
            ) { Text("Start server") }
        }
        if (status != "Stopped") {
            Button(
                onClick = {
                    status = "Stopping..."
                    Intent(context.applicationContext, WebService::class.java).also {
                        it.action = WebService.Actions.STOP.toString()
                        context.applicationContext.startService(it)
                        status = "Stopped"
                    }
                    Toast.makeText(context, "Stopping server...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(bottom = 8.dp),
            ) { Text("Stop server") }
        }
        Text(
            text = "Server IP: ${if (addressButton == "Show Server IP") "*.*.*.*" else addressText}",
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = {
                addressButton =
                    if (addressButton == "Show Server IP") "Hide Server IP" else "Show Server IP"
            },
        ) { Text(addressButton) }
        Button(
            onClick = {
                val data = ClipData.newPlainText("text", addressText)
                clipboardManager.setPrimaryClip(data)
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            },
        ) { Text("Copy to clipboard") }
        Button(
            onClick = {
                uriHandler.openUri("https://github.com/Schnitzel5/ApkBridge")
            },
            modifier = Modifier.padding(top = 40.dp),
        ) { Text("GitHub repository") }
    }
}

@Composable
fun NewUpdateDialog(
    updateText: String,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Dialog(onDismissRequest = {
        onDismissRequest()
    }) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "New Update Available",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
                Text(
                    text = updateText,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp),
                )
                Button(
                    onClick = { onConfirmation() },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Update Now")
                }
                TextButton(
                    onClick = {
                        uriHandler.openUri("https://github.com/Schnitzel5/ApkBridge/releases/latest")
                    },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(style = MaterialTheme.typography.bodySmall, text = "View Release Notes")
                }
            }
        }
    }
}

package me.schnitzel.apkbridge

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    private var networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val manager = getSystemService(
                ConnectivityManager::class.java
            )
            val prop = manager.getLinkProperties(network)
            val ipv4 =
                prop!!.linkAddresses.firstOrNull { la -> la.address.hostAddress?.indexOf(':')!! < 0 }
            val anyIp = prop.linkAddresses.firstOrNull()
            if (ipv4 != null) {
                addressText = ipv4.address.hostAddress?.toString() ?: "Failed to fetch IP"
            } else if (anyIp != null) {
                addressText = anyIp.address.hostAddress?.toString() ?: "Failed to fetch IP"
            } else {
                addressText = "Failed to fetch IP"
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
                        modifier = Modifier.padding(8.dp).alpha(0.5f),
                        fontSize = 12.sp
                    )
                }) { innerPadding ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
        super.onDestroy()
    }

    private fun checkUpdate(context: Context, client: OkHttpClient) {
        makeGetRequest(
            client,
            "https://github.com/Schnitzel5/ApkBridge/releases/latest",
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body.string()
                    if (isNewUpdateAvailable(context, responseData) == true) {
                        requestDownload(context, responseData, client)
                    }
                    //runOnUiThread {}
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Request Failure.")
                }
            })
    }

    private fun isNewUpdateAvailable(context: Context, response: String): Boolean? {
        val latestVersion = Regex("[0-9]+\\.[0-9]+").find(response)?.value
        val currentVersionName =
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        if (latestVersion != null) return latestVersion != currentVersionName
        return null
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
                //runOnUiThread {}
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

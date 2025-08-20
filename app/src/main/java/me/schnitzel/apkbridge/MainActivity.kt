package me.schnitzel.apkbridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import me.schnitzel.apkbridge.ui.theme.ApkBridgeTheme
import me.schnitzel.apkbridge.web.service.WebService
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
            addressText =
                prop!!.linkAddresses.first { la -> la.address.hostAddress?.indexOf(':')!! < 0 }.address.hostAddress?.toString()
                    ?: "Failed to fetch IP"
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

        pm = packageManager
        enableEdgeToEdge()
        val context = this
        setContent {
            ApkBridgeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Greeting(
                            name = "ApkBridge",
                            modifier = Modifier.padding(innerPadding)
                        )
                        ServerContent(
                            context = context,
                            addressText = addressText
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
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier
    )
}

@Composable
fun ServerContent(context: Context, addressText: String) {
    val uriHandler = LocalUriHandler.current
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
                    Toast.makeText(context, "Starting server...", Toast.LENGTH_SHORT)
                        .show()
                },
                modifier = Modifier.padding(bottom = 8.dp),
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
                    Toast.makeText(context, "Stopping server...", Toast.LENGTH_SHORT)
                        .show()
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
                uriHandler.openUri("https://github.com/Schnitzel5/ApkBridge")
            },
            modifier = Modifier.padding(top = 40.dp),
        ) { Text("GitHub repository") }
    }
}

package me.schnitzel.apkbridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import kotlinx.coroutines.Job
import me.schnitzel.apkbridge.web.service.WebService

@Composable
fun MainScreen(
    navController: NavHostController,
    context: Context,
    modifier: Modifier,
    model: AppViewModel,
    requestDownload: () -> Job,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            model.showNewUpdate -> {
                NewUpdateDialog(
                    updateText = model.updateText,
                    onDismissRequest = { model.showNewUpdate = false },
                    onConfirmation = {
                        requestDownload()
                    },
                )
            }
        }
        Greeting(
            name = "ApkBridge", modifier = modifier
        )
        ServerContent(
            navController = navController,
            context = context, addressText = model.addressText
        )
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
fun ServerContent(navController: NavHostController, context: Context, addressText: String) {
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
                navController.navigate(AppRoutes.AppLogs.name)
            },
            modifier = Modifier.padding(top = 40.dp),
        ) { Text("View logs") }
        Button(
            onClick = {
                uriHandler.openUri("https://github.com/Schnitzel5/ApkBridge")
            },
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

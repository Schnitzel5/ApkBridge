package me.schnitzel.apkbridge

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Job

enum class AppRoutes(@StringRes val title: Int) {
    Start(title = R.string.app_name), AppLogs(title = R.string.app_logs)
}

@Composable
fun ApkBridge(
    context: Context,
    currentVersion: String?,
    model: AppViewModel,
    requestDownload: () -> Job,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        Text(
            text = "Current version: v$currentVersion",
            modifier = Modifier
                .padding(24.dp)
                .alpha(0.5f),
            fontSize = 12.sp
        )
    }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.Start.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = AppRoutes.Start.name) {
                MainScreen(
                    navController = navController,
                    context = context,
                    modifier = Modifier.padding(innerPadding),
                    model = model,
                    requestDownload = requestDownload
                )
            }
            composable(route = AppRoutes.AppLogs.name) {
                LogScreen(
                    navController = navController,
                    context = context,
                    model = model
                )
            }
        }
    }
}

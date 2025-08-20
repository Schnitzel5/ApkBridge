package me.schnitzel.apkbridge

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class AppModule(private val ctx: Context) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        val app = ctx.applicationContext as Application
        addSingleton(app)

        addSingletonFactory {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        }

        addSingletonFactory { NetworkHelper(ctx) }

        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(app).execute {
            get<NetworkHelper>()
        }
    }
}

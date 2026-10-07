package dev.wificloak

import android.app.Application
import dev.wificloak.service.FrameworkBridge

class CloakApplication : Application() {
    val bridge by lazy { FrameworkBridge() }
    override fun onCreate() {
        super.onCreate()
        bridge.connect()
    }
}

package dev.wificloak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.wificloak.ui.CloakApp
import dev.wificloak.ui.CloakTheme

class MainActivity : ComponentActivity() {
    private val manager: ManagerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CloakTheme { CloakApp(manager) } }
    }
    override fun onResume() {
        super.onResume()
        manager.refresh()
    }
}

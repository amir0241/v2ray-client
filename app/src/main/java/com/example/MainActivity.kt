package com.example

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.AppDatabase
import com.example.data.ProfileRepository
import com.example.data.ProxyProfile
import com.example.service.ProxyVpnService
import com.example.ui.DashboardScreen
import com.example.ui.ProfilesScreen
import com.example.ui.ProxyViewModel
import com.example.ui.ProxyViewModelFactory
import com.example.ui.SettingsScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { ProfileRepository(database.profileDao()) }
    
    private val viewModel: ProxyViewModel by viewModels {
        ProxyViewModelFactory(repository)
    }

    // Capture standard system VPN permissions and startup on approval
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.selectedProfile.value?.let { startVpnService(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainLayout(
                    viewModel = viewModel,
                    onConnectToggle = { profile -> toggleVpn(profile) }
                )
            }
        }
    }

    private fun toggleVpn(profile: ProxyProfile) {
        if (ProxyVpnService.vpnState.value == ProxyVpnService.Companion.State.CONNECTED) {
            val intent = Intent(this, ProxyVpnService::class.java).apply {
                action = ProxyVpnService.ACTION_DISCONNECT
            }
            try {
                startService(intent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to stop VPN: ${e.message}")
                Toast.makeText(this, "Failed to stop service: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                try {
                    vpnPermissionLauncher.launch(vpnIntent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to launch VPN dialog: ${e.message}")
                    Toast.makeText(this, "Failed to launch VPN settings: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                startVpnService(profile)
            }
        }
    }

    private fun startVpnService(profile: ProxyProfile) {
        val intent = Intent(this, ProxyVpnService::class.java).apply {
            action = ProxyVpnService.ACTION_CONNECT
            putExtra(ProxyVpnService.EXTRA_SERVER_NAME, profile.name)
            putExtra(ProxyVpnService.EXTRA_PROTOCOL_TYPE, profile.type)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start VPN service: ${e.message}")
            Toast.makeText(this, "Failed to start VPN service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun MainLayout(
    viewModel: ProxyViewModel,
    onConnectToggle: (ProxyProfile) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCosmicSlate),
        bottomBar = {
            NavigationBar(
                containerColor = TechCardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("app_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.ElectricBolt, contentDescription = "Dashboard Toggle") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        unselectedIconColor = PaleBlueGrey,
                        unselectedTextColor = PaleBlueGrey,
                        indicatorColor = DeepIndigoBg
                    ),
                    modifier = Modifier.testTag("nav_dashboard")
                )

                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Dns, contentDescription = "Profile Management") },
                    label = { Text("Configs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        unselectedIconColor = PaleBlueGrey,
                        unselectedTextColor = PaleBlueGrey,
                        indicatorColor = DeepIndigoBg
                    ),
                    modifier = Modifier.testTag("nav_configs")
                )

                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Advanced Settings Options") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        unselectedIconColor = PaleBlueGrey,
                        unselectedTextColor = PaleBlueGrey,
                        indicatorColor = DeepIndigoBg
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            when (activeTab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onConnectToggle = onConnectToggle,
                    onNavigateToProfiles = { activeTab = 1 }
                )
                1 -> ProfilesScreen(viewModel = viewModel)
                2 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

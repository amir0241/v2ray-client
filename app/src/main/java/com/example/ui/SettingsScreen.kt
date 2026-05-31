package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun SettingsScreen(viewModel: ProxyViewModel) {
    val routingMode by viewModel.routingMode.collectAsState()
    val dnsServer by viewModel.dnsServer.collectAsState()
    val showOnlySecure by viewModel.showOnlySecure.collectAsState()

    val simulatedLogs = remember {
        listOf(
            "[INFO] V2Ray Kernel core v5.12.1 successfully loaded in cache.",
            "[DEBUG] Scanning and initializing routing routing rules.",
            "[INFO] Loaded routing configs: bypass LAN and local broadcast addresses.",
            "[INFO] Outbound protocol VMess-Security-AEAD enabled.",
            "[INFO] Outbound protocol VLESS-Vision flow decryption ready.",
            "[INFO] Local proxy agent listening on socks:127.0.0.1:10808, http:127.0.0.1:10809",
            "[SYSTEM] VpnService starting loopback bridge interface...",
            "[DEBUG] Tun device MTU size locked at standard 1500 bytes for stability.",
            "[INFO] Routing rule matched context: Bypass local LAN network."
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCosmicSlate)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Core Header Settings Title
        item {
            Column {
                Text(
                    text = "ADVANCED TUNNEL SETTINGS",
                    style = MaterialTheme.typography.labelMedium,
                    color = PaleBlueGrey,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Configure engine cores and routing preferences",
                    style = MaterialTheme.typography.bodySmall,
                    color = BorderSlate
                )
            }
        }

        // Routing Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = TechCardBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Route, contentDescription = "Routing Rules", tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Routing Strategy Presets", fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val strategies = listOf("Bypass LAN", "Global Mode", "Bypass China Rules")
                    strategies.forEach { strat ->
                        val isSelected = routingMode == strat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkCosmicSlate else Color.Transparent)
                                .clickable { viewModel.setRoutingMode(strat) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strat,
                                color = if (isSelected) CyberCyan else PaleBlueGrey,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Selected", tint = CyberCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // DNS and Safety Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = TechCardBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.FilterAlt, contentDescription = "Tunnel configurations", tint = SolarPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DNS & Filters", fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Local DNS choice row
                    Text("Local DNS Server", color = PaleBlueGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("1.1.1.1", "8.8.8.8", "Custom").forEach { dns ->
                            val isSelected = dnsServer == dns
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SolarPurple else DarkCosmicSlate)
                                    .clickable { viewModel.setDnsServer(dns) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dns,
                                    color = if (isSelected) IceWhite else PaleBlueGrey,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Only TLS switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Only TLS Nodes", color = IceWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Filter profile list to only TLS security", color = PaleBlueGrey, fontSize = 11.sp)
                        }
                        Switch(
                            checked = showOnlySecure,
                            onCheckedChange = { viewModel.toggleShowOnlySecure(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SolarPurple,
                                checkedTrackColor = SolarPurple.copy(alpha = 0.4f),
                                uncheckedThumbColor = BorderSlate,
                                uncheckedTrackColor = DarkCosmicSlate
                            )
                        )
                    }
                }
            }
        }

        // Immersive Terminal V2Ray Console / Diagnostic Logs
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = TechCardBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = "Core Diagnostic logs", tint = SafeAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Core Console Logs", fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Console console logs wrapper
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B0F19)) // Slate jet black
                            .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        simulatedLogs.forEach { log ->
                            Text(
                                text = log,
                                color = if (log.contains("[INFO]")) Color(0xFF60A5FA) else if (log.contains("[SYSTEM]")) Color(0xFF34D399) else Color(0xFF9CA3AF),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // About section
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = TechCardBg)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "V2Proxy Core Client v1.0",
                        fontWeight = FontWeight.Bold,
                        color = IceWhite,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Modern V2Ray connection agent designed with premium Material 3 layouts.",
                        color = PaleBlueGrey,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}

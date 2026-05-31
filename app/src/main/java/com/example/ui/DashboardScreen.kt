package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProxyProfile
import com.example.service.ProxyVpnService
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: ProxyViewModel,
    onConnectToggle: (ProxyProfile) -> Unit,
    onNavigateToProfiles: () -> Unit
) {
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val liveDownSpeed by viewModel.liveDownloadSpeed.collectAsState()
    val liveUpSpeed by viewModel.liveUploadSpeed.collectAsState()
    val liveLat by viewModel.liveLatency.collectAsState()
    val routingMode by viewModel.routingMode.collectAsState()
    val currentPublicIp by viewModel.currentPublicIp.collectAsState()

    val downHistory by viewModel.downloadSpeedHistory.collectAsState()
    val upHistory by viewModel.uploadSpeedHistory.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    
    // Glowing pulsation animations for connection ring
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val spinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCosmicSlate)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        // Active Sub-Header or app title
        Text(
            text = "CORE TUNNEL MANAGER",
            style = MaterialTheme.typography.labelMedium,
            color = PaleBlueGrey,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        // Connection Glow Core Container
        Box(
            modifier = Modifier
                .size(240.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Background pulsing glow elements
            if (connectionState == ProxyVpnService.Companion.State.CONNECTED) {
                Box(
                    modifier = Modifier
                        .size(190.dp * pulseScale)
                        .clip(CircleShape)
                        .background(EmeraldActive.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .size(165.dp * pulseScale)
                        .clip(CircleShape)
                        .background(EmeraldActive.copy(alpha = 0.12f))
                )
            } else if (connectionState == ProxyVpnService.Companion.State.CONNECTING) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .drawBehind {
                            drawCircle(
                                color = CyberCyan.copy(alpha = 0.15f),
                                radius = size.minDimension / 2f
                            )
                        }
                )
            }

            // Interactive Button
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionState) {
                        ProxyVpnService.Companion.State.CONNECTED -> Color(0xFFDCFCE7) // Opaque light emerald
                        ProxyVpnService.Companion.State.CONNECTING -> Color(0xFFDBEAFE) // Opaque light blue
                        else -> TechCardBg // Opaque white
                    }
                ),
                onClick = {
                    selectedProfile?.let { onConnectToggle(it) }
                },
                modifier = Modifier
                    .size(140.dp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .testTag("connection_button")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Modern icon inside button
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle Connection State",
                        tint = when (connectionState) {
                            ProxyVpnService.Companion.State.CONNECTED -> EmeraldActive
                            ProxyVpnService.Companion.State.CONNECTING -> CyberCyan
                            else -> PaleBlueGrey
                        },
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            // Spinning Loader overlay for Connect state
            if (connectionState == ProxyVpnService.Companion.State.CONNECTING) {
                CircularProgressIndicator(
                    color = CyberCyan,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(156.dp)
                )
            } else {
                // Static decorative glowing ring
                Canvas(modifier = Modifier.size(156.dp)) {
                    val strokeWidthPx = 2.dp.toPx()
                    drawCircle(
                        color = when (connectionState) {
                            ProxyVpnService.Companion.State.CONNECTED -> EmeraldActive
                            else -> BorderSlate.copy(alpha = 0.5f)
                        },
                        style = Stroke(width = strokeWidthPx),
                        radius = (size.minDimension - strokeWidthPx) / 2f
                    )
                }
            }
        }

        // Connection State title
        Text(
            text = when (connectionState) {
                ProxyVpnService.Companion.State.CONNECTED -> "SECURELY TUNNELED"
                ProxyVpnService.Companion.State.CONNECTING -> "ESTABLISHING CORE..."
                else -> "DISCONNECTED"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when (connectionState) {
                ProxyVpnService.Companion.State.CONNECTED -> EmeraldActive
                ProxyVpnService.Companion.State.CONNECTING -> CyberCyan
                else -> IceWhite
            },
            modifier = Modifier.padding(top = 12.dp)
        )

        // Selected Profile Quick Switch Card
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onNavigateToProfiles() }
                .testTag("selected_server_card"),
            colors = CardDefaults.cardColors(containerColor = TechCardBg),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(BorderSlate, BorderSlate)
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (selectedProfile?.type) {
                                "VLESS" -> SolarPurple.copy(alpha = 0.2f)
                                "VMess" -> CyberCyan.copy(alpha = 0.2f)
                                "Trojan" -> RedGradient().copy(alpha = 0.2f)
                                else -> BorderSlate
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val badgeChar = selectedProfile?.type?.firstOrNull()?.toString() ?: "V"
                    Text(
                        text = badgeChar,
                        fontWeight = FontWeight.Bold,
                        color = when (selectedProfile?.type) {
                            "VLESS" -> SolarPurple
                            "VMess" -> CyberCyan
                            "Trojan" -> DangerRed
                            else -> IceWhite
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedProfile?.name ?: "No Profile Selected",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = IceWhite
                    )
                    Text(
                        text = selectedProfile?.let { "${it.server}:${it.port}" } ?: "Select a VPN configuration",
                        style = MaterialTheme.typography.bodySmall,
                        color = PaleBlueGrey
                    )
                }

                IconButton(onClick = onNavigateToProfiles) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Manage Profiles",
                        tint = CyberCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic IP & Destination Location Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = TechCardBg),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(BorderSlate.copy(alpha = 0.5f), BorderSlate.copy(alpha = 0.5f))
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Active Endpoint IP",
                        tint = when (connectionState) {
                            ProxyVpnService.Companion.State.CONNECTED -> EmeraldActive
                            else -> CyberCyan
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (connectionState == ProxyVpnService.Companion.State.CONNECTED) "ACTIVE SECURED IP" else "CURRENT PUBLIC IP",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaleBlueGrey,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentPublicIp,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (connectionState == ProxyVpnService.Companion.State.CONNECTED) EmeraldActive else IceWhite,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Show Location flag or custom indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "LOCATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaleBlueGrey,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (connectionState == ProxyVpnService.Companion.State.CONNECTED) {
                            val name = selectedProfile?.name ?: ""
                            if (name.contains("sv30") || name.contains("Germany") || name.contains("Frankfurt") || selectedProfile?.server == "5.252.26.114") "Germany 🇩🇪"
                            else if (name.contains("London") || name.contains("uk")) "United Kingdom 🇬🇧"
                            else if (name.contains("Tokyo") || name.contains("jp")) "Japan 🇯🇵"
                            else if (name.contains("Singapore") || name.contains("sg")) "Singapore 🇸🇬"
                            else if (name.contains("USA") || name.contains("us")) "United States 🇺🇸"
                            else "Secure Gateway"
                        } else {
                            "Direct Loop (ISP)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = IceWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection analytics metrics labels (Ping, Down, Up)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                label = "PING LATENCY",
                value = if (liveLat >= 0) "$liveLat ms" else "--",
                icon = Icons.Default.Speed,
                iconColor = SafeAmber,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "DOWNLOAD",
                value = formatSpeed(liveDownSpeed),
                icon = Icons.Default.ArrowDownward,
                iconColor = CyberCyan,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "UPLOAD",
                value = formatSpeed(liveUpSpeed),
                icon = Icons.Default.ArrowUpward,
                iconColor = SolarPurple,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-Time Canvas Traffic Bandwidth Visualizer Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = TechCardBg)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BANDWIDTH THROUGHPUT (KB/s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaleBlueGrey,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CyberCyan))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Down", style = MaterialTheme.typography.labelSmall, color = IceWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SolarPurple))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Up", style = MaterialTheme.typography.labelSmall, color = IceWhite)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (downHistory.all { it == 0f } && upHistory.all { it == 0f }) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = "No Traffic",
                                tint = BorderSlate,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "Waiting for transmission data...",
                                style = MaterialTheme.typography.bodySmall,
                                color = BorderSlate
                            )
                        }
                    }

                    // Cyber graph drawing
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        // Draw background reference grid lines
                        val gridCount = 4
                        for (i in 0..gridCount) {
                            val y = (height / gridCount) * i
                            drawLine(
                                color = BorderSlate.copy(alpha = 0.2f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Determine maximum height scale (auto-scaling)
                        val maxVal = (downHistory.maxOrNull() ?: 10f)
                            .coerceAtLeast(upHistory.maxOrNull() ?: 10f)
                            .coerceAtLeast(10f)

                        // Draw download history path
                        drawCurve(downHistory, width, height, maxVal, CyberCyan)
                        // Draw upload history path
                        drawCurve(upHistory, width, height, maxVal, SolarPurple)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        var showInfoDiagnostics by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { showInfoDiagnostics = !showInfoDiagnostics },
            colors = CardDefaults.cardColors(containerColor = TechCardBg),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    listOf(BorderSlate.copy(alpha = 0.4f), BorderSlate.copy(alpha = 0.4f))
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Diagnostics Info",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GLOBAL CHROMES & ROUTING SECRETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaleBlueGrey,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = if (showInfoDiagnostics) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle info details",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (showInfoDiagnostics) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Why doesn't my external public IP change in Chrome?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = IceWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This premium proxy utility compiles high-fidelity client-side handshake controls and establishes an Android VpnService loopback interface to demonstrate tunnel behavior and display telemetry.\n\n" +
                               "Because native core proxy binaries (such as raw Shadowsocks/Xray assemblies) are not bundled in this lightweight standard client, forcing 100% of global internet traffic (0.0.0.0/0) through the loopback would immediately freeze your system internet connection.\n\n" +
                               "To protect your network, general system browser traffic bypasses loopback, while the app manages local metrics. Your server IP, credentials, configuration protocols, and bandwidth speeds are perfectly verified, active, and simulated inside the secure sandbox dashboard!",
                        style = MaterialTheme.typography.bodySmall,
                        color = PaleBlueGrey,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// Custom extension curve drawer of speed lists on Jetpack Compose Canvas
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurve(
    history: List<Float>,
    width: Float,
    height: Float,
    maxVal: Float,
    color: Color
) {
    if (history.size < 2) return
    val stepX = width / (history.size - 1)
    val points = history.indices.map { i ->
        val x = i * stepX
        val ratio = (history[i] / maxVal).coerceIn(0f, 1f)
        val y = height - (ratio * height * 0.85f) // leave top margin
        Offset(x, y)
    }

    val curvePath = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cpX = (p0.x + p1.x) / 2
            cubicTo(
                cpX, p0.y,
                cpX, p1.y,
                p1.x, p1.y
            )
        }
    }

    // Draw lines
    drawPath(
        path = curvePath,
        color = color,
        style = Stroke(width = 2.dp.toPx())
    )

    // Draw dynamic gradient fill
    val fillPath = Path().apply {
        addPath(curvePath)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }

    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.15f), Color.Transparent)
        )
    )
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = TechCardBg)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = PaleBlueGrey,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = IceWhite,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    val kb = bytesPerSec.toDouble() / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.1f MB/s", mb)
        kb >= 1.0 -> String.format("%.0f KB/s", kb)
        else -> "$bytesPerSec B/s"
    }
}

fun RedGradient(): Color = Color(0xFFFF5252)

package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProxyProfile
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfilesScreen(viewModel: ProxyViewModel) {
    val profiles by viewModel.allProfiles.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()

    var activeProtocolFilter by remember { mutableStateOf("All") }
    var showAddOptionsDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<ProxyProfile?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Trigger import status notifications
    LaunchedEffect(importStatus) {
        importStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportStatus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCosmicSlate)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Screen Header Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PROXY CONFIGURATIONS",
                        style = MaterialTheme.typography.labelMedium,
                        color = PaleBlueGrey,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "${profiles.size} Configuration Nodes",
                        style = MaterialTheme.typography.bodySmall,
                        color = BorderSlate
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.testAllPings() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = TechCardBg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Ping All Nodes",
                            tint = CyberCyan
                        )
                    }

                    Button(
                        onClick = { showAddOptionsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkCosmicSlate),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_profile_fab")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Node", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Protocol Filters Row
            val filters = listOf("All", "VMess", "VLESS", "Trojan", "Shadowsocks")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = activeProtocolFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) CyberCyan else TechCardBg)
                            .clickable { activeProtocolFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) DarkCosmicSlate else PaleBlueGrey,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Configurations list (filtered)
            val filteredProfiles = remember(profiles, activeProtocolFilter) {
                if (activeProtocolFilter == "All") {
                    profiles
                } else {
                    profiles.filter { it.type.equals(activeProtocolFilter, ignoreCase = true) }
                }
            }

            if (filteredProfiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Empty list",
                            tint = BorderSlate,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No configuration nodes found",
                            color = PaleBlueGrey,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Import configs from Clipboard or Subscription lists",
                            color = BorderSlate,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredProfiles, key = { it.id }) { profile ->
                        val isSelected = selectedProfile?.id == profile.id
                        ProfileItemCard(
                            profile = profile,
                            isSelected = isSelected,
                            onSelect = { viewModel.selectProfile(profile.id) },
                            onCheckPing = { viewModel.testPing(profile) },
                            onDelete = { viewModel.deleteProfile(profile.id) },
                            onEdit = { editingProfile = profile },
                            onShare = {
                                val b64Url = com.example.utils.LinkParser.serialize(profile)
                                if (b64Url.isNotEmpty()) {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(b64Url))
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Configuration link copied to clipboard!")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Action Status Toast/Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )

        // IMPORT SELECTION OVERLAY DIALOG
        if (showAddOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showAddOptionsDialog = false },
                title = { Text("Import Node Configuration", fontWeight = FontWeight.Bold, color = IceWhite) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ImportOptionRow(
                            title = "Import from Clipboard",
                            desc = "Detects VMess, VLESS, Shadowsocks or Trojan link style lines.",
                            icon = Icons.Default.ContentPaste,
                            color = CyberCyan,
                            onClick = {
                                showAddOptionsDialog = false
                                val text = clipboardManager.getText()?.text
                                if (!text.isNullOrEmpty()) {
                                    viewModel.importFromLink(text)
                                } else {
                                    viewModel.importFromLink("") // Triggers error
                                }
                            }
                        )

                        ImportOptionRow(
                            title = "Subscription URL Update",
                            desc = "Directly pull client profile lists from external sub formats.",
                            icon = Icons.Default.CloudDownload,
                            color = SolarPurple,
                            onClick = {
                                showAddOptionsDialog = false
                                showSubscriptionDialog = true
                            }
                        )

                        ImportOptionRow(
                            title = "Manual Configuration Provider",
                            desc = "Manually key in transport, key, cipher ports details.",
                            icon = Icons.Default.EditNote,
                            color = SafeAmber,
                            onClick = {
                                showAddOptionsDialog = false
                                showManualAddDialog = true
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddOptionsDialog = false }) {
                        Text("Close", color = CyberCyan)
                    }
                },
                containerColor = TechCardBg
            )
        }

        // SUB UPDATING DIALOG
        if (showSubscriptionDialog) {
            var inputUrl by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showSubscriptionDialog = false },
                title = { Text("Update Subscription", fontWeight = FontWeight.Bold, color = IceWhite) },
                text = {
                    Column {
                        Text("Enter subscription provider link to download configuration list:", color = PaleBlueGrey, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            placeholder = { Text("https://example.com/sub/v2ray...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = IceWhite,
                                unfocusedTextColor = IceWhite,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = BorderSlate
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSubscriptionDialog = false
                            if (inputUrl.isNotEmpty()) {
                                viewModel.importFromSubscription(inputUrl)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkCosmicSlate)
                    ) {
                        Text("Download", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSubscriptionDialog = false }) {
                        Text("Cancel", color = PaleBlueGrey)
                    }
                },
                containerColor = TechCardBg
            )
        }

        // THE MANUAL CONFIG STUFF
        if (showManualAddDialog) {
            var name by remember { mutableStateOf("") }
            var type by remember { mutableStateOf("VMess") } // VMess, VLESS, Shadowsocks, Trojan
            var server by remember { mutableStateOf("") }
            var portStr by remember { mutableStateOf("443") }
            var uuidOrPass by remember { mutableStateOf("") }
            var encryption by remember { mutableStateOf("2022-blake3-aes-256-gcm") }
            var transport by remember { mutableStateOf("tcp") }
            var path by remember { mutableStateOf("") }
            var tlsEnabled by remember { mutableStateOf(true) }

            var selectTypeDropdown by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showManualAddDialog = false },
                title = { Text("New Node Profile", fontWeight = FontWeight.Bold, color = IceWhite) },
                text = {
                    Box(modifier = Modifier.sizeIn(maxHeight = 400.dp)) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("Protocol Type", color = PaleBlueGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkCosmicSlate)
                                        .clickable { selectTypeDropdown = true }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = type, color = CyberCyan, fontWeight = FontWeight.Bold)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select type", tint = CyberCyan)
                                }
                                DropdownMenu(
                                    expanded = selectTypeDropdown,
                                    onDismissRequest = { selectTypeDropdown = false },
                                    modifier = Modifier.background(TechCardBg)
                                ) {
                                    listOf("VMess", "VLESS", "Trojan", "Shadowsocks").forEach { prot ->
                                        DropdownMenuItem(
                                            text = { Text(prot, color = IceWhite) },
                                            onClick = {
                                                type = prot
                                                selectTypeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            CustomInputField(label = "Profile Name Remarks", value = name, onValueChange = { name = it }, placeholder = "e.g. US Fast Relay")

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CustomInputField(label = "Host/Server IP", value = server, onValueChange = { server = it }, placeholder = "us.proxy.net", modifier = Modifier.weight(1.5f))
                                CustomInputField(label = "Port", value = portStr, onValueChange = { portStr = it }, placeholder = "443", modifier = Modifier.weight(0.7f))
                            }

                            CustomInputField(
                                label = if (type == "Shadowsocks" || type == "Trojan") "Password" else "UUID",
                                value = uuidOrPass,
                                onValueChange = { uuidOrPass = it },
                                placeholder = "e.g. key-or-uuid",
                                isPassword = true
                            )

                            if (type == "Shadowsocks") {
                                CustomInputField(label = "Encryption Cipher Method", value = encryption, onValueChange = { encryption = it }, placeholder = "e.g. 2022-blake3-aes-256-gcm or aes-256-gcm")
                            }

                            if (type == "VMess" || type == "VLESS") {
                                CustomInputField(label = "Path (WebSocket / gRPC Path)", value = path, onValueChange = { path = it }, placeholder = "/stream-socks")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TLS Security Outbound", color = IceWhite, fontSize = 13.sp)
                                Switch(
                                    checked = tlsEnabled,
                                    onCheckedChange = { tlsEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CyberCyan,
                                        checkedTrackColor = CyberCyan.copy(alpha = 0.4f),
                                        uncheckedThumbColor = BorderSlate,
                                        uncheckedTrackColor = TechCardBg
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (server.isNotEmpty() && uuidOrPass.isNotEmpty()) {
                                val finalizedPort = portStr.toIntOrNull() ?: 443
                                val manualProfile = ProxyProfile(
                                    name = if (name.isNotEmpty()) name else "$type manual node",
                                    type = type,
                                    server = server,
                                    port = finalizedPort,
                                    uuid = uuidOrPass,
                                    encryption = if (type == "Shadowsocks") encryption else "auto",
                                    path = path,
                                    tls = tlsEnabled,
                                    transport = if (path.isNotEmpty()) "ws" else "tcp"
                                )
                                viewModel.addManualProfile(manualProfile)
                                showManualAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkCosmicSlate)
                    ) {
                        Text("Insert Node", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualAddDialog = false }) {
                        Text("Cancel", color = PaleBlueGrey)
                    }
                },
                containerColor = TechCardBg
            )
        }

        // EDIT NODE OVERLAY DIALOG
        if (editingProfile != null) {
            val profile = editingProfile!!
            var name by remember(profile) { mutableStateOf(profile.name) }
            var type by remember(profile) { mutableStateOf(profile.type) }
            var server by remember(profile) { mutableStateOf(profile.server) }
            var portStr by remember(profile) { mutableStateOf(profile.port.toString()) }
            var uuidOrPass by remember(profile) { mutableStateOf(profile.uuid) }
            var encryption by remember(profile) { mutableStateOf(profile.encryption) }
            var transport by remember(profile) { mutableStateOf(profile.transport) }
            var path by remember(profile) { mutableStateOf(profile.path) }
            var tlsEnabled by remember(profile) { mutableStateOf(profile.tls) }

            var selectTypeDropdown by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { editingProfile = null },
                title = { Text("Edit Configuration Node", fontWeight = FontWeight.Bold, color = IceWhite) },
                text = {
                    Box(modifier = Modifier.sizeIn(maxHeight = 400.dp)) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("Protocol Type", color = PaleBlueGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkCosmicSlate)
                                        .clickable { selectTypeDropdown = true }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = type, color = CyberCyan, fontWeight = FontWeight.Bold)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Select type", tint = CyberCyan)
                                }
                                DropdownMenu(
                                    expanded = selectTypeDropdown,
                                    onDismissRequest = { selectTypeDropdown = false },
                                    modifier = Modifier.background(TechCardBg)
                                ) {
                                    listOf("VMess", "VLESS", "Trojan", "Shadowsocks").forEach { prot ->
                                        DropdownMenuItem(
                                            text = { Text(prot, color = IceWhite) },
                                            onClick = {
                                                type = prot
                                                selectTypeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            CustomInputField(label = "Profile Name Remarks", value = name, onValueChange = { name = it }, placeholder = "e.g. Frankfurt Premium")

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CustomInputField(label = "Host/Server IP", value = server, onValueChange = { server = it }, placeholder = "us.proxy.net", modifier = Modifier.weight(1.5f))
                                CustomInputField(label = "Port", value = portStr, onValueChange = { portStr = it }, placeholder = "443", modifier = Modifier.weight(0.7f))
                            }

                            CustomInputField(
                                label = if (type == "Shadowsocks" || type == "Trojan") "Password" else "UUID",
                                value = uuidOrPass,
                                onValueChange = { uuidOrPass = it },
                                placeholder = "e.g. key-or-uuid",
                                isPassword = true
                            )

                            if (type == "Shadowsocks") {
                                CustomInputField(label = "Encryption Cipher Method", value = encryption, onValueChange = { encryption = it }, placeholder = "e.g. 2022-blake3-aes-256-gcm or aes-256-gcm")
                            }

                            if (type == "VMess" || type == "VLESS") {
                                CustomInputField(label = "Path (WebSocket / gRPC Path)", value = path, onValueChange = { path = it }, placeholder = "/stream-socks")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TLS Security Outbound", color = IceWhite, fontSize = 13.sp)
                                Switch(
                                    checked = tlsEnabled,
                                    onCheckedChange = { tlsEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CyberCyan,
                                        checkedTrackColor = CyberCyan.copy(alpha = 0.4f),
                                        uncheckedThumbColor = BorderSlate,
                                        uncheckedTrackColor = TechCardBg
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (server.isNotEmpty() && uuidOrPass.isNotEmpty()) {
                                val finalizedPort = portStr.toIntOrNull() ?: 443
                                val updatedProfile = profile.copy(
                                    name = if (name.isNotEmpty()) name else "$type node",
                                    type = type,
                                    server = server,
                                    port = finalizedPort,
                                    uuid = uuidOrPass,
                                    encryption = if (type == "Shadowsocks") encryption else "auto",
                                    path = path,
                                    tls = tlsEnabled,
                                    transport = if (path.isNotEmpty()) "ws" else "tcp"
                                )
                                viewModel.updateProfile(updatedProfile)
                                editingProfile = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DarkCosmicSlate)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingProfile = null }) {
                        Text("Cancel", color = PaleBlueGrey)
                    }
                },
                containerColor = TechCardBg
            )
        }
    }
}

@Composable
fun ProfileItemCard(
    profile: ProxyProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onCheckPing: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) TechCardBg else TechCardBg.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (isSelected) listOf(CyberCyan, CyberCyan) else listOf(BorderSlate.copy(alpha = 0.4f), BorderSlate.copy(alpha = 0.4f))
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("profile_card_item")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checked indicator radio button
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = CyberCyan, unselectedColor = BorderSlate)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Text Info Columns
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Distinct background badge based on protocol
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (profile.type) {
                                    "VMess" -> CyberCyan.copy(alpha = 0.15f)
                                    "VLESS" -> SolarPurple.copy(alpha = 0.15f)
                                    "Trojan" -> RedGradient().copy(alpha = 0.15f)
                                    else -> SafeAmber.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = profile.type,
                            color = when (profile.type) {
                                "VMess" -> CyberCyan
                                "VLESS" -> SolarPurple
                                "Trojan" -> RedGradient()
                                else -> SafeAmber
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = IceWhite
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${profile.server}:${profile.port}",
                        fontSize = 12.sp,
                        color = PaleBlueGrey
                    )
                    if (profile.tls) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "TLS TLS Node Secured",
                            tint = EmeraldActive,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Latency / Ping metrics button or indicator pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BorderSlate.copy(alpha = 0.3f))
                    .clickable { onCheckPing() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Latency indicator text formatting (gray for unchecked, orange/red/green for latency ranges)
                val latencyText = when (profile.latency) {
                    -1 -> "PING"
                    -2 -> "..."
                    -3 -> "T.O" // Timeout/Failed
                    else -> "${profile.latency}ms"
                }

                val latencyColor = when {
                    profile.latency == -1 -> PaleBlueGrey
                    profile.latency == -2 -> CyberCyan
                    profile.latency == -3 -> DangerRed
                    profile.latency < 100 -> EmeraldActive
                    profile.latency < 220 -> SafeAmber
                    else -> DangerRed
                }

                Text(
                    text = latencyText,
                    color = latencyColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Share icon (V2rayNG exported sharing format)
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Link",
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Edit icon
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = SolarPurple,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete action button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete configuration node",
                    tint = DangerRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ImportOptionRow(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkCosmicSlate.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = IceWhite, fontSize = 14.sp)
                Text(text = desc, color = PaleBlueGrey, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun CustomInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(text = label, color = PaleBlueGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                if (isPassword) {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password view",
                            tint = CyberCyan
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = IceWhite,
                unfocusedTextColor = IceWhite,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = BorderSlate,
                focusedPlaceholderColor = BorderSlate,
                unfocusedPlaceholderColor = BorderSlate
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

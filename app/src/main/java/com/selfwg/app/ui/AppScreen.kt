package com.selfwg.app.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.selfwg.app.data.Prefs
import com.selfwg.app.data.TunnelEntry
import com.selfwg.app.data.TunnelStore
import com.selfwg.app.vpn.TunnelManager
import com.wireguard.android.backend.Tunnel
import java.io.ByteArrayOutputStream
import java.io.InputStream

private const val MAX_CONFIG_BYTES = 16 * 1024

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSwitch: () -> Unit
) {
    val context = LocalContext.current
    val s = remember { I18n.get(context) }

    val state by TunnelManager.state.collectAsStateWithLifecycle()
    val serverIp by TunnelManager.serverIp.collectAsStateWithLifecycle()

    var tunnels by remember { mutableStateOf(TunnelStore.list(context)) }
    var activeId by remember { mutableStateOf(TunnelStore.activeId(context)) }
    var desiredOn by remember { mutableStateOf(Prefs.isIntendedUp(context)) }

    var showImport by remember { mutableStateOf(false) }
    var showPaste by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var pendingConf by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<TunnelEntry?>(null) }
    var deleting by remember { mutableStateOf<TunnelEntry?>(null) }

    fun refresh() {
        tunnels = TunnelStore.list(context)
        activeId = TunnelStore.activeId(context)
    }

    fun importText(text: String) {
        if (text.length > MAX_CONFIG_BYTES) {
            Toast.makeText(context, s.tConfigTooBig, Toast.LENGTH_LONG).show()
            return
        }
        runCatching { TunnelStore.parse(text) }
            .onSuccess {
                showImport = false
                showPaste = false
                pendingConf = text.trim()
            }
            .onFailure { Toast.makeText(context, s.tInvalidConfig, Toast.LENGTH_LONG).show() }
    }

    val activeTunnel = tunnels.firstOrNull { it.id == activeId } ?: tunnels.firstOrNull()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { importText(it) }
    }
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)!!.use { readCapped(it, MAX_CONFIG_BYTES) }
            }.onSuccess { bytes ->
                if (bytes == null) Toast.makeText(context, s.tFileTooBig, Toast.LENGTH_LONG).show()
                else importText(bytes.decodeToString())
            }.onFailure {
                Toast.makeText(context, s.tFileError, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { SelfWgWordmark(22.sp) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = s.settings)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(s, state, desiredOn, serverIp, activeTunnel?.name)

            if (activeTunnel != null) {
                ConnectRow(
                    s = s,
                    name = activeTunnel.name,
                    desiredOn = desiredOn,
                    onToggle = { on ->
                        desiredOn = on
                        if (on) onConnect() else onDisconnect()
                    }
                )
            }

            Text(
                s.tunnelsSection,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (tunnels.isEmpty()) {
                Text(s.noTunnelHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            } else {
                tunnels.forEach { t ->
                    TunnelRow(
                        s = s,
                        entry = t,
                        isActive = t.id == (activeTunnel?.id),
                        onSelect = {
                            if (t.id != activeId) {
                                TunnelStore.setActive(context, t.id)
                                activeId = t.id
                                if (desiredOn) onSwitch()
                            }
                        },
                        onEdit = { editing = t },
                        onDelete = { deleting = t }
                    )
                }
            }

            OutlinedButton(onClick = { showImport = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(s.addTunnel)
            }
        }
    }

    if (showImport) {
        ModalBottomSheet(onDismissRequest = { showImport = false }) {
            Column(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(s.addTunnelTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                ImportOption(Icons.Filled.QrCodeScanner, s.scanQr, s.scanQrSub) {
                    val opts = ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt(s.scanQr)
                        setBeepEnabled(false)
                        setOrientationLocked(false)
                    }
                    scanLauncher.launch(opts)
                }
                ImportOption(Icons.Filled.FileOpen, s.chooseFile, s.chooseFileSub) {
                    fileLauncher.launch("*/*")
                }
                ImportOption(Icons.Filled.ContentPaste, s.pasteText, s.pasteTextSub) {
                    showImport = false
                    showPaste = true
                }
            }
        }
    }

    if (showPaste) {
        var pasteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPaste = false },
            title = { Text(s.pasteTitle) },
            text = {
                OutlinedTextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    placeholder = { Text("[Interface]\n...") },
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            },
            confirmButton = { TextButton(onClick = { importText(pasteText) }) { Text(s.next) } },
            dismissButton = { TextButton(onClick = { showPaste = false }) { Text(s.cancel) } }
        )
    }

    if (pendingConf != null) {
        var name by remember { mutableStateOf("Tunnel ${tunnels.size + 1}") }
        AlertDialog(
            onDismissRequest = { pendingConf = null },
            title = { Text(s.nameTitle) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    TunnelStore.add(context, name, pendingConf!!)
                    pendingConf = null
                    refresh()
                    Toast.makeText(context, s.tAdded, Toast.LENGTH_SHORT).show()
                }) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { pendingConf = null }) { Text(s.cancel) } }
        )
    }

    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(s.deleteTitle) },
            text = { Text("\"${entry.name}\" ${s.deleteMsg}") },
            confirmButton = {
                TextButton(onClick = {
                    val wasActive = entry.id == activeId
                    TunnelStore.delete(context, entry.id)
                    deleting = null
                    refresh()
                    if (wasActive) {
                        if (tunnels.isEmpty()) {
                            desiredOn = false
                            Prefs.setIntendedUp(context, false)
                            onDisconnect()
                        } else if (desiredOn) {
                            onSwitch()
                        }
                    }
                    Toast.makeText(context, s.tDeleted, Toast.LENGTH_SHORT).show()
                }) { Text(s.delete, color = Color(0xFFE5746B)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(s.cancel) } }
        )
    }

    if (showSettings) {
        var autoBoot by remember { mutableStateOf(Prefs.autoConnectOnBoot(context)) }
        var biometric by remember { mutableStateOf(Prefs.biometricEnabled(context)) }
        var lang by remember { mutableStateOf(Prefs.language(context)) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(s.settings) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.languageTitle, modifier = Modifier.weight(1f))
                        FilterChip(selected = lang == "de", onClick = {
                            lang = "de"; Prefs.setLanguage(context, "de"); (context as? Activity)?.recreate()
                        }, label = { Text("DE") })
                        Spacer(Modifier.size(8.dp))
                        FilterChip(selected = lang == "en", onClick = {
                            lang = "en"; Prefs.setLanguage(context, "en"); (context as? Activity)?.recreate()
                        }, label = { Text("EN") })
                    }
                    SettingSwitch(s.biometricTitle, s.biometricSub, biometric) {
                        biometric = it; Prefs.setBiometricEnabled(context, it)
                    }
                    SettingSwitch(s.autoBootTitle, s.autoBootSub, autoBoot) {
                        autoBoot = it; Prefs.setAutoConnectOnBoot(context, it)
                    }
                    Text(
                        s.watchdogInfo,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text(s.done) } }
        )
    }

    // Vollbild-Editor (überlagert alles)
    editing?.let { entry ->
        EditTunnelScreen(
            entry = entry,
            strings = s,
            onSave = { name, conf, appMode, apps ->
                TunnelStore.update(context, entry.id, name, conf, appMode, apps)
                val wasActive = entry.id == activeId
                editing = null
                refresh()
                if (wasActive && desiredOn) onSwitch()
                Toast.makeText(context, s.tSaved, Toast.LENGTH_SHORT).show()
            },
            onCancel = { editing = null }
        )
    }
}

@Composable
private fun StatusCard(s: Strings, state: Tunnel.State, desiredOn: Boolean, serverIp: String?, activeName: String?) {
    val connected = state == Tunnel.State.UP
    val statusText = when {
        connected -> s.connected
        desiredOn -> s.connecting
        else -> s.disconnected
    }
    val dot = when {
        connected -> Color(0xFF35D07F)
        desiredOn -> Color(0xFFE7B549)
        else -> Color(0xFF6B7B78)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(12.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.size(10.dp))
                Text(statusText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text("${s.tunnelLabel}: ${activeName ?: "—"}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${s.serverLabel}: ${serverIp ?: "—"}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConnectRow(s: Strings, name: String, desiredOn: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (desiredOn) s.tunnelActive else s.tunnelOff,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = desiredOn, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun TunnelRow(
    s: Strings,
    entry: TunnelEntry,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isActive, onClick = onSelect)
            Text(
                entry.name,
                fontSize = 16.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = s.editTitle) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = s.delete) }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ImportOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun readCapped(stream: InputStream, max: Int): ByteArray? {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(4096)
    var total = 0
    while (true) {
        val n = stream.read(buf)
        if (n < 0) break
        total += n
        if (total > max) return null
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}

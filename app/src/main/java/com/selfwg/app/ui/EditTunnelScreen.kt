package com.selfwg.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfwg.app.data.TunnelEntry
import com.selfwg.app.data.TunnelStore
import com.selfwg.app.data.WgFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTunnelScreen(
    entry: TunnelEntry,
    strings: Strings,
    onSave: (name: String, conf: String, appMode: String, apps: List<String>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val s = strings
    var name by remember(entry.id) { mutableStateOf(entry.name) }
    var f by remember(entry.id) { mutableStateOf(WgFields.parse(entry.conf)) }
    var appMode by remember(entry.id) { mutableStateOf(entry.appMode) }
    var apps by remember(entry.id) { mutableStateOf(entry.apps) }
    var showPicker by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = s.cancel) }
                Text(s.editTitle, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val conf = WgFields.build(f)
                    runCatching { TunnelStore.parse(conf) }
                        .onSuccess { onSave(name, conf, appMode, apps) }
                        .onFailure { Toast.makeText(context, s.tInvalidConfig, Toast.LENGTH_LONG).show() }
                }) { Text(s.save) }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Field(s.fieldName, "", name, singleLine = true) { name = it }

                SectionLabel("[Interface]")
                Field(s.lPrivateKey, s.dPrivateKey, f.privateKey, singleLine = true, secret = true) { f = f.copy(privateKey = it) }
                Field(s.lAddress, s.dAddress, f.address, singleLine = true) { f = f.copy(address = it) }
                Field(s.lDns, s.dDns, f.dns, singleLine = true) { f = f.copy(dns = it) }
                Field(s.lMtu, s.dMtu, f.mtu, singleLine = true) { f = f.copy(mtu = it) }

                SectionLabel("[Peer]")
                Field(s.lPublicKey, s.dPublicKey, f.publicKey, singleLine = true) { f = f.copy(publicKey = it) }
                Field(s.lPresharedKey, s.dPresharedKey, f.presharedKey, singleLine = true, secret = true) { f = f.copy(presharedKey = it) }
                Field(s.lEndpoint, s.dEndpoint, f.endpoint, singleLine = true) { f = f.copy(endpoint = it) }
                Field(s.lAllowedIps, s.dAllowedIps, f.allowedIps, singleLine = true) { f = f.copy(allowedIps = it) }
                Field(s.lKeepalive, s.dKeepalive, f.keepalive, singleLine = true) { f = f.copy(keepalive = it) }

                Spacer(Modifier.size(4.dp))
                Text(s.appsSection, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(s.appsInfo, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = appMode == "all", onClick = { appMode = "all" }, label = { Text(s.appModeAll) })
                    FilterChip(selected = appMode == "include", onClick = { appMode = "include" }, label = { Text(s.appModeInclude) })
                    FilterChip(selected = appMode == "exclude", onClick = { appMode = "exclude" }, label = { Text(s.appModeExclude) })
                }
                if (appMode != "all") {
                    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${s.chooseApps} (${apps.size} ${s.appsSelected})")
                    }
                }
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            strings = s,
            selected = apps,
            onConfirm = { apps = it; showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = SelfTeal,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun Field(
    label: String,
    desc: String,
    value: String,
    singleLine: Boolean,
    secret: Boolean = false,
    onChange: (String) -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        visualTransformation = if (secret && !revealed) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = if (secret) {
            KeyboardOptions(keyboardType = KeyboardType.Password)
        } else {
            KeyboardOptions.Default
        },
        trailingIcon = if (!secret) null else {
            {
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(
                        imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            }
        },
        supportingText = if (desc.isBlank()) null else {
            { Text(desc, fontSize = 11.sp) }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

package com.selfwg.app

import android.Manifest
import android.app.Activity
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.selfwg.app.data.Prefs
import com.selfwg.app.data.TunnelStore
import com.selfwg.app.ui.AppScreen
import com.selfwg.app.ui.LockScreen
import com.selfwg.app.ui.theme.SelfWgTheme
import com.selfwg.app.vpn.SelfWgService

class MainActivity : FragmentActivity() {

    private var onVpnGranted: (() -> Unit)? = null
    private val authed = mutableStateOf(false)
    private var prompting = false

    private val vpnPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) onVpnGranted?.invoke()
        onVpnGranted = null
    }

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Service läuft auch ohne sichtbare Notification weiter. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            SelfWgTheme {
                val locked = Prefs.biometricEnabled(this) && !authed.value
                if (locked) {
                    LockScreen(onUnlock = { promptBiometric() })
                } else {
                    AppScreen(
                        onConnect = { connect() },
                        onDisconnect = { SelfWgService.stop(this) },
                        onSwitch = { switchTunnelIfRunning() }
                    )
                }
            }
        }
        if (Prefs.biometricEnabled(this)) promptBiometric()
    }

    // Beim Verlassen sperren, bei echter Rückkehr neu entsperren (nicht beim
    // System-Biometrie-Dialog, der löst kein onRestart aus).
    override fun onStop() {
        super.onStop()
        if (Prefs.biometricEnabled(this)) authed.value = false
    }

    override fun onRestart() {
        super.onRestart()
        if (Prefs.biometricEnabled(this) && !authed.value) promptBiometric()
    }

    private fun connect() {
        if (!TunnelStore.hasAny(this)) return
        withVpnPermission { SelfWgService.start(this) }
    }

    private fun switchTunnelIfRunning() {
        if (Prefs.isIntendedUp(this)) withVpnPermission { SelfWgService.switchTunnel(this) }
    }

    private fun withVpnPermission(action: () -> Unit) {
        val prepare = VpnService.prepare(this)
        if (prepare != null) {
            onVpnGranted = action
            vpnPermLauncher.launch(prepare)
        } else {
            action()
        }
    }

    private fun promptBiometric() {
        if (prompting) return
        val auths = allowedAuthenticators()
        if (BiometricManager.from(this).canAuthenticate(auths) != BiometricManager.BIOMETRIC_SUCCESS) {
            // Nichts eingerichtet -> Nutzer nicht aussperren.
            authed.value = true
            return
        }
        prompting = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    prompting = false
                    authed.value = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    prompting = false
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("SelfWG entsperren")
            .setSubtitle("Mit Fingerabdruck oder Gerätesperre bestätigen")
            .setAllowedAuthenticators(auths)
            .build()
        prompt.authenticate(info)
    }

    private fun allowedAuthenticators(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
}

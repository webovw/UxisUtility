package com.uxis.utility

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.uxis.utility.core.AppStore
import com.uxis.utility.core.UxisVpnService
import com.uxis.utility.core.VpnBus
import com.uxis.utility.core.VpnState
import com.uxis.utility.ui.UxisApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = AppStore(applicationContext)

        setContent {
            val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) startVpn()
            }
            val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            UxisApp(store) {
                when (VpnBus.state.value) {
                    VpnState.Connected, VpnState.Connecting -> stopVpn()
                    VpnState.Stopping -> Unit
                    else -> {
                        if (store.selectedServer() == null) {
                            Toast.makeText(this, "Сначала добавьте сервер", Toast.LENGTH_SHORT).show()
                        } else {
                            val prepare = VpnService.prepare(this)
                            if (prepare != null) vpnLauncher.launch(prepare) else startVpn()
                        }
                    }
                }
            }
        }
    }

    private fun startVpn() {
        ContextCompat.startForegroundService(
            this, Intent(this, UxisVpnService::class.java).setAction(UxisVpnService.ACTION_START)
        )
    }

    private fun stopVpn() {
        startService(Intent(this, UxisVpnService::class.java).setAction(UxisVpnService.ACTION_STOP))
    }
}

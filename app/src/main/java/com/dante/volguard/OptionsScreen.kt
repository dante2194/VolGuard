package com.dante.volguard

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private fun Context.tryStart(vararg intents: Intent) {
    for (i in intents) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {
        }
    }
}

@Composable
fun OptionsScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val activity = ctx as ComponentActivity
    val cs = MaterialTheme.colorScheme

    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(activity) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) tick++
        }
        activity.lifecycle.addObserver(obs)
        onDispose { activity.lifecycle.removeObserver(obs) }
    }

    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    val dndOk = remember(tick) { nm.isNotificationPolicyAccessGranted }
    val batteryOk = remember(tick) { pm.isIgnoringBatteryOptimizations(ctx.packageName) }
    val notifOk = remember(tick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            "Options",
            fontSize = 34.sp,
            fontWeight = FontWeight.Medium,
            color = cs.onBackground,
        )
        Text(
            "Make the locks survive on Xiaomi / HyperOS",
            style = MaterialTheme.typography.bodyLarge,
            color = cs.onSurfaceVariant,
        )

        SettingRow(
            title = "Do Not Disturb access",
            desc = "Needed to set Ringtone / Notification to 0 or change them while in silent mode.",
            ok = dndOk,
            button = "Grant",
        ) {
            ctx.tryStart(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        HorizontalDivider(color = cs.surfaceVariant)

        SettingRow(
            title = "No battery restrictions",
            desc = "Set battery saver to \"No restrictions\" so MIUI doesn't kill the guard.",
            ok = batteryOk,
            button = "Allow",
        ) {
            ctx.tryStart(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${ctx.packageName}")
                ),
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            )
        }
        HorizontalDivider(color = cs.surfaceVariant)

        SettingRow(
            title = "Autostart",
            desc = "Enable autostart for VolGuard so locks come back after a reboot.",
            ok = null,
            button = "Open",
        ) {
            ctx.tryStart(
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                ),
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${ctx.packageName}")
                ),
            )
        }
        HorizontalDivider(color = cs.surfaceVariant)

        if (!notifOk) {
            SettingRow(
                title = "Notifications",
                desc = "Required for the guard's status notification.",
                ok = false,
                button = "Open",
            ) {
                ctx.tryStart(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                )
            }
            HorizontalDivider(color = cs.surfaceVariant)
        }

        Spacer(Modifier.size(20.dp))
        Text("How locking works", style = MaterialTheme.typography.titleMedium, color = cs.onBackground)
        Spacer(Modifier.size(6.dp))
        Text(
            "Tap the lock to freeze a channel at its current level. " +
                "With both chips off, the level is fixed. " +
                "\"Allow lowering\" lets the volume go below the locked level; " +
                "\"Allow raising\" lets it go above. " +
                "Moving a slider while locked sets a new locked level.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            "Tip: lock VolGuard in the Recents screen so it is never swiped away.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    desc: String,
    ok: Boolean?,
    button: String,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onBackground)
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        if (ok == true) {
            Icon(Icons.Rounded.Check, contentDescription = "OK", tint = cs.primary)
        } else {
            FilledTonalButton(onClick = onClick) { Text(button) }
        }
    }
}

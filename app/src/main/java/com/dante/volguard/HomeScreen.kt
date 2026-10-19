package com.dante.volguard

import android.content.Context
import android.media.AudioManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class ChannelState(
    private val ctx: Context,
    private val am: AudioManager,
    val ch: Channel,
) {
    val min = am.minVol(ch)
    val max = am.maxVol(ch)

    var current by mutableIntStateOf(am.getStreamVolume(ch.stream))
    var locked by mutableStateOf(Prefs.locked(ctx, ch))
    var target by mutableIntStateOf(Prefs.target(ctx, ch, current))
    var allowLower by mutableStateOf(Prefs.allowLower(ctx, ch))
    var allowRaise by mutableStateOf(Prefs.allowRaise(ctx, ch))

    fun refresh() {
        current = am.getStreamVolume(ch.stream)
    }

    fun setVolume(v: Int) {
        am.setSafe(ch, v)
        current = am.getStreamVolume(ch.stream)
        if (locked) {
            target = v.coerceIn(min, max)
            Prefs.setTarget(ctx, ch, target)
        }
    }

    /** Locking captures the current level as the locked level. */
    fun applyLock(on: Boolean) {
        locked = on
        if (on) {
            current = am.getStreamVolume(ch.stream)
            target = current
            Prefs.setTarget(ctx, ch, target)
        }
        Prefs.setLocked(ctx, ch, on)
    }

    fun toggleLock() {
        applyLock(!locked)
        GuardService.sync(ctx)
    }

    fun setLower(v: Boolean) {
        allowLower = v
        Prefs.setAllowLower(ctx, ch, v)
    }

    fun setRaise(v: Boolean) {
        allowRaise = v
        Prefs.setAllowRaise(ctx, ch, v)
    }
}

private fun Channel.icon(): ImageVector = when (this) {
    Channel.MEDIA -> Icons.Rounded.MusicNote
    Channel.CALL -> Icons.Rounded.Call
    Channel.RING -> Icons.Rounded.RingVolume
    Channel.NOTIF -> Icons.Rounded.NotificationsActive
    Channel.ALARM -> Icons.Rounded.Alarm
}

@Composable
fun HomeScreen(padding: PaddingValues) {
    val ctx = LocalContext.current
    val activity = ctx as ComponentActivity
    val app = ctx.applicationContext
    val am = remember { app.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val states = remember { Channel.entries.map { ChannelState(app, am, it) } }

    // Live-refresh while the app is visible (hardware volume keys etc.)
    LaunchedEffect(Unit) {
        activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                states.forEach { it.refresh() }
                delay(300)
            }
        }
    }

    val lockedCount = states.count { it.locked }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                Text(
                    "VolGuard",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "$lockedCount of ${states.size} locked",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = {
                            states.forEach { if (!it.locked) it.applyLock(true) }
                            GuardService.sync(app)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) {
                        Icon(Icons.Rounded.Lock, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Lock all")
                    }
                    OutlinedButton(
                        onClick = {
                            states.forEach { it.applyLock(false) }
                            GuardService.sync(app)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) {
                        Icon(Icons.Rounded.LockOpen, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock all")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        items(states) { ChannelCard(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelCard(s: ChannelState) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(s.ch.tileBg)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    s.ch.icon(), null,
                    tint = Color(s.ch.tileFg),
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    s.ch.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = cs.onBackground,
                )
                Text(
                    "${s.current} / ${s.max}",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurfaceVariant,
                )
            }
            IconButton(onClick = { s.toggleLock() }) {
                Icon(
                    if (s.locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = if (s.locked) "Unlock" else "Lock",
                    tint = if (s.locked) cs.primary else cs.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        val range = s.min.toFloat()..s.max.toFloat()
        Slider(
            value = s.current.toFloat().coerceIn(range),
            onValueChange = { s.setVolume(it.roundToInt()) },
            valueRange = range,
            steps = (s.max - s.min - 1).coerceAtLeast(0),
            thumb = {
                Box(
                    Modifier
                        .width(5.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(cs.onSurfaceVariant)
                )
            },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = s.allowLower,
                onClick = { s.setLower(!s.allowLower) },
                label = { Text("Allow lowering") },
                leadingIcon = if (s.allowLower) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                } else null,
            )
            FilterChip(
                selected = s.allowRaise,
                onClick = { s.setRaise(!s.allowRaise) },
                label = { Text("Allow raising") },
                leadingIcon = if (s.allowRaise) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                } else null,
            )
        }
    }
}

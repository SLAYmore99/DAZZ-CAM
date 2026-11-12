package com.funcam.app.ui.kit

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.funcam.app.ui.CamViewModel
import com.funcam.app.ui.Darkroom
import com.funcam.app.ui.FC
import kotlinx.coroutines.delay

/** What an edition's screen can ask its host to do. */
class Host(
    val openCameras: () -> Unit, val openLens: () -> Unit, val openTune: () -> Unit, val openSettings: () -> Unit,
    val openGallery: () -> Unit, val shutter: () -> Unit,
)

/**
 * Everything the three editions share around their own screen: camera lifecycle, the panels,
 * the photo picker, the Darkroom, messages and the light meter.
 * [sheet] lets tests open one panel straight away: "cams", "tune", "lens" or "settings".
 */
@Composable
fun EditionHost(
    vm: CamViewModel, skin: Skin, sheet: String? = null, settingsExtra: @Composable () -> Unit = {},
    screen: @Composable BoxScope.(Host) -> Unit,
) {
    val owner = LocalLifecycleOwner.current
    val ctx = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var open by remember { mutableStateOf(sheet) }
    var darkroom by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.bindCamera(owner); vm.startMeter() }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP && vm.recording) vm.stopRecording() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    // the Darkroom (shared with the classic edition) takes its colours from here
    SideEffect {
        FC.bg = skin.bg; FC.panel = skin.card; FC.panel2 = skin.faint; FC.amber = skin.accent; FC.text = skin.ink; FC.dim = skin.dim; FC.red = skin.danger
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.importPhoto(uri) }
    val host = remember {
        Host(
            openCameras = { open = "cams" }, openLens = { open = "lens" }, openTune = { open = "tune" }, openSettings = { open = "settings" },
            openGallery = {
                val uri = vm.lastUri
                val intent = if (uri != null) Intent(Intent.ACTION_VIEW).setDataAndType(uri, if (vm.lastIsVideo) "video/*" else "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                else Intent(Intent.ACTION_VIEW).setType("image/*")
                runCatching { ctx.startActivity(intent) }
            },
            shutter = { haptics.thud(); vm.onShutter() },
        )
    }
    CompositionLocalProvider(LocalSkin provides skin) {
        Box(Modifier.fillMaxSize().background(skin.bg)) {
            screen(host)
            CamerasPanel(vm, open == "cams") { open = null }
            LensPanel(vm, open == "lens") { open = null }
            TunePanel(vm, open == "tune") { open = null }
            SettingsPanel(vm, open == "settings", { open = null },
                onImport = { importer.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, onDarkroom = { darkroom = true }, extra = settingsExtra)
            val msg = vm.message
            LaunchedEffect(msg) { if (msg != null) { delay(2600); vm.message = null } }
            AnimatedVisibility(msg != null, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 96.dp, start = 28.dp, end = 28.dp),
                enter = fadeIn() + scaleIn(spring(dampingRatio = 0.6f), initialScale = 0.85f), exit = fadeOut()) {
                val shape = RoundedCornerShape(when (skin.kind) { Kind.DOT -> 16.dp; Kind.GLASS -> 20.dp; Kind.FIELD -> 8.dp })
                Box(Modifier.clip(shape).background(when (skin.kind) { Kind.DOT -> skin.ink; Kind.GLASS -> Color(0xE6282A2E); Kind.FIELD -> skin.ink }).padding(horizontal = 14.dp, vertical = 9.dp)) {
                    Label(msg ?: "", color = if (skin.kind == Kind.GLASS) skin.ink else skin.bg, size = if (skin.kind == Kind.GLASS) 13.sp else 10.5.sp, maxLines = 3)
                }
            }
            if (darkroom) Darkroom(vm) { darkroom = false }
        }
    }
}

fun recTime(vm: CamViewModel, now: Long): String {
    val s = ((now - vm.recordStart) / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

/** Ticks while recording so timers repaint. */
@Composable
fun recClock(vm: CamViewModel): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(vm.recording) { while (vm.recording) { now = System.currentTimeMillis(); delay(250) } }
    return now
}

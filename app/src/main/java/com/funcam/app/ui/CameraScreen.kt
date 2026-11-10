package com.funcam.app.ui

import android.content.Intent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.funcam.app.model.Cameras
import com.funcam.app.model.FPS_VALUES
import com.funcam.app.model.ISO_VALUES
import com.funcam.app.model.SHUTTER_SPEEDS
import com.funcam.app.model.focalLabel
import com.funcam.app.model.shutterLabel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class Dial { NONE, WB, LENS, EV, ISO, SHUTTER, FPS }

@Composable
fun CameraScreen(vm: CamViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val ctx = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var showPicker by remember { mutableStateOf(false) }
    var showAcc by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDarkroom by remember { mutableStateOf(false) }
    var dial by remember { mutableStateOf(Dial.NONE) }

    LaunchedEffect(Unit) { vm.bindCamera(lifecycleOwner) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP && vm.recording) vm.stopRecording() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(vm.recipe.id) { if (dial in listOf(Dial.ISO, Dial.SHUTTER, Dial.FPS) && !vm.recipe.manual) dial = Dial.NONE }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importPhoto(uri)
    }

    Box(Modifier.fillMaxSize().background(FC.bg)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            TopBar(
                vm, onAcc = { showAcc = true }, onSettings = { showSettings = true },
                onImport = { importLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onDarkroom = { showDarkroom = true },
            )
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Viewfinder(vm)
            }
            DialRow(vm, dial) { dial = if (dial == it) Dial.NONE else it }
            Box(Modifier.fillMaxWidth().height(58.dp), contentAlignment = Alignment.Center) {
                if (dial != Dial.NONE) DialRuler(vm, dial)
                else Text(
                    vm.recipe.description, color = FC.dim, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2,
                    lineHeight = 14.sp, modifier = Modifier.padding(horizontal = 36.dp),
                )
            }
            BottomBar(
                vm,
                onShutter = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); vm.onShutter() },
                onGallery = {
                    val uri = vm.lastUri
                    val intent = if (uri != null) Intent(Intent.ACTION_VIEW).setDataAndType(uri, if (vm.lastIsVideo) "video/*" else "image/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    else Intent(Intent.ACTION_VIEW).setType("image/*")
                    runCatching { ctx.startActivity(intent) }
                },
                onCameras = { showPicker = true },
            )
        }

        vm.message?.let { msg ->
            LaunchedEffect(msg) { delay(2600); vm.message = null }
            Text(
                msg, color = FC.text, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp, start = 24.dp, end = 24.dp)
                    .background(Color(0xE0222222), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        if (showPicker) CameraPickerSheet(vm) { showPicker = false }
        if (showAcc) AccessoriesSheet(vm) { showAcc = false }
        if (showSettings) SettingsSheet(vm) { showSettings = false }
        if (showDarkroom) Darkroom(vm) { showDarkroom = false }
    }
}

@Composable
private fun TopBar(
    vm: CamViewModel, onAcc: () -> Unit, onSettings: () -> Unit, onImport: () -> Unit, onDarkroom: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TopIcon(if (vm.flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff, vm.flashOn) { vm.toggleFlash() }
        TopIcon(
            when (vm.timer) { 3 -> Icons.Filled.Timer3; 10 -> Icons.Filled.Timer10; else -> Icons.Filled.TimerOff },
            vm.timer > 0,
        ) { vm.cycleTimer() }
        Text(
            vm.ratio.label, color = if (vm.recipe.ratios.size > 1) FC.text else FC.dim, fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.clip(RoundedCornerShape(7.dp)).border(1.dp, FC.dim.copy(alpha = 0.6f), RoundedCornerShape(7.dp))
                .clickable(enabled = vm.recipe.ratios.size > 1) { vm.cycleRatio() }.padding(horizontal = 9.dp, vertical = 3.dp),
        )
        TopIcon(Icons.Filled.Layers, vm.acc.doubleExposure) { vm.updateAcc(vm.acc.copy(doubleExposure = !vm.acc.doubleExposure)) }
        TopIcon(Icons.Filled.AutoAwesome, vm.acc.run { fisheyeW || fisheyeC || prism || star || nd }, onAcc)
        Box {
            TopIcon(Icons.Filled.MoreVert, false) { menu = true }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = FC.panel2) {
                DropdownMenuItem(
                    text = { Text(if (vm.grid) "Hide grid" else "Show grid") },
                    leadingIcon = { Icon(if (vm.grid) Icons.Filled.GridOff else Icons.Filled.Grid3x3, null) },
                    onClick = { vm.grid = !vm.grid; vm.saveSettings(); menu = false },
                )
                DropdownMenuItem(text = { Text("Import photo") }, leadingIcon = { Icon(Icons.Filled.AddPhotoAlternate, null) },
                    onClick = { menu = false; onImport() })
                DropdownMenuItem(text = { Text("Darkroom") }, leadingIcon = { Icon(Icons.Filled.Science, null) },
                    onClick = { menu = false; onDarkroom() })
                DropdownMenuItem(text = { Text("Settings") }, leadingIcon = { Icon(Icons.Filled.Settings, null) },
                    onClick = { menu = false; onSettings() })
            }
        }
    }
}

@Composable
private fun TopIcon(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, null, tint = if (active) FC.amber else FC.text) }
}

@Composable
private fun Viewfinder(vm: CamViewModel) {
    val spec = vm.frameSpec
    val blink = remember { Animatable(0f) }
    LaunchedEffect(vm.flashBlink) {
        if (vm.flashBlink > 0) { blink.snapTo(1f); blink.animateTo(0f, tween(240)) }
    }
    var focus by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(focus) { if (focus != null) { delay(900); focus = null } }
    var swipe by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val a = spec.outAspect
        val (w, h) = if (maxWidth / maxHeight > a) (maxHeight * a) to maxHeight else maxWidth to (maxWidth / a)
        val density = LocalDensity.current
        Box(
            Modifier.size(w, h).clip(RoundedCornerShape(3.dp))
                .pointerInput(spec) {
                    detectTapGestures { off ->
                        val x = off.x / size.width
                        val yUp = 1f - off.y / size.height
                        val r = spec.rect
                        val px = (x - r[0]) / (r[2] - r[0])
                        val py = 1f - (yUp - r[1]) / (r[3] - r[1])
                        if (px in 0f..1f && py in 0f..1f) { focus = off; vm.tapFocus(px, py) }
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (abs(swipe) > size.width * 0.18f) vm.stepCamera(if (swipe < 0) 1 else -1)
                            swipe = 0f
                        },
                        onDragCancel = { swipe = 0f },
                    ) { _, d -> swipe += d }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        if (zoomChange != 1f) vm.setZoomRatio(vm.zoom * zoomChange)
                    }
                },
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    SurfaceView(c).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {}
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                                vm.renderer.setDisplay(holder.surface, width, height)
                            }
                            override fun surfaceDestroyed(holder: SurfaceHolder) { vm.renderer.setDisplay(null, 0, 0) }
                        })
                    }
                },
            )
            if (vm.grid) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = spec.rect
                    val x0 = r[0] * size.width; val x1 = r[2] * size.width
                    val y0 = (1 - r[3]) * size.height; val y1 = (1 - r[1]) * size.height
                    val c = Color.White.copy(alpha = 0.35f)
                    for (i in 1..2) {
                        val x = x0 + (x1 - x0) * i / 3f
                        val y = y0 + (y1 - y0) * i / 3f
                        drawLine(c, Offset(x, y0), Offset(x, y1), 1.5f)
                        drawLine(c, Offset(x0, y), Offset(x1, y), 1.5f)
                    }
                }
            }
            focus?.let { f ->
                val s = with(density) { 64.dp.toPx() }
                Box(
                    Modifier.offset { IntOffset((f.x - s / 2).roundToInt(), (f.y - s / 2).roundToInt()) }
                        .size(64.dp).border(1.5.dp, FC.amber, RoundedCornerShape(6.dp)),
                )
            }
            if (blink.value > 0f) Box(Modifier.fillMaxSize().alpha(blink.value).background(Color.Black))
            if (vm.countdown > 0) {
                Text("${vm.countdown}", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
            }
            if (vm.recording) RecordingBadge(vm, Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
            else CameraNameToast(vm, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
            val status = vm.multiProgress ?: if (vm.busy && !vm.recording && vm.countdown == 0) "Developing…" else null
            status?.let {
                Text(
                    it, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center).background(FC.amber, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun CameraNameToast(vm: CamViewModel, modifier: Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(vm.recipe.id) { visible = true; delay(1400); visible = false }
    AnimatedVisibility(visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        Text(
            vm.recipe.name.uppercase(), color = FC.amber, fontWeight = FontWeight.Black, fontSize = 15.sp,
            modifier = Modifier.background(Color(0x99000000), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun RecordingBadge(vm: CamViewModel, modifier: Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(vm.recording) { while (vm.recording) { now = System.currentTimeMillis(); delay(250) } }
    val secs = ((now - vm.recordStart) / 1000).coerceAtLeast(0)
    Row(
        modifier.background(Color(0x99000000), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(FC.red, CircleShape).alpha(if ((now / 500) % 2 == 0L) 1f else 0.3f))
        Spacer(Modifier.width(6.dp))
        Text("%02d:%02d".format(secs / 60, secs % 60), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}

/** Round dial buttons under the viewfinder: WB · lens (mm) · EV, plus ISO / S / FPS on manual bodies. */
@Composable
private fun DialRow(vm: CamViewModel, dial: Dial, onSelect: (Dial) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DialButton(dial == Dial.WB, vm.temp != 0f, onClick = { onSelect(Dial.WB) }) {
            Icon(Icons.Filled.Thermostat, "White balance", tint = if (dial == Dial.WB) Color.Black else FC.text, modifier = Modifier.size(18.dp))
        }
        DialButton(dial == Dial.LENS, false, onClick = { onSelect(Dial.LENS) }) {
            Text(focalLabel(vm.zoom), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (dial == Dial.LENS) Color.Black else FC.text)
        }
        if (vm.recipe.manual) {
            DialButton(dial == Dial.ISO, vm.iso != 0, wide = true, onClick = { onSelect(Dial.ISO) }) {
                DialText(if (vm.iso == 0) "ISO A" else "ISO ${vm.iso}", dial == Dial.ISO)
            }
            DialButton(dial == Dial.SHUTTER, vm.shutter != 0f, wide = true, onClick = { onSelect(Dial.SHUTTER) }) {
                DialText(if (vm.shutter == 0f) "S A" else "S ${shutterLabel(vm.shutter)}", dial == Dial.SHUTTER)
            }
            if (vm.recipe.isVideo) DialButton(dial == Dial.FPS, false, wide = true, onClick = { onSelect(Dial.FPS) }) {
                DialText("${vm.fps}fps", dial == Dial.FPS)
            }
        }
        DialButton(dial == Dial.EV, vm.exposure != 0f, onClick = { onSelect(Dial.EV) }) {
            DialText("EV", dial == Dial.EV)
        }
    }
}

@Composable
private fun DialText(t: String, sel: Boolean) =
    Text(t, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (sel) Color.Black else FC.text, maxLines = 1)

@Composable
private fun DialButton(selected: Boolean, changed: Boolean, wide: Boolean = false, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.padding(horizontal = 4.dp).height(38.dp).width(if (wide) 70.dp else 38.dp).clip(RoundedCornerShape(19.dp))
            .background(if (selected) FC.amber else FC.panel2)
            .border(1.dp, if (changed && !selected) FC.amber.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(19.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Ruler-style slider for the selected dial. */
@Composable
private fun DialRuler(vm: CamViewModel, dial: Dial) {
    when (dial) {
        Dial.WB -> Ruler(vm.temp, -2f, 2f, label = "%+.1f".format(vm.temp), onReset = { vm.temp = 0f; vm.sync() }) { vm.temp = it; vm.sync() }
        Dial.EV -> Ruler(vm.exposure, -2f, 2f, label = "%+.1f EV".format(vm.exposure), onReset = { vm.exposure = 0f; vm.sync() }) { vm.exposure = it; vm.sync() }
        Dial.LENS -> {
            val stops = buildList {
                if (vm.caps.minZoom < 0.95f) add(vm.caps.minZoom)
                addAll(listOf(1f, 1.46f, 2.08f, 3.2f, 5f).filter { it <= vm.caps.maxZoom })
            }
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                stops.forEach { z ->
                    val sel = abs(vm.zoom - z) < 0.05f
                    Text(
                        focalLabel(z), fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center,
                        color = if (sel) Color.Black else FC.text,
                        modifier = Modifier.padding(horizontal = 4.dp).size(40.dp).clip(CircleShape)
                            .background(if (sel) FC.amber else FC.panel).clickable { vm.setZoomRatio(z) }.padding(top = 11.dp),
                    )
                }
            }
        }
        Dial.ISO -> StepRuler(ISO_VALUES.map { if (it == 0) "AUTO" else "$it" }, ISO_VALUES.indexOf(vm.iso).coerceAtLeast(0)) {
            vm.setManualValues(iso = ISO_VALUES[it])
        }
        Dial.SHUTTER -> {
            val labels = listOf("AUTO") + SHUTTER_SPEEDS.map { shutterLabel(it) }
            val idx = if (vm.shutter == 0f) 0 else SHUTTER_SPEEDS.indexOfFirst { it == vm.shutter } + 1
            StepRuler(labels, idx.coerceAtLeast(0)) { vm.setManualValues(shutter = if (it == 0) 0f else SHUTTER_SPEEDS[it - 1]) }
        }
        Dial.FPS -> StepRuler(FPS_VALUES.map { "$it" }, FPS_VALUES.indexOf(vm.fps).coerceAtLeast(0)) { vm.setManualValues(fps = FPS_VALUES[it]) }
        Dial.NONE -> {}
    }
}

@Composable
private fun Ruler(value: Float, min: Float, max: Float, label: String, onReset: () -> Unit, onChange: (Float) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = FC.amber, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onReset))
        RulerTrack((value - min) / (max - min)) { onChange(min + it * (max - min)) }
    }
}

@Composable
private fun StepRuler(labels: List<String>, index: Int, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(labels[index.coerceIn(0, labels.lastIndex)], color = FC.amber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        RulerTrack(index / (labels.size - 1f), ticks = labels.size) {
            val i = (it * (labels.size - 1)).roundToInt()
            if (i != index) onChange(i)
        }
    }
}

/** A tick ruler you drag left/right; the centre marker is the current value. */
@Composable
private fun RulerTrack(fraction: Float, ticks: Int = 41, onChange: (Float) -> Unit) {
    var f by remember { mutableFloatStateOf(fraction) }
    LaunchedEffect(fraction) { f = fraction }
    Canvas(
        Modifier.width(280.dp).height(30.dp).pointerInput(ticks) {
            detectHorizontalDragGestures { _, d ->
                f = (f - d / (size.width * 1.2f)).coerceIn(0f, 1f)
                onChange(f)
            }
        },
    ) {
        val spacing = size.width * 1.2f / (ticks - 1)
        val cx = size.width / 2
        for (i in 0 until ticks) {
            val x = cx + (i - f * (ticks - 1)) * spacing
            if (x < 0 || x > size.width) continue
            val major = ticks <= 15 || i % 5 == 0
            val fade = 1f - abs(x - cx) / cx
            drawLine(
                FC.text.copy(alpha = 0.25f + 0.6f * fade), Offset(x, size.height * (if (major) 0.25f else 0.45f)),
                Offset(x, size.height * 0.85f), if (major) 2.5f else 1.5f,
            )
        }
        drawLine(FC.amber, Offset(cx, 0f), Offset(cx, size.height), 4f)
    }
}

@Composable
private fun BottomBar(vm: CamViewModel, onShutter: () -> Unit, onGallery: () -> Unit, onCameras: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(FC.panel2)
                    .border(1.dp, FC.dim.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).clickable(onClick = onGallery),
                contentAlignment = Alignment.Center,
            ) {
                val t = vm.lastThumb
                if (t != null) Image(t.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Filled.PhotoLibrary, null, tint = FC.dim)
            }
            Spacer(Modifier.height(6.dp))
            Icon(Icons.Filled.Cameraswitch, "Flip camera", tint = FC.text, modifier = Modifier.size(22.dp).clickable { vm.flipCamera() })
        }
        ShutterButton(vm, onShutter)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(80.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onCameras).padding(vertical = 4.dp),
        ) {
            CameraIcon(vm.recipe, width = 70.dp)
            Text(vm.recipe.name, color = FC.amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun ShutterButton(vm: CamViewModel, onClick: () -> Unit) {
    val video = vm.recipe.isVideo
    val pressScale by animateFloatAsState(if (vm.busy && !vm.recording) 0.9f else 1f, label = "shutter")
    Box(
        Modifier.size(82.dp).scale(pressScale).border(4.dp, Color.White, CircleShape).padding(7.dp).clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            video && vm.recording -> Box(Modifier.size(28.dp).background(FC.red, RoundedCornerShape(6.dp)))
            video -> Box(Modifier.fillMaxSize().background(FC.red, CircleShape))
            else -> Box(Modifier.fillMaxSize().background(if (vm.busy) FC.dim else Color.White, CircleShape))
        }
    }
}

package com.funcam.app.ui

import android.content.Intent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.funcam.app.model.shutterLabel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class Panel { NONE, EXP, TEMP, TINT, ISO, SHUTTER, FPS }

@Composable
fun CameraScreen(vm: CamViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val ctx = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var showPicker by remember { mutableStateOf(false) }
    var showAcc by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDarkroom by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf(Panel.NONE) }
    var adjustOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.bindCamera(lifecycleOwner) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP && vm.recording) vm.stopRecording() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importPhoto(uri)
    }

    Box(Modifier.fillMaxSize().background(FC.bg)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            TopBar(
                vm, adjustOpen = adjustOpen,
                onAdjust = { adjustOpen = !adjustOpen; if (!adjustOpen) panel = Panel.NONE else if (panel == Panel.NONE) panel = Panel.EXP },
                onAcc = { showAcc = true }, onSettings = { showSettings = true },
                onImport = { importLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onDarkroom = { showDarkroom = true },
            )
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Viewfinder(vm)
            }
            Box(Modifier.fillMaxWidth().height(46.dp), contentAlignment = Alignment.Center) {
                if (!vm.recording) ZoomPills(vm, Modifier)
            }
            AnimatedVisibility(adjustOpen, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    AdjustRow(vm, panel) { panel = it }
                    if (panel != Panel.NONE) AdjustSlider(vm, panel)
                }
            }
            CameraCarousel(vm, onAll = { showPicker = true })
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
    vm: CamViewModel, adjustOpen: Boolean, onAdjust: () -> Unit, onAcc: () -> Unit, onSettings: () -> Unit,
    onImport: () -> Unit, onDarkroom: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
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
        TopIcon(Icons.Filled.Tune, adjustOpen || vm.exposure != 0f || vm.temp != 0f || vm.tint != 0f, onAdjust)
        val accOn = vm.acc.anyLens
        TopIcon(Icons.Filled.AutoAwesome, accOn, onAcc)
        Box {
            TopIcon(Icons.Filled.MoreVert, false) { menu = true }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = FC.panel2) {
                DropdownMenuItem(
                    text = { Text(if (vm.grid) "Hide grid" else "Show grid") },
                    leadingIcon = { Icon(if (vm.grid) Icons.Filled.GridOff else Icons.Filled.Grid3x3, null) },
                    onClick = { vm.grid = !vm.grid; vm.saveSettings(); menu = false },
                )
                DropdownMenuItem(
                    text = { Text("Import photo") }, leadingIcon = { Icon(Icons.Filled.AddPhotoAlternate, null) },
                    onClick = { menu = false; onImport() },
                )
                DropdownMenuItem(
                    text = { Text("Darkroom") }, leadingIcon = { Icon(Icons.Filled.Science, null) },
                    onClick = { menu = false; onDarkroom() },
                )
                DropdownMenuItem(
                    text = { Text("Settings") }, leadingIcon = { Icon(Icons.Filled.Settings, null) },
                    onClick = { menu = false; onSettings() },
                )
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
        if (vm.flashBlink > 0) {
            blink.snapTo(1f); blink.animateTo(0f, tween(220))
        }
    }
    var focus by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(focus) { if (focus != null) { delay(900); focus = null } }

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val maxW = maxWidth
        val maxH = maxHeight
        val a = spec.outAspect
        val (w, h) = if (maxW / maxH > a) (maxH * a) to maxH else maxW to (maxW / a)
        val density = LocalDensity.current
        Box(
            Modifier.size(w, h).clip(RoundedCornerShape(4.dp))
                .pointerInput(spec) {
                    detectTapGestures { off ->
                        val x = off.x / size.width
                        val yUp = 1f - off.y / size.height
                        val r = spec.rect
                        val px = (x - r[0]) / (r[2] - r[0])
                        val py = 1f - (yUp - r[1]) / (r[3] - r[1])
                        if (px in 0f..1f && py in 0f..1f) {
                            focus = off
                            vm.tapFocus(px, py)
                        }
                    }
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
                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                vm.renderer.setDisplay(null, 0, 0)
                            }
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
                Text(
                    "${vm.countdown}", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            if (vm.recording) RecordingBadge(vm, Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
            else CameraNameToast(vm, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))

            val status = vm.multiProgress ?: if (vm.busy && !vm.recording && vm.countdown == 0) "Developing…" else null
            status?.let {
                Text(
                    it, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                        .background(FC.amber, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
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

@Composable
private fun AdjustRow(vm: CamViewModel, panel: Panel, onSelect: (Panel) -> Unit) {
    val r = vm.recipe
    val items = buildList {
        add(Panel.EXP to "EXP " + "%+.1f".format(vm.exposure))
        add(Panel.TEMP to "TEMP " + "%+.1f".format(vm.temp))
        add(Panel.TINT to "TINT " + "%+.1f".format(vm.tint))
        // slow shutter (and manual ISO / frame rate) on every camera
        add(Panel.SHUTTER to "S " + if (vm.shutter == 0f) "AUTO" else shutterLabel(vm.shutter))
        add(Panel.ISO to "ISO " + if (vm.iso == 0) "AUTO" else vm.iso.toString())
        if (r.isVideo) add(Panel.FPS to "${vm.fps} FPS")
    }
    LazyRow(
        Modifier.fillMaxWidth().padding(top = 6.dp), contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items) { (p, label) ->
            val sel = p == panel
            Text(
                label, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                color = if (sel) Color.Black else FC.text,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) FC.amber else FC.panel2)
                    .clickable { onSelect(p) }.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun AdjustSlider(vm: CamViewModel, panel: Panel) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp)) {
        when (panel) {
            Panel.EXP -> ValueSlider(vm.exposure, -2f, 2f) { vm.exposure = it; vm.sync() }
            Panel.TEMP -> ValueSlider(vm.temp, -2f, 2f) { vm.temp = it; vm.sync() }
            Panel.TINT -> ValueSlider(vm.tint, -2f, 2f) { vm.tint = it; vm.sync() }
            Panel.ISO -> StepSlider(ISO_VALUES.map { if (it == 0) "AUTO" else "$it" }, ISO_VALUES.indexOf(vm.iso).coerceAtLeast(0)) {
                vm.setManualValues(iso = ISO_VALUES[it])
            }
            Panel.SHUTTER -> {
                val labels = listOf("AUTO") + SHUTTER_SPEEDS.map { shutterLabel(it) }
                val idx = if (vm.shutter == 0f) 0 else SHUTTER_SPEEDS.indexOfFirst { it == vm.shutter } + 1
                StepSlider(labels, idx.coerceAtLeast(0)) { vm.setManualValues(shutter = if (it == 0) 0f else SHUTTER_SPEEDS[it - 1]) }
            }
            Panel.FPS -> StepSlider(FPS_VALUES.map { "$it" }, FPS_VALUES.indexOf(vm.fps).coerceAtLeast(0)) {
                vm.setManualValues(fps = FPS_VALUES[it])
            }
            Panel.NONE -> {}
        }
    }
}

@Composable
private fun ValueSlider(value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Slider(
            value = value, onValueChange = onChange, valueRange = min..max, modifier = Modifier.weight(1f),
        )
        Text(
            "RESET", color = FC.amber, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onChange(0f) }.padding(8.dp),
        )
    }
}

@Composable
private fun StepSlider(labels: List<String>, index: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Slider(
            value = index.toFloat(), onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..(labels.size - 1).toFloat(), steps = (labels.size - 2).coerceAtLeast(0),
            modifier = Modifier.weight(1f),
        )
        Text(labels[index.coerceIn(0, labels.lastIndex)], color = FC.amber, fontFamily = FontFamily.Monospace,
            fontSize = 12.sp, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun CameraNameToast(vm: CamViewModel, modifier: Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(vm.recipe.id) { visible = true; delay(1600); visible = false }
    AnimatedVisibility(visible, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        Column(
            Modifier.background(Color(0x99000000), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(vm.recipe.name.uppercase(), color = FC.amber, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(vm.recipe.description, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp,
                textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.width(240.dp))
        }
    }
}

@Composable
private fun ZoomPills(vm: CamViewModel, modifier: Modifier) {
    val caps = vm.caps
    val stops = buildList {
        if (caps.minZoom < 0.95f) add(caps.minZoom)
        add(1f)
        if (caps.maxZoom >= 2f) add(2f)
        if (caps.maxZoom >= 3f) add(3f)
    }
    if (stops.size < 2) return
    Row(
        modifier.background(FC.panel, RoundedCornerShape(50)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        stops.forEach { z ->
            val sel = kotlin.math.abs(vm.zoom - z) < 0.05f
            val label = if (z < 1f) "%.1f".format(z) else "${z.toInt()}"
            Box(
                Modifier.size(if (sel) 34.dp else 28.dp).clip(CircleShape)
                    .background(if (sel) FC.panel2 else Color.Transparent)
                    .clickable { vm.setZoomRatio(z) },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (sel) "${label}×" else label, fontSize = if (sel) 11.sp else 10.sp, fontWeight = FontWeight.Bold,
                    color = if (sel) FC.amber else Color.White)
            }
        }
    }
}

@Composable
private fun CameraCarousel(vm: CamViewModel, onAll: () -> Unit) {
    val list = Cameras.all
    val state = rememberLazyListState()
    LaunchedEffect(vm.recipe.id) {
        val i = list.indexOfFirst { it.id == vm.recipe.id }
        if (i >= 0) state.animateScrollToItem((i - 2).coerceAtLeast(0))
    }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(vm.recipe.name.uppercase(), color = FC.amber, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text("  ·  ${vm.recipe.category.label}", color = FC.dim, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(
                "ALL CAMERAS", color = FC.text, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onAll)
                    .background(FC.panel2).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        LazyRow(
            state = state, flingBehavior = rememberSnapFlingBehavior(state),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(list, key = { it.id }) { r ->
                val sel = r.id == vm.recipe.id
                Box(
                    Modifier.size(if (sel) 64.dp else 54.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (sel) FC.panel2 else Color.Transparent)
                        .border(if (sel) 1.5.dp else 0.dp, if (sel) FC.amber else Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable { vm.selectRecipe(r) },
                    contentAlignment = Alignment.Center,
                ) {
                    CameraIcon(r, width = if (sel) 54.dp else 42.dp, modifier = Modifier.alpha(if (sel) 1f else 0.8f))
                }
            }
        }
    }
}

@Composable
private fun BottomBar(vm: CamViewModel, onShutter: () -> Unit, onGallery: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(FC.panel2)
                .border(1.dp, FC.dim.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).clickable(onClick = onGallery),
            contentAlignment = Alignment.Center,
        ) {
            val t = vm.lastThumb
            if (t != null) Image(t.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Filled.PhotoLibrary, null, tint = FC.dim)
        }
        ShutterButton(vm, onShutter)
        Box(
            Modifier.size(50.dp).clip(CircleShape).background(FC.panel2).clickable { vm.flipCamera() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Cameraswitch, "Flip", tint = FC.text, modifier = Modifier.size(26.dp)) }
    }
}

@Composable
private fun ShutterButton(vm: CamViewModel, onClick: () -> Unit) {
    val video = vm.recipe.isVideo
    Box(
        Modifier.size(78.dp).border(4.dp, Color.White, CircleShape).padding(8.dp).clip(CircleShape)
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

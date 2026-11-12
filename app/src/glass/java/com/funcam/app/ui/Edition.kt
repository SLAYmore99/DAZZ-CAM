package com.funcam.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.Iso
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShutterSpeed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funcam.app.model.shutterLabel
import com.funcam.app.ui.kit.Body
import com.funcam.app.ui.kit.CameraSnapRow
import com.funcam.app.ui.kit.Dial
import com.funcam.app.ui.kit.Display
import com.funcam.app.ui.kit.EditionHost
import com.funcam.app.ui.kit.Host
import com.funcam.app.ui.kit.KIconButton
import com.funcam.app.ui.kit.Kind
import com.funcam.app.ui.kit.Label
import com.funcam.app.ui.kit.LookThumb
import com.funcam.app.ui.kit.Skin
import com.funcam.app.ui.kit.Viewfinder
import com.funcam.app.ui.kit.dialParams
import com.funcam.app.ui.kit.glass
import com.funcam.app.ui.kit.pressable
import com.funcam.app.ui.kit.recClock
import com.funcam.app.ui.kit.recTime
import com.funcam.app.ui.kit.shutterPulse
import kotlinx.coroutines.delay

/** FUN CAM · GLASS — translucent, rounded, calm. The scene itself tints the interface. */
val GlassSkin = Skin(
    Kind.GLASS, bg = Color.Black, panel = Color(0xFB1E1E22), card = Color(0x14FFFFFF), ink = Color.White, dim = Color(0x99EBEBF5),
    faint = Color(0x4DEBEBF5), line = Color(0x1FFFFFFF), accent = Color(0xFFFFD60A), onAccent = Color.Black, danger = Color(0xFFFF453A),
    display = FontFamily.Default, label = FontFamily.Default, sheetCorner = 38.dp, caps = false,
)
private val Red = Color(0xFFFF453A)

@Composable
fun EditionRoot(vm: CamViewModel, sheet: String? = null, tray: Boolean = false) = EditionHost(vm, GlassSkin, sheet) { host -> GlassScreen(vm, host, tray) }

@Composable
private fun BoxScope.GlassScreen(vm: CamViewModel, host: Host, trayStart: Boolean) {
    val s = GlassSkin
    val r = vm.recipe
    var tray by remember { mutableStateOf(trayStart) }
    var pkey by remember { mutableStateOf<String?>(if (trayStart) "ev" else null) }
    val params = dialParams(vm)
    val param = params.firstOrNull { it.key == pkey }
    val now = recClock(vm)
    val looks = vm.orderedCameras()

    // look previews follow the scene: refreshed on entry, on mode change and every few seconds while idle
    LaunchedEffect(vm.isVideoMode, vm.favourites) { delay(700); while (true) { if (!tray) vm.refreshLookPreviews(); delay(6000) } }

    // a wash of the scene's own colour rises from the bottom of the screen
    val c = vm.sceneColor
    val tint = Color((c[0] * 0.75f + 0.04f).coerceIn(0f, 1f), (c[1] * 0.75f + 0.04f).coerceIn(0f, 1f), (c[2] * 0.75f + 0.05f).coerceIn(0f, 1f))
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black, 0.74f to Color.Black, 1f to tint)))

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(top = 8.dp, bottom = 10.dp)) {
        // ---- top: flash, the island, controls
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            KIconButton(if (vm.flashOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff, "Flash", size = 44.dp, active = vm.flashOn) { vm.toggleFlash() }
            Island(vm, now, host.openTune)
            Box(Modifier.rotate(animateFloatAsState(if (tray) 180f else 0f, spring(dampingRatio = 0.6f), label = "chev").value)) {
                KIconButton(Icons.Rounded.KeyboardArrowUp, "Controls", size = 44.dp, active = tray, tag = "tray") { tray = !tray; if (!tray) pkey = null }
            }
        }
        // ---- viewfinder
        Viewfinder(vm, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp), corner = 30.dp, maskColor = Color.Black,
            onSwipe = { d -> val i = looks.indexOfFirst { it.id == r.id }; vm.selectRecipe(looks[(i + d + looks.size) % looks.size]) },
            reticle = { m ->
                Canvas(Modifier.size(1.dp)) {
                    val h = 38.dp.toPx(); val y = s.accent
                    drawRoundRect(y, Offset(-h, -h), Size(2 * h, 2 * h), CornerRadius(6.dp.toPx()), style = Stroke(1.2.dp.toPx()))
                    val n = 6.dp.toPx()
                    drawLine(y, Offset(0f, -h), Offset(0f, -h + n), 1.dp.toPx()); drawLine(y, Offset(0f, h), Offset(0f, h - n), 1.dp.toPx())
                    drawLine(y, Offset(-h, 0f), Offset(-h + n, 0f), 1.dp.toPx()); drawLine(y, Offset(h, 0f), Offset(h - n, 0f), 1.dp.toPx())
                    // the sun slides on its line as you drag
                    val sx = h + 16.dp.toPx(); val sy = -m.exposure / 2f * 60.dp.toPx()
                    if (m.adjusting) { drawLine(y, Offset(sx, -62.dp.toPx()), Offset(sx, sy - 13.dp.toPx()), 1.dp.toPx()); drawLine(y, Offset(sx, sy + 13.dp.toPx()), Offset(sx, 62.dp.toPx()), 1.dp.toPx()) }
                    drawCircle(y, 4.dp.toPx(), Offset(sx, sy))
                    for (i in 0 until 8) { val a = i * Math.PI / 4; val dx = kotlin.math.cos(a).toFloat(); val dy = kotlin.math.sin(a).toFloat()
                        drawLine(y, Offset(sx + dx * 6.5.dp.toPx(), sy + dy * 6.5.dp.toPx()), Offset(sx + dx * 9.5.dp.toPx(), sy + dy * 9.5.dp.toPx()), 1.3.dp.toPx()) }
                }
                if (m.locked) Box(Modifier.offset((-44).dp, (-66).dp).background(s.accent, RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                    androidx.compose.material3.Text("AE/AF LOCK", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            },
        ) {
            val pulse = shutterPulse(vm, 240)
            if (pulse > 0f) Box(Modifier.fillMaxSize().alpha(pulse * 0.85f).background(Color.Black))
            if (vm.slowShutter) Row(
                Modifier.align(Alignment.TopStart).padding(12.dp).pressable({ vm.setManualValues(shutter = 0f) }).height(30.dp).clip(CircleShape).background(s.accent).padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Rounded.ShutterSpeed, null, tint = Color.Black, modifier = Modifier.size(15.dp))
                androidx.compose.material3.Text("Slow ${shutterLabel(vm.shutter)}", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.Close, "Turn off", tint = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(13.dp))
            }
            if (vm.histogramOn) Canvas(Modifier.align(Alignment.TopEnd).padding(14.dp).size(96.dp, 36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x59000000))) {
                val hist = vm.histogram
                val p = Path().apply {
                    moveTo(4.dp.toPx(), size.height - 4.dp.toPx())
                    for (x in 0 until 24) lineTo(4.dp.toPx() + x * (size.width - 8.dp.toPx()) / 23f, size.height - 4.dp.toPx() - hist[x] * (size.height - 10.dp.toPx()))
                    lineTo(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()); close()
                }
                drawPath(p, Color.White.copy(alpha = 0.85f))
            }
            if (!vm.recording) ZoomCapsule(vm, Modifier.align(Alignment.BottomCenter).padding(12.dp))
            if (vm.countdown > 0) androidx.compose.material3.Text("${vm.countdown}", Modifier.align(Alignment.Center), color = Color.White, fontSize = 110.sp, fontWeight = FontWeight.ExtraLight)
            else (vm.multiProgress ?: if (vm.busy && !vm.recording) "Developing" else null)?.let {
                Box(Modifier.align(Alignment.Center).glass(CircleShape, Color(0x66000000)).padding(horizontal = 14.dp, vertical = 7.dp)) { Body(it, color = Color.White, size = 13.sp) }
            }
        }
        // ---- looks strip and control tray share one slot
        AnimatedContent(tray, Modifier.fillMaxWidth().padding(top = 8.dp).height(98.dp), label = "deck",
            transitionSpec = { (fadeIn() + scaleIn(spring(dampingRatio = 0.7f), initialScale = 0.94f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.94f)) }) { open ->
            if (!open) Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.padding(start = 14.dp, end = 4.dp).testTag("cams").pressable(host.openCameras), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(54.dp).glass(RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PhotoCamera, "All cameras", tint = Color.White, modifier = Modifier.size(24.dp)) }
                    Label("All", Modifier.padding(top = 5.dp), size = 10.sp, weight = FontWeight.SemiBold)
                }
                CameraSnapRow(vm, looks, 66.dp, Modifier.weight(1f).height(98.dp)) { c2, centred, _ ->
                    val k by animateFloatAsState(if (centred) 1.14f else 1f, spring(dampingRatio = 0.5f), label = "look")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.scale(k).size(54.dp).then(if (centred) Modifier.border(2.5.dp, s.accent, RoundedCornerShape(17.dp)) else Modifier).clip(RoundedCornerShape(17.dp))) {
                            LookThumb(vm, c2, Modifier.fillMaxSize())
                            if (c2.id in vm.favourites) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(6.dp).background(s.accent, CircleShape))
                        }
                        Label(c2.name, Modifier.padding(top = 7.dp).widthIn(max = 64.dp), color = if (centred) s.accent else s.dim, size = 10.sp, weight = FontWeight.SemiBold)
                    }
                }
            } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(if (param != null) 8.dp else 12.dp)) {
                    val small = param != null
                    val icons = mapOf("ev" to Icons.Rounded.WbSunny, "wb" to Icons.Rounded.Thermostat, "tint" to Icons.Rounded.WaterDrop, "s" to Icons.Rounded.ShutterSpeed,
                        "iso" to Icons.Rounded.Iso, "zoom" to Icons.Rounded.ZoomIn, "fps" to Icons.Rounded.Movie)
                    params.forEach { p -> Tool(icons[p.key]!!, p.name, small, selected = p.key == pkey, on = p.isSet) { pkey = if (pkey == p.key) null else p.key } }
                    Tool(Icons.Rounded.Timer, if (vm.timer > 0) "${vm.timer} s" else "Timer", small, on = vm.timer > 0) { vm.cycleTimer() }
                    Tool(null, "Shape", small, text = vm.ratio.label, dim = r.ratios.size < 2) { vm.cycleRatio() }
                    Tool(Icons.Rounded.Grid3x3, "Grid", small, on = vm.grid) { vm.grid = !vm.grid; vm.saveSettings() }
                    Tool(Icons.Rounded.AutoAwesome, "Lenses", small, on = vm.acc.anyLens, tag = "lens", onClick = host.openLens)
                    Tool(Icons.Rounded.Tune, "Customize", small, on = vm.tuned, onClick = host.openTune)
                    Tool(Icons.Rounded.AutoMode, "Auto", small, dim = !vm.dirty) { vm.auto() }
                    Tool(Icons.Rounded.Settings, "Settings", small, tag = "settings", onClick = host.openSettings)
                }
                if (param != null) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 6.dp).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Label(param.name, Modifier.width(62.dp), size = 11.sp, weight = FontWeight.SemiBold)
                    Dial(param, Modifier.weight(1f).height(44.dp))
                    Box(Modifier.pressable({ param.reset() }).size(62.dp, 30.dp).clip(CircleShape).background(Color(0x24FFFFFF)), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text(param.value, color = s.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // ---- photo / video
        val k by animateFloatAsState(if (vm.isVideoMode) 0f else 1f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "mode")
        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 10.dp).glass(CircleShape, Color(0x4D000000)).padding(3.dp)) {
            Box(Modifier.offset { IntOffset((76 * k).dp.roundToPx(), 0) }.size(76.dp, 30.dp).glass(CircleShape, Color(0x33FFFFFF)))
            Row {
                listOf("VIDEO" to true, "PHOTO" to false).forEach { (t, v) ->
                    Box(Modifier.pressable({ vm.setVideoMode(v) }).size(76.dp, 30.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text(t, color = if (vm.isVideoMode == v) s.accent else s.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
        // ---- dock (swipe up to open the controls)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 34.dp).pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { if (total < -30.dp.toPx()) tray = true else if (total > 30.dp.toPx()) { tray = false; pkey = null } }) { _, d -> total += d }
            },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val t = vm.lastThumb
            Box(Modifier.pressable(host.openGallery).size(52.dp).clip(RoundedCornerShape(15.dp)).background(Color(0x1FFFFFFF)).border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(15.dp))) {
                if (t != null) Image(t.asImageBitmap(), "Last shot", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            GlassShutter(vm, host.shutter)
            KIconButton(Icons.Rounded.Cameraswitch, "Flip camera", size = 52.dp) { vm.flipCamera() }
        }
    }
}

/** The pill at the top: camera name (tap to customize), widens to say what changed, turns red with the timer while recording. */
@Composable
private fun Island(vm: CamViewModel, now: Long, onClick: () -> Unit) {
    val s = GlassSkin
    var wide by remember { mutableStateOf(false) }
    LaunchedEffect(vm.recipe.id) { wide = true; delay(2200); wide = false }
    Row(
        Modifier.testTag("tune").pressable(onClick).height(36.dp).glass(CircleShape, if (vm.recording) Color(0x8C780A06) else Color(0x6B000000)).animateContentSize(spring(dampingRatio = 0.6f, stiffness = 300f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (vm.recording) {
            Box(Modifier.size(8.dp).alpha(if ((now / 500) % 2 == 0L) 1f else 0.3f).background(Red, CircleShape))
            androidx.compose.material3.Text(recTime(vm, now), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        } else {
            androidx.compose.material3.Text(vm.recipe.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (wide) Body(vm.recipe.category.label, size = 12.sp, maxLines = 1)
            Icon(Icons.Rounded.Tune, "Customize", tint = if (vm.tuned) s.accent else s.dim, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun Tool(icon: ImageVector?, label: String, small: Boolean, selected: Boolean = false, on: Boolean = false, text: String? = null, dim: Boolean = false, tag: String? = null, onClick: () -> Unit) {
    val s = GlassSkin
    val d = if (small) 36.dp else 44.dp
    Column(Modifier.alpha(if (dim) 0.4f else 1f).then(if (tag != null) Modifier.testTag(tag) else Modifier).pressable(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(d).then(if (selected) Modifier.clip(CircleShape).background(Color.White) else if (on) Modifier.clip(CircleShape).background(s.accent) else Modifier.glass(CircleShape)), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, label, tint = if (selected || on) Color.Black else Color.White, modifier = Modifier.size(d * 0.5f))
            else androidx.compose.material3.Text(text ?: "", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (!small) Label(label, Modifier.padding(top = 4.dp), color = if (selected || on) s.ink else s.dim, size = 9.5.sp)
    }
}

@Composable
private fun ZoomCapsule(vm: CamViewModel, modifier: Modifier) {
    val caps = vm.caps
    val stops = buildList { if (caps.minZoom < 0.95f) add(caps.minZoom); add(1f); if (caps.maxZoom >= 2f) add(2f); if (caps.maxZoom >= 3f) add(3f) }
    if (stops.size < 2) return
    val nearest = stops.minBy { kotlin.math.abs(it - vm.zoom) }
    Row(modifier.glass(CircleShape, Color(0x59000000)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        stops.forEach { z ->
            val sel = z == nearest
            val w by animateFloatAsState(if (sel) 40f else 32f, spring(dampingRatio = 0.55f), label = "zoom")
            Box(Modifier.pressable({ vm.setZoomRatio(z) }).size(w.dp, 32.dp).clip(CircleShape).background(if (sel) Color(0x33FFFFFF) else Color.Transparent), contentAlignment = Alignment.Center) {
                val label = if (sel) (if (vm.zoom % 1f < 0.05f) "${vm.zoom.toInt()}×" else "%.1f×".format(vm.zoom)) else if (z < 1f) "%.1f".format(z).trimStart('0') else "${z.toInt()}"
                androidx.compose.material3.Text(label, color = if (sel) GlassSkin.accent else Color.White, fontSize = if (sel) 12.sp else 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun GlassShutter(vm: CamViewModel, onClick: () -> Unit) {
    val video = vm.recipe.isVideo
    val working = vm.busy && !vm.recording && vm.countdown == 0
    val inner by animateFloatAsState(if (vm.recording) 0.44f else if (working) 0.8f else 1f, spring(dampingRatio = 0.55f), label = "inner")
    Box(Modifier.testTag("shutter").pressable(onClick, haptic = false).size(80.dp).border(4.dp, if (vm.slowShutter) GlassSkin.accent else Color.White, CircleShape).padding(8.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer { scaleX = inner; scaleY = inner }.clip(RoundedCornerShape(if (vm.recording) 26 else 50))
            .background(if (video) Red else if (working) Color.White.copy(alpha = 0.4f) else Color.White))
    }
}

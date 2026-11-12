package com.funcam.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funcam.app.model.shutterLabel
import com.funcam.app.ui.kit.CameraSnapRow
import com.funcam.app.ui.kit.Dial
import com.funcam.app.ui.kit.Display
import com.funcam.app.ui.kit.Doto
import com.funcam.app.ui.kit.EditionHost
import com.funcam.app.ui.kit.Host
import com.funcam.app.ui.kit.KIconButton
import com.funcam.app.ui.kit.Kind
import com.funcam.app.ui.kit.Label
import com.funcam.app.ui.kit.Skin
import com.funcam.app.ui.kit.Viewfinder
import com.funcam.app.ui.kit.dialParams
import com.funcam.app.ui.kit.dotLine
import com.funcam.app.ui.kit.pressable
import com.funcam.app.ui.kit.recClock
import com.funcam.app.ui.kit.recTime
import com.funcam.app.ui.kit.shutterPulse
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/** FUN CAM · DOT — monochrome, dot-matrix, one red accent. */
val DotSkin = Skin(
    Kind.DOT, bg = Color.Black, panel = Color(0xFF050505), card = Color(0xFF0E0E0E), ink = Color.White, dim = Color(0xFF8B8B8B),
    faint = Color(0xFF3A3A3A), line = Color(0xFF262626), accent = Color(0xFFE1202A), onAccent = Color.White, danger = Color(0xFFE1202A),
    display = Doto, label = FontFamily.Monospace, sheetCorner = 26.dp, caps = true,
)

@Composable
fun EditionRoot(vm: CamViewModel, sheet: String? = null) = EditionHost(vm, DotSkin, sheet) { host -> DotScreen(vm, host) }

private const val GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

/** The camera name resolves out of random glyphs, like a split-flap board made of dots. */
@Composable
private fun scrambled(text: String): String {
    var shown by remember { mutableStateOf(text) }
    LaunchedEffect(text) {
        for (n in 0..text.length) {
            shown = text.mapIndexed { i, ch -> if (i < n || ch == ' ') ch else GLYPHS.random() }.joinToString("")
            delay(28)
        }
        shown = text
    }
    return shown
}

@Composable
private fun BoxScope.DotScreen(vm: CamViewModel, host: Host) {
    val s = DotSkin
    val r = vm.recipe
    val list = vm.cameras()
    var pkey by remember { mutableStateOf("ev") }
    val params = dialParams(vm)
    val param = params.firstOrNull { it.key == pkey } ?: params.first()
    val now = recClock(vm)

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(top = 6.dp, bottom = 8.dp)) {
        // ---- the camera's name is the headline
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Display(scrambled(r.name.uppercase()), size = 30.sp)
                Row(Modifier.padding(top = 4.dp)) {
                    Label("${r.category.label} · ${(list.indexOfFirst { it.id == r.id } + 1).toString().padStart(2, '0')}/${list.size}", size = 9.5.sp)
                    if (vm.tuned) Label(" · TUNED", color = s.accent, size = 9.5.sp)
                    if (vm.acc.anyLens) Label(" · LENS", color = s.accent, size = 9.5.sp)
                    if (vm.focusLocked) Label(" · AE/AF LOCK", color = s.accent, size = 9.5.sp)
                }
            }
            val auto by animateFloatAsState(if (vm.dirty) 1f else 0f, spring(dampingRatio = 0.5f), label = "auto")
            Box(Modifier.scale(0.6f + 0.4f * auto).alpha(auto).pressable({ vm.auto() }, enabled = vm.dirty).size(38.dp).border(1.dp, s.accent, CircleShape), contentAlignment = Alignment.Center) {
                Display("A", color = s.accent, size = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            KIconButton(Icons.Outlined.Settings, "Settings", size = 38.dp, tag = "settings", onClick = host.openSettings)
        }
        // ---- quick row: labelled toggles with a status dot
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Quick("FLASH", vm.flashOn, Modifier.weight(1.2f)) { vm.toggleFlash() }
            Quick(if (vm.timer > 0) "${vm.timer}S" else "TIMER", vm.timer > 0, Modifier.weight(1.2f)) { vm.cycleTimer() }
            Quick(vm.ratio.label, false, Modifier.weight(1f), enabled = r.ratios.size > 1) { vm.cycleRatio() }
            Quick("GRID", vm.grid, Modifier.weight(1f)) { vm.grid = !vm.grid; vm.saveSettings() }
            Quick("HIST", vm.histogramOn, Modifier.weight(1f)) { vm.setHistogram(!vm.histogramOn) }
        }
        // ---- viewfinder
        Viewfinder(vm, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), corner = 18.dp, maskColor = s.bg,
            onSwipe = { d -> val i = list.indexOfFirst { it.id == r.id }; vm.selectRecipe(list[(i + d + list.size) % list.size]) },
            reticle = { m ->
                // four corner dots and an exposure line of dots with a red marker
                Canvas(Modifier.size(1.dp)) {
                    val h = 37.dp.toPx()
                    for ((dx, dy) in listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)) drawCircle(if (m.locked) s.accent else Color.White, 2.6.dp.toPx(), Offset(dx * h, dy * h))
                    val x = h + 18.dp.toPx()
                    var y = -50.dp.toPx()
                    while (y <= 50.dp.toPx()) { drawCircle(Color.White.copy(alpha = 0.6f), 1.dp.toPx(), Offset(x, y)); y += 6.dp.toPx() }
                    drawCircle(s.accent, 5.5.dp.toPx(), Offset(x, -m.exposure / 2f * 50.dp.toPx()))
                }
            },
        ) {
            val pulse = shutterPulse(vm, 340)
            if (pulse > 0f) Canvas(Modifier.fillMaxSize().alpha(pulse)) {
                // the shutter is a screen of dots that clears
                drawRect(Color.Black)
                val pitch = 9.dp.toPx()
                var y = pitch / 2
                while (y < size.height) { dotLine(Color.White, y, pitch = pitch, r = 1.1.dp.toPx()); y += pitch }
            }
            if (vm.slowShutter) Row(
                Modifier.align(Alignment.TopStart).padding(10.dp).pressable({ vm.setManualValues(shutter = 0f) }).height(26.dp).clip(CircleShape).background(s.accent).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).background(Color.White, CircleShape)); Spacer(Modifier.width(6.dp))
                Label("SLOW ${shutterLabel(vm.shutter)}  ×", color = Color.White, weight = FontWeight.Bold)
            }
            if (vm.recording) Row(Modifier.align(Alignment.TopEnd).padding(10.dp).height(26.dp).clip(CircleShape).background(Color(0xCC000000)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).alpha(if ((now / 500) % 2 == 0L) 1f else 0.25f).background(s.accent, CircleShape)); Spacer(Modifier.width(6.dp))
                Display(recTime(vm, now), size = 16.sp)
            } else Box(
                Modifier.align(Alignment.BottomCenter).padding(10.dp).pressable({
                    vm.setZoomRatio(if (vm.zoom < 1.95f && vm.caps.maxZoom >= 2f) 2f else if (vm.zoom < 2.95f && vm.caps.maxZoom >= 3f) 3f else if (vm.zoom > 1.05f || vm.caps.minZoom >= 1f) 1f else vm.caps.minZoom)
                }).height(26.dp).clip(CircleShape).background(Color(0xCC000000)).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape).padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center,
            ) { Display("%.1f×".format(vm.zoom), size = 15.sp) }
            if (vm.histogramOn) Canvas(Modifier.align(Alignment.BottomStart).padding(12.dp).size(96.dp, 28.dp)) {
                val hist = vm.histogram
                for (x in 0 until 24) for (y in 0 until 7) {
                    val lit = hist[x] * 7 > y
                    drawCircle(if (lit) (if (x > 21) s.accent else Color.White) else Color.White.copy(alpha = 0.16f), (if (lit) 1.3f else 0.8f).dp.toPx(),
                        Offset((2 + x * 4).dp.toPx(), (26 - y * 4).dp.toPx()))
                }
            }
            val status = if (vm.countdown > 0) null else vm.multiProgress ?: if (vm.busy && !vm.recording) "DEVELOPING" else null
            if (vm.countdown > 0) Display("${vm.countdown}", Modifier.align(Alignment.Center), size = 120.sp)
            status?.let { Display(it, Modifier.align(Alignment.Center), size = 24.sp) }
        }
        // ---- parameter tabs + ruler of dots
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp), verticalAlignment = Alignment.Bottom) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                params.forEach { p ->
                    val sel = p.key == param.key
                    Column(Modifier.pressable({ pkey = p.key }), horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(p.short, color = if (sel) s.ink else if (p.isSet) s.accent else s.dim, modifier = Modifier.padding(vertical = 5.dp))
                        Box(Modifier.size(4.dp).background(if (sel) s.accent else Color.Transparent, CircleShape))
                    }
                }
            }
            Box(Modifier.pressable({ param.reset() })) { Display(param.value, size = 26.sp) }
        }
        Dial(param, Modifier.fillMaxWidth().height(40.dp))
        // ---- camera ticker: names, not pictures
        Row(Modifier.fillMaxWidth().drawBehind { dotLine(s.line, 0f) }.padding(start = 16.dp, end = 16.dp, top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            ModeSwitch(vm)
            CameraSnapRow(vm, list, 132.dp, Modifier.weight(1f).height(50.dp)) { c, centred, _ ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Display(c.name, color = if (centred) s.ink else Color(0xFF4A4A4A), size = 18.sp)
                    if (c.id in vm.favourites) Box(Modifier.padding(start = 4.dp).size(4.dp).background(s.accent, CircleShape))
                }
            }
            KIconButton(Icons.Outlined.PhotoCamera, "All cameras", size = 38.dp, tag = "cams", onClick = host.openCameras)
        }
        // ---- dock
        Row(Modifier.fillMaxWidth().padding(horizontal = 26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val t = vm.lastThumb
                val pop by animateFloatAsState(1f, spring(dampingRatio = 0.4f), label = "thumb")
                Box(Modifier.pressable(host.openGallery).size(52.dp).scale(pop).clip(CircleShape).background(Color(0xFF111111)).border(1.dp, if (t != null) s.ink else s.faint, CircleShape)) {
                    if (t != null) Image(t.asImageBitmap(), "Last shot", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                TextKey("TUNE", vm.tuned, "tune", host.openTune)
            }
            GlyphShutter(vm, host.shutter)
            Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                KIconButton(Icons.Outlined.Cameraswitch, "Flip camera", size = 52.dp) { vm.flipCamera() }
                TextKey("LENS", vm.acc.anyLens, "lens", host.openLens)
            }
        }
    }
}

@Composable
private fun Quick(text: String, on: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val s = DotSkin
    Row(
        modifier.alpha(if (enabled) 1f else 0.35f).pressable(onClick, enabled).height(30.dp).border(1.dp, if (on) Color(0xFF555555) else s.line, CircleShape),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(5.dp).background(if (on) s.accent else s.faint, CircleShape)); Spacer(Modifier.width(6.dp))
        Label(text, color = if (on) s.ink else s.dim, size = 9.5.sp)
    }
}

@Composable
private fun TextKey(text: String, on: Boolean, tag: String, onClick: () -> Unit) {
    val s = DotSkin
    Box(Modifier.padding(top = 8.dp).testTag(tag).pressable(onClick).border(1.dp, if (on) s.accent else Color.Transparent, CircleShape).padding(horizontal = 9.dp, vertical = 5.dp)) {
        Label(text, color = if (on) s.accent else s.dim, size = 9.sp)
    }
}

@Composable
private fun ModeSwitch(vm: CamViewModel) {
    val s = DotSkin
    val k by animateFloatAsState(if (vm.isVideoMode) 1f else 0f, spring(dampingRatio = 0.55f), label = "mode")
    Box(Modifier.pressable({ vm.setVideoMode(!vm.isVideoMode) }).size(62.dp, 38.dp).border(1.dp, s.faint, CircleShape)) {
        Box(Modifier.offset { IntOffset(2.dp.roundToPx(), (2 + 17 * k).dp.roundToPx()) }.size(58.dp, 17.dp).background(if (vm.isVideoMode) s.accent else s.ink, CircleShape))
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
            Label("PHOTO", color = if (vm.isVideoMode) s.dim else s.bg, size = 8.sp)
            Label("VIDEO", color = if (vm.isVideoMode) Color.White else s.dim, size = 8.sp)
        }
    }
}

/** The shutter sits in a ring of 28 dots: it counts down, chases while developing, pulses red while recording. */
@Composable
private fun GlyphShutter(vm: CamViewModel, onClick: () -> Unit) {
    val s = DotSkin
    val video = vm.recipe.isVideo
    val working = vm.busy && !vm.recording && vm.countdown == 0
    val inf = rememberInfiniteTransition(label = "ring")
    val spin by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "spin")
    val beat by inf.animateFloat(1f, 0.25f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "beat")
    val pulse = shutterPulse(vm, 420)
    val inner by animateFloatAsState(if (vm.recording) 30f else 66f, spring(dampingRatio = 0.6f), label = "inner")
    Box(Modifier.testTag("shutter").pressable(onClick, haptic = false).size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val n = 28
            for (i in 0 until n) {
                val a = (i.toFloat() / n) * 2f * Math.PI.toFloat() - Math.PI.toFloat() / 2f
                val p = Offset(center.x + cos(a) * 48.dp.toPx(), center.y + sin(a) * 48.dp.toPx())
                val c = when {
                    vm.recording -> s.accent.copy(alpha = if (i % 2 == 0) beat else 1.25f - beat)
                    vm.countdown > 0 -> if (i < n * (1f - (vm.countdown - 1f) / vm.timer.coerceAtLeast(1))) Color.White else Color(0xFF333333)
                    working -> if ((i + (spin * n).toInt()) % 3 == 0) Color.White else Color(0xFF333333)
                    pulse > 0f && i <= n * (1f - pulse) + 2 && i >= n * (1f - pulse) - 4 -> Color.White
                    vm.slowShutter && i % 2 == 0 -> s.accent
                    else -> Color(0xFF333333)
                }
                drawCircle(c, 1.9.dp.toPx(), p)
            }
        }
        Box(Modifier.size(inner.dp).clip(RoundedCornerShape(if (vm.recording) 8.dp else 33.dp)).background(if (video) s.accent else if (working) Color(0xFF555555) else Color.White))
    }
}

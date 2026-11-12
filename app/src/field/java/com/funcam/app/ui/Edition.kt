package com.funcam.app.ui

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funcam.app.model.shutterLabel
import com.funcam.app.ui.kit.CameraSnapRow
import com.funcam.app.ui.kit.Dial
import com.funcam.app.ui.kit.EditionHost
import com.funcam.app.ui.kit.Grotesk
import com.funcam.app.ui.kit.Host
import com.funcam.app.ui.kit.KIconButton
import com.funcam.app.ui.kit.KSection
import com.funcam.app.ui.kit.Kind
import com.funcam.app.ui.kit.Skin
import com.funcam.app.ui.kit.ToggleRow
import com.funcam.app.ui.kit.Viewfinder
import com.funcam.app.ui.kit.dialParams
import com.funcam.app.ui.kit.evLabel
import com.funcam.app.ui.kit.keySurface
import com.funcam.app.ui.kit.pressable
import com.funcam.app.ui.kit.recClock
import com.funcam.app.ui.kit.recTime
import com.funcam.app.ui.kit.shutterPulse
import kotlin.math.cos
import kotlin.math.sin

/** FUN CAM · FIELD — a pocket instrument: bone body, pressable keys, a thumbwheel, an LCD and one orange key. */
private val Orange = Color(0xFFFF5A1F)
private val Well = Color(0xFF151515)
val FieldBone = Skin(
    Kind.FIELD, bg = Color(0xFFE9E6DF), panel = Color(0xFFE9E6DF), card = Color(0xFFE0DDD3), ink = Color(0xFF1B1B1A), dim = Color(0xFF7B776D),
    faint = Color(0xFFCFCBC0), line = Color(0xFFB8B4A8), accent = Orange, onAccent = Color.White, danger = Color(0xFFE3311F),
    display = Grotesk, label = Grotesk, sheetCorner = 22.dp, caps = true, dark = false,
)
val FieldGraphite = FieldBone.copy(
    bg = Color(0xFF232427), panel = Color(0xFF232427), card = Color(0xFF2A2C30), ink = Color(0xFFECEBE6), dim = Color(0xFF8D8F95),
    faint = Color(0xFF17181A), line = Color(0xFF0C0C0D), dark = true,
)
private val LcdInk = Color(0xFF1C2319)

@Composable
fun EditionRoot(vm: CamViewModel, sheet: String? = null, graphite: Boolean? = null) {
    val prefs = LocalContext.current.getSharedPreferences("funcam", Context.MODE_PRIVATE)
    var dark by remember { mutableStateOf(graphite ?: prefs.getBoolean("field_dark", false)) }
    val skin = if (dark) FieldGraphite else FieldBone
    EditionHost(vm, skin, sheet, settingsExtra = {
        KSection("Body")
        ToggleRow("Graphite body", "Dark body for shooting at night", dark, first = true, last = true) { dark = it; prefs.edit().putBoolean("field_dark", it).apply() }
    }) { host -> FieldScreen(vm, host, skin) }
}

@Composable
private fun Mono(text: String, size: Float, color: Color, modifier: Modifier = Modifier, weight: FontWeight = FontWeight.Bold) =
    androidx.compose.material3.Text(text, modifier, color = color, fontSize = size.sp, fontFamily = FontFamily.Monospace, fontWeight = weight, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)

@Composable
private fun Engraved(text: String, size: Float, color: Color, modifier: Modifier = Modifier) =
    androidx.compose.material3.Text(text, modifier, color = color, fontSize = size.sp, fontFamily = Grotesk, fontWeight = FontWeight.Bold, letterSpacing = (size * 0.12f).sp, maxLines = 1, softWrap = false)

private fun Modifier.lcd() = this.clip(RoundedCornerShape(9.dp)).background(Brush.verticalGradient(listOf(Color(0xFFA9B297), Color(0xFFC3CBB1))))
private fun Modifier.recess(radius: Int = 10) = this.clip(RoundedCornerShape(radius.dp)).background(Well)

@Composable
private fun BoxScope.FieldScreen(vm: CamViewModel, host: Host, s: Skin) {
    val r = vm.recipe
    val list = vm.cameras()
    var pkey by remember { mutableStateOf("ev") }
    val params = dialParams(vm)
    val param = params.firstOrNull { it.key == pkey } ?: params.first()
    val now = recClock(vm)
    var hover by remember(r.id) { mutableStateOf(r.name) }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(top = 8.dp, bottom = 10.dp)) {
        // ---- top plate: brand, LCD, photo/video slide switch
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(60.dp)) {
                androidx.compose.material3.Text("FUN CAM", color = s.ink, fontSize = 13.sp, fontFamily = Grotesk, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                Engraved("FIELD · F-51", 6.5f, s.dim)
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp).testTag("tune").pressable(host.openTune).lcd().padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    androidx.compose.material3.Text(hover.uppercase(), Modifier.weight(1f), color = LcdInk, fontSize = 18.sp, fontFamily = Grotesk, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    Mono("№ " + ((vm.shots % 36) + 1).toString().padStart(2, '0'), 13f, LcdInk)
                }
                Mono(listOfNotNull("EV ${evLabel(vm.exposure)}", "S ${if (vm.shutter == 0f) "AUTO" else shutterLabel(vm.shutter)}", vm.ratio.label,
                    if (r.isVideo) "${vm.fps} FPS" else null, if (vm.flashOn) "FLASH" else null, if (vm.focusLocked) "LOCK" else null, if (vm.tuned) "TUNED" else null).joinToString(" · "),
                    9f, LcdInk.copy(alpha = 0.8f), Modifier.padding(top = 2.dp), FontWeight.SemiBold)
            }
            val k by animateFloatAsState(if (vm.isVideoMode) 1f else 0f, spring(dampingRatio = 0.55f), label = "mode")
            Column(Modifier.pressable({ vm.setVideoMode(!vm.isVideoMode) }).width(46.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Engraved("PHOTO", 6.5f, if (vm.isVideoMode) s.dim else s.ink)
                Box(Modifier.padding(vertical = 3.dp).size(40.dp, 20.dp).recess()) {
                    Box(Modifier.offset { IntOffset((2 + 18 * k).dp.roundToPx(), 2.dp.roundToPx()) }.size(18.dp, 16.dp).clip(RoundedCornerShape(8.dp))
                        .background(Brush.verticalGradient(if (vm.isVideoMode) listOf(Color(0xFFFF6A4A), s.danger) else listOf(Color(0xFFFAFAFA), Color(0xFFCFCFCF)))))
                }
                Engraved("VIDEO", 6.5f, if (vm.isVideoMode) s.danger else s.dim)
            }
        }
        // ---- the viewfinder sits in a recessed dark window
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).recess(20).padding(7.dp)) {
            Viewfinder(vm, Modifier.fillMaxSize(), corner = 13.dp, maskColor = Well,
                onSwipe = { d -> val i = list.indexOfFirst { it.id == r.id }; vm.selectRecipe(list[(i + d + list.size) % list.size]) },
                reticle = { m ->
                    Canvas(Modifier.size(1.dp)) {
                        drawCircle(if (m.locked) Orange else Color.White, 35.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                        drawCircle(Orange, 2.5.dp.toPx())
                        // exposure fader
                        val x = 49.dp.toPx()
                        drawRoundRect(Color.Black.copy(alpha = 0.5f), Offset(x - 4.dp.toPx(), -50.dp.toPx()), Size(8.dp.toPx(), 100.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                        drawRoundRect(Orange, Offset(x - 8.dp.toPx(), -m.exposure / 2f * 44.dp.toPx() - 6.dp.toPx()), Size(16.dp.toPx(), 12.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                    }
                },
            ) {
                // mechanical shutter: two blades meet and part
                val pulse = shutterPulse(vm, 300)
                if (pulse > 0f) {
                    val close = 1f - kotlin.math.abs(pulse - 0.5f) * 2f
                    Canvas(Modifier.fillMaxSize()) {
                        val hh = size.height * 0.51f * close.coerceIn(0f, 1f)
                        drawRect(Color(0xFF0B0B0B), Offset.Zero, Size(size.width, hh)); drawRect(Color(0xFF0B0B0B), Offset(0f, size.height - hh), Size(size.width, hh))
                    }
                }
                if (vm.slowShutter) Box(Modifier.align(Alignment.TopStart).padding(9.dp).pressable({ vm.setManualValues(shutter = 0f) }).clip(RoundedCornerShape(5.dp)).background(Orange).padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Mono("SLOW ${shutterLabel(vm.shutter)} ✕", 10.5f, Color.White)
                }
                if (vm.recording) Row(Modifier.align(Alignment.TopEnd).padding(9.dp).clip(RoundedCornerShape(5.dp)).background(Color(0x99000000)).padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).alpha(if ((now / 500) % 2 == 0L) 1f else 0.25f).background(s.danger, CircleShape)); Spacer(Modifier.width(6.dp))
                    Mono(recTime(vm, now), 10.5f, Color.White)
                }
                if (vm.countdown > 0) androidx.compose.material3.Text("${vm.countdown}", Modifier.align(Alignment.Center), color = Color.White, fontSize = 110.sp, fontFamily = Grotesk, fontWeight = FontWeight.Bold)
                else (vm.multiProgress ?: if (vm.busy && !vm.recording) "DEVELOPING" else null)?.let { Mono(it, 13f, Color.White, Modifier.align(Alignment.Center)) }
            }
        }
        // ---- keys with status lights
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Key(s, "FLASH", vm.flashOn, Modifier.weight(1f)) { vm.toggleFlash() }
            Key(s, if (vm.timer > 0) "${vm.timer} SEC" else "TIMER", vm.timer > 0, Modifier.weight(1f)) { vm.cycleTimer() }
            Key(s, vm.ratio.label, false, Modifier.weight(1f), enabled = r.ratios.size > 1) { vm.cycleRatio() }
            Key(s, "LENS", vm.acc.anyLens, Modifier.weight(1f).testTag("lens"), onClick = host.openLens)
            Key(s, "AUTO", vm.dirty, Modifier.weight(1f)) { vm.auto() }
            Key(s, "MENU", false, Modifier.weight(1f).testTag("settings"), onClick = host.openSettings)
        }
        // ---- bench: light meter and thumbwheel
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp).height(64.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Meter(vm, s, Modifier.width(92.dp).fillMaxHeight())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    params.forEach { p ->
                        val sel = p.key == param.key
                        Box(Modifier.weight(1f).pressable({ pkey = p.key }).height(22.dp).clip(RoundedCornerShape(6.dp)).background(if (sel) s.ink else s.faint.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                            Engraved(p.short, 7.5f, if (sel) s.bg else s.dim)
                            if (p.isSet) Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(4.dp).background(Orange, CircleShape))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dial(param, Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(8.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF111111), Color(0xFF3C3C3C), Color(0xFF111111), Color(0xFF262626)))))
                    Box(Modifier.pressable({ param.reset() }).size(66.dp, 34.dp).lcd(), contentAlignment = Alignment.Center) { Mono(param.value, 14f, LcdInk) }
                }
            }
        }
        // ---- camera drum
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).height(62.dp).clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(0f to s.faint, 0.22f to Color.Transparent, 0.78f to Color.Transparent, 1f to s.faint))) {
                CameraSnapRow(vm, list, 66.dp, Modifier.fillMaxSize(), onCentre = { hover = it.name }) { c, centred, _ ->
                    val k by animateFloatAsState(if (centred) 1.08f else 0.78f, spring(dampingRatio = 0.55f), label = "drum")
                    Box(Modifier.graphicsLayer { scaleX = k; scaleY = k; alpha = if (centred) 1f else 0.5f }) { CameraIcon(c, width = 58.dp) }
                }
            }
            KIconButton(Icons.Outlined.PhotoCamera, "All cameras", size = 46.dp, tag = "cams", onClick = host.openCameras)
        }
        // ---- dock: print slot, orange shutter, flip
        Row(Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            PrintSlot(vm, host.openGallery)
            Box(Modifier.size(92.dp).drawBehind {
                drawCircle(if (vm.slowShutter) Orange else s.faint, size.minDimension / 2 + 4.dp.toPx()); drawCircle(Well)
            }, contentAlignment = Alignment.Center) {
                val working = vm.busy && !vm.recording && vm.countdown == 0
                Box(Modifier.testTag("shutter").pressable(host.shutter, haptic = false).size(76.dp).offset(y = (-3).dp).shadow(6.dp, CircleShape)
                    .drawBehind { drawCircle(Color(0xFFA9340A), center = Offset(center.x, center.y + 5.dp.toPx())) }
                    .clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFFF8A55), Orange, Color(0xFFD9430D)), center = Offset(100f, 60f), radius = 230f)).alpha(if (working) 0.55f else 1f),
                    contentAlignment = Alignment.Center) {
                    val d by animateFloatAsState(if (vm.recording) 24f else if (r.isVideo) 18f else 0f, spring(dampingRatio = 0.6f), label = "mark")
                    Box(Modifier.size(d.dp).clip(RoundedCornerShape(if (vm.recording) 5.dp else 12.dp)).background(Color.White))
                }
            }
            KIconButton(Icons.Outlined.Cameraswitch, "Flip camera", size = 54.dp) { vm.flipCamera() }
        }
    }
}

@Composable
private fun Key(s: Skin, text: String, on: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Column(modifier.alpha(if (enabled) 1f else 0.45f).pressable(onClick, enabled).height(40.dp).keySurface(s), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(5.dp).background(if (on) Orange else s.faint, CircleShape))
        Engraved(text, 7.5f, s.ink, Modifier.padding(top = 4.dp))
    }
}

/** An analogue needle that swings with the light in front of the lens (plus your exposure setting). */
@Composable
private fun Meter(vm: CamViewModel, s: Skin, modifier: Modifier) {
    val ev by animateFloatAsState(vm.meterEv.coerceIn(-2.2f, 2.2f), spring(dampingRatio = 0.45f, stiffness = 120f), label = "needle")
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(Brush.verticalGradient(listOf(Color(0xFFFBFAF6), Color(0xFFECEBE4))))) {
        Canvas(Modifier.fillMaxSize()) {
            val pivot = Offset(size.width / 2f, size.height * 0.9f)
            val rad = size.height * 0.68f
            fun at(deg: Float, rr: Float) = Offset(pivot.x + cos(Math.toRadians(deg - 90.0)).toFloat() * rr, pivot.y + sin(Math.toRadians(deg - 90.0)).toFloat() * rr)
            drawArc(Color(0xFF222222), 180f + 34f, 112f, false, Offset(pivot.x - rad, pivot.y - rad), Size(rad * 2, rad * 2), style = Stroke(1.2.dp.toPx()))
            drawArc(Color(0xFFE3311F), 270f + 28f, 28f, false, Offset(pivot.x - rad, pivot.y - rad), Size(rad * 2, rad * 2), style = Stroke(3.dp.toPx()))
            for (i in -4..4) drawLine(Color(0xFF222222), at(i * 14f, rad), at(i * 14f, rad - (if (i % 2 == 0) 6f else 3.5f).dp.toPx()), 1.dp.toPx())
            drawLine(Color(0xFFE3311F), pivot, at(ev * 28f, rad - 2.dp.toPx()), 1.6.dp.toPx(), StrokeCap.Round)
            drawCircle(Color(0xFF222222), 4.dp.toPx(), pivot)
        }
        Mono("−", 9f, Color(0xFF333333), Modifier.align(Alignment.BottomStart).padding(start = 7.dp, bottom = 12.dp))
        Mono("+", 9f, Color(0xFF333333), Modifier.align(Alignment.BottomEnd).padding(end = 7.dp, bottom = 12.dp))
        Engraved("LIGHT", 5.5f, Color(0xFF777777), Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp).offset(x = 0.dp, y = 0.dp).alpha(0f))
    }
}

/** The last shot comes out of a slot as a small print. */
@Composable
private fun PrintSlot(vm: CamViewModel, onClick: () -> Unit) {
    val t = vm.lastThumb
    val out by animateFloatAsState(if (t != null) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 90f), label = "print")
    Box(Modifier.pressable(onClick).size(54.dp, 62.dp)) {
        Box(Modifier.align(Alignment.TopCenter).padding(top = 3.dp).size(44.dp, 54.dp).graphicsLayer { translationY = -(1f - out) * 40.dp.toPx(); clip = false; alpha = out }
            .shadow(3.dp).background(Color(0xFFF6F3EA)).padding(start = 3.dp, end = 3.dp, top = 3.dp, bottom = 9.dp)) {
            if (t != null) Image(t.asImageBitmap(), "Last shot", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(Modifier.align(Alignment.TopCenter).size(54.dp, 7.dp).recess(4))
    }
}

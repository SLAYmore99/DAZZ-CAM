package com.funcam.app.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funcam.app.model.FPS_VALUES
import com.funcam.app.model.ISO_VALUES
import com.funcam.app.model.SHUTTER_SPEEDS
import com.funcam.app.model.shutterLabel
import com.funcam.app.ui.CamViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

/** One adjustable value on the dial. All values are stepped, so the wheel has real detents. */
class Param(
    val key: String, val short: String, val name: String, val count: Int, val major: Int,
    val index: () -> Int, val set: (Int) -> Unit, val label: (Int) -> String,
    /** Index of the automatic / neutral position. */
    val neutral: Int,
    /** Text stops (shutter speeds, ISO) instead of a ruler. */
    val stops: Boolean = false,
) {
    val isSet: Boolean get() = index() != neutral
    val value: String get() = label(index())
    fun reset() = set(neutral)
}

fun evLabel(v: Float): String = (if (v > 0.04f) "+" else if (v < -0.04f) "−" else "±") + "%.1f".format(abs(v))

fun dialParams(vm: CamViewModel): List<Param> {
    fun ev(key: String, short: String, name: String, get: () -> Float, set: (Float) -> Unit) = Param(
        key, short, name, 41, 5, { ((get() + 2f) * 10f).roundToInt().coerceIn(0, 40) }, { set(it / 10f - 2f); vm.sync() },
        { evLabel(it / 10f - 2f) }, 20,
    )
    val shutters = listOf(0f) + SHUTTER_SPEEDS
    val zMin = vm.caps.minZoom
    val zMax = vm.caps.maxZoom.coerceAtMost(10f).coerceAtLeast(zMin)
    val zCount = ((zMax - zMin) * 10f).roundToInt() + 1
    return buildList {
        add(ev("ev", "EV", "Exposure", { vm.exposure }, { vm.exposure = it }))
        add(ev("wb", "WB", "Warmth", { vm.temp }, { vm.temp = it }))
        add(ev("tint", "TINT", "Tint", { vm.tint }, { vm.tint = it }))
        add(Param("s", "S", "Shutter", shutters.size, 1, { shutters.indexOf(vm.shutter).coerceAtLeast(0) }, { vm.setManualValues(shutter = shutters[it]) },
            { if (it == 0) "AUTO" else shutterLabel(shutters[it]) }, 0, stops = true))
        add(Param("iso", "ISO", "ISO", ISO_VALUES.size, 1, { ISO_VALUES.indexOf(vm.iso).coerceAtLeast(0) }, { vm.setManualValues(iso = ISO_VALUES[it]) },
            { if (it == 0) "AUTO" else ISO_VALUES[it].toString() }, 0, stops = true))
        if (zCount > 1) add(Param("zoom", "ZOOM", "Zoom", zCount, 10, { ((vm.zoom - zMin) * 10f).roundToInt().coerceIn(0, zCount - 1) },
            { vm.setZoomRatio(zMin + it / 10f) }, { "%.1f×".format(zMin + it / 10f) }, ((1f - zMin) * 10f).roundToInt().coerceIn(0, zCount - 1)))
        if (vm.recipe.isVideo) add(Param("fps", "FPS", "Frame rate", FPS_VALUES.size, 1, { FPS_VALUES.indexOf(vm.fps).coerceAtLeast(0) },
            { vm.setManualValues(fps = FPS_VALUES[it]) }, { FPS_VALUES[it].toString() }, FPS_VALUES.indexOf(vm.recipe.videoFps).coerceAtLeast(0), stops = true))
    }
}

/**
 * A ruler you drag sideways. It has momentum, settles on a stop and ticks under your finger.
 * Drawn as dots (DOT), fine lines (GLASS) or a knurled thumbwheel (FIELD).
 */
@Composable
fun Dial(param: Param, modifier: Modifier = Modifier) {
    val skin = LocalSkin.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val p by rememberUpdatedState(param)
    val gapPx = with(density) { (if (param.stops) 54.dp else 11.dp).toPx() }
    val pos = remember(param.key) { Animatable(param.index().toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var last by remember(param.key) { mutableIntStateOf(param.index()) }
    val measurer = rememberTextMeasurer()

    fun moved() {
        val i = pos.value.roundToInt().coerceIn(0, p.count - 1)
        if (i != last) {
            last = i
            if (p.stops || i % p.major == 0) haptics.thud() else haptics.tick()
            p.set(i)
        }
    }
    // follow changes made elsewhere (gestures on the viewfinder, reset, another control)
    val external = param.index()
    LaunchedEffect(external, param.key) {
        if (!dragging && !pos.isRunning && external != pos.value.roundToInt()) { last = external; pos.animateTo(external.toFloat(), spring(stiffness = 300f)) }
    }
    val drag = rememberDraggableState { d ->
        scope.launch { pos.snapTo((pos.value - d / gapPx).coerceIn(0f, (p.count - 1).toFloat())); moved() }
    }
    Canvas(
        modifier.draggable(
            drag, Orientation.Horizontal,
            onDragStarted = { dragging = true; pos.stop() },
            onDragStopped = { v ->
                dragging = false
                scope.launch {
                    pos.updateBounds(0f, (p.count - 1).toFloat())
                    pos.animateDecay(-v / gapPx, exponentialDecay(frictionMultiplier = 2.2f)) { moved() }
                    pos.animateTo(pos.value.roundToInt().toFloat(), spring(dampingRatio = 0.7f, stiffness = 500f)) { moved() }
                }
            },
        ),
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val from = (pos.value - cx / gapPx).toInt().coerceAtLeast(0)
        val to = (pos.value + cx / gapPx).toInt().plus(1).coerceAtMost(param.count - 1)
        if (skin.kind == Kind.FIELD) {
            // thumbwheel grooves travel with the finger and curve away at the edges
            val pitch = 5.dp.toPx()
            var x = -((pos.value * gapPx) % pitch)
            while (x < w) {
                val k = cos(((x - cx) / (w / 2f)) * 1.35f).coerceAtLeast(0f)
                drawRect(Color(0xFFBBBBBB).copy(alpha = k * 0.9f), Offset(x, 4.dp.toPx()), Size(1.4.dp.toPx() * k.coerceAtLeast(0.3f), h - 8.dp.toPx()))
                x += pitch
            }
        }
        for (i in from..to) {
            val x = cx + (i - pos.value) * gapPx
            val fade = (1f - abs(x - cx) / (cx * 1.02f)).coerceIn(0f, 1f)
            val big = param.stops || i % param.major == 0
            val near = abs(i - pos.value) < 0.5f
            val c = (if (near) skin.accent else if (big) (if (skin.kind == Kind.FIELD) Color.White else skin.ink) else skin.dim).let { it.copy(alpha = it.alpha * fade) }
            if (param.stops) {
                val style = TextStyle(color = c, fontSize = if (near) 15.sp else 12.sp, fontFamily = if (skin.kind == Kind.DOT) skin.display else skin.mono,
                    fontWeight = FontWeight.Bold)
                val t = measurer.measure(param.label(i), style)
                drawText(t, topLeft = Offset(x - t.size.width / 2f, (h - t.size.height) / 2f))
            } else when (skin.kind) {
                Kind.DOT -> drawCircle(c, (if (big) 2.1f else 1.2f).dp.toPx() + if (near) 1.dp.toPx() else 0f, Offset(x, h * 0.5f))
                Kind.GLASS -> {
                    val len = if (big) h * 0.42f else h * 0.24f
                    drawRect(c, Offset(x - (if (near) 1f else 0.6f).dp.toPx(), h * 0.55f - len / 2f), Size((if (near) 2f else 1.2f).dp.toPx(), len))
                }
                Kind.FIELD -> if (big) drawRect(c, Offset(x - 0.8.dp.toPx(), h * 0.2f), Size(1.6.dp.toPx(), h * 0.6f))
            }
        }
        if (skin.kind == Kind.FIELD) drawRect(skin.accent, Offset(cx - 1.2.dp.toPx(), 2.dp.toPx()), Size(2.4.dp.toPx(), h - 4.dp.toPx()))
        else if (!param.stops) {
            val t = 4.dp.toPx()
            drawPath(Path().apply { moveTo(cx - t, 0f); lineTo(cx + t, 0f); lineTo(cx, t * 1.4f); close() }, skin.accent)
        }
    }
}

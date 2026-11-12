package com.funcam.app.ui.kit

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.funcam.app.model.Recipe
import com.funcam.app.ui.CamViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** What the reticle slot needs to draw itself. */
class FocusMark(val locked: Boolean, val adjusting: Boolean, val exposure: Float)

/**
 * The live viewfinder at the exact output frame, with the shared gestures:
 * tap = focus, press and hold = lock focus and exposure, drag up/down = exposure, swipe sideways = next camera, pinch = zoom.
 * The SurfaceView cannot be clipped, so rounded corners are painted over it in [maskColor].
 */
@Composable
fun Viewfinder(
    vm: CamViewModel, modifier: Modifier, corner: Dp, maskColor: Color, onSwipe: (Int) -> Unit,
    reticle: @Composable (FocusMark) -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
) {
    val spec = vm.frameSpec
    val haptics = LocalHapticFeedback.current
    val skin = LocalSkin.current
    var focus by remember { mutableStateOf<Offset?>(null) }
    var focusTick by remember { mutableIntStateOf(0) }
    var adjusting by remember { mutableStateOf(false) }
    LaunchedEffect(focusTick, vm.focusLocked, adjusting) { if (focus != null && !vm.focusLocked && !adjusting) { delay(2600); focus = null } }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val a = spec.outAspect
        val (w, h) = if (maxWidth / maxHeight > a) (maxHeight * a) to maxHeight else maxWidth to (maxWidth / a)
        Box(
            Modifier.size(w, h).testTag("viewfinder").pointerInput(spec) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var mode = 0            // 0 undecided, 1 exposure, 2 swipe, 3 pinch
                    var total = Offset.Zero
                    var held = false
                    val ev0 = vm.exposure
                    val z0 = vm.zoom
                    var zoom = 1f
                    fun picture(o: Offset): Offset? {
                        val x = o.x / size.width
                        val yUp = 1f - o.y / size.height
                        val r = spec.rect
                        val px = (x - r[0]) / (r[2] - r[0])
                        val py = 1f - (yUp - r[1]) / (r[3] - r[1])
                        return if (px in 0f..1f && py in 0f..1f) Offset(px, py) else null
                    }
                    while (true) {
                        val e = if (mode == 0 && !held) withTimeoutOrNull(420) { awaitPointerEvent() } else awaitPointerEvent()
                        if (e == null) {
                            // press and hold: lock focus and exposure on this spot
                            held = true
                            picture(down.position)?.let { p -> focus = down.position; focusTick++; vm.tapFocus(p.x, p.y, lock = true); haptics.thud() }
                            continue
                        }
                        val pressed = e.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size >= 2) {
                            mode = 3; zoom *= e.calculateZoom(); vm.setZoomRatio(z0 * zoom); e.changes.forEach { it.consume() }
                            continue
                        }
                        if (mode == 3 || held) continue
                        total += pressed[0].positionChange()
                        if (mode == 0 && total.getDistance() > slop) mode = if (abs(total.y) > abs(total.x)) 1 else 2
                        if (mode == 1) {
                            val v = ((ev0 - total.y / 110.dp.toPx()).coerceIn(-2f, 2f) * 10f).roundToInt() / 10f
                            if (v != vm.exposure) { if (v == 0f) haptics.thud() else haptics.tick(); vm.exposure = v; vm.sync() }
                            if (focus == null) focus = down.position
                            adjusting = true
                            pressed[0].consume()
                        }
                    }
                    adjusting = false
                    if (mode == 0 && !held) picture(down.position)?.let { p ->
                        focus = down.position; focusTick++; vm.tapFocus(p.x, p.y); haptics.tick()
                    }
                    if (mode == 1) focusTick++
                    if (mode == 2 && abs(total.x) > 56.dp.toPx()) onSwipe(if (total.x < 0) 1 else -1)
                }
            },
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    SurfaceView(c).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {}
                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { vm.renderer.setDisplay(holder.surface, width, height) }
                            override fun surfaceDestroyed(holder: SurfaceHolder) { vm.renderer.setDisplay(null, 0, 0) }
                        })
                    }
                },
            )
            Canvas(Modifier.fillMaxSize()) {
                // rounded corners, painted in the surrounding colour
                val r = corner.toPx()
                drawPath(Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
                }, maskColor)
                val pr = spec.rect
                val x0 = pr[0] * size.width; val x1 = pr[2] * size.width
                val y0 = (1 - pr[3]) * size.height; val y1 = (1 - pr[1]) * size.height
                if (vm.grid) {
                    val c = Color.White.copy(alpha = 0.4f)
                    for (i in 1..2) {
                        val x = x0 + (x1 - x0) * i / 3f; val y = y0 + (y1 - y0) * i / 3f
                        drawLine(c, Offset(x, y0), Offset(x, y1), 1.dp.toPx()); drawLine(c, Offset(x0, y), Offset(x1, y), 1.dp.toPx())
                    }
                }
                if (vm.levelOn) {
                    val cx = (x0 + x1) / 2f; val cy = (y0 + y1) / 2f
                    val flat = abs(vm.tilt) < 1f
                    val len = 46.dp.toPx()
                    drawLine(Color.White.copy(alpha = 0.6f), Offset(cx - len * 1.7f, cy), Offset(cx - len * 1.15f, cy), 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.6f), Offset(cx + len * 1.15f, cy), Offset(cx + len * 1.7f, cy), 1.dp.toPx())
                    val t = Math.toRadians(vm.tilt.toDouble())
                    val dx = (kotlin.math.cos(t) * len).toFloat(); val dy = (kotlin.math.sin(t) * len).toFloat()
                    drawLine(if (flat) skin.accent else Color.White, Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy), (if (flat) 2.5f else 1.5f).dp.toPx())
                }
            }
            focus?.let { f ->
                val pop = remember(focusTick) { Animatable(1.5f) }
                LaunchedEffect(focusTick) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f)) }
                Box(Modifier.offset { IntOffset(f.x.roundToInt(), f.y.roundToInt()) }.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
                    reticle(FocusMark(vm.focusLocked, adjusting, vm.exposure))
                }
            }
            overlay()
        }
    }
}

/** Drives a shutter effect: returns 1 → 0 each time a picture is taken. */
@Composable
fun shutterPulse(vm: CamViewModel, millis: Int = 260): Float {
    val a = remember { Animatable(0f) }
    LaunchedEffect(vm.flashBlink) { if (vm.flashBlink > 0) { a.snapTo(1f); a.animateTo(0f, tween(millis)) } }
    return a.value
}

/**
 * A row of cameras that snaps one to the centre. The centred one becomes the camera when the row settles,
 * each step ticks, and the row follows changes made elsewhere.
 */
@Composable
fun CameraSnapRow(
    vm: CamViewModel, items: List<Recipe>, itemWidth: Dp, modifier: Modifier = Modifier,
    onCentre: (Recipe) -> Unit = {},
    item: @Composable (r: Recipe, centred: Boolean, distance: () -> Float) -> Unit,
) {
    if (items.isEmpty()) return
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val start = items.indexOfFirst { it.id == vm.recipe.id }.coerceAtLeast(0)
    val state = remember(items) { androidx.compose.foundation.lazy.LazyListState(start) }
    val centre by remember(items) {
        derivedStateOf {
            val info = state.layoutInfo
            val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index ?: start
        }
    }
    LaunchedEffect(centre) { if (state.isScrollInProgress) haptics.tick(); items.getOrNull(centre)?.let(onCentre) }
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) items.getOrNull(centre)?.let { if (it.id != vm.recipe.id) vm.selectRecipe(it) }
    }
    LaunchedEffect(vm.recipe.id) {
        val target = items.indexOfFirst { it.id == vm.recipe.id }
        if (target >= 0 && target != centre && !state.isScrollInProgress) state.animateScrollToItem(target)
    }
    BoxWithConstraints(modifier) {
        val pad = ((maxWidth - itemWidth) / 2).coerceAtLeast(0.dp)
        LazyRow(state = state, flingBehavior = rememberSnapFlingBehavior(state), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = pad), verticalAlignment = Alignment.CenterVertically) {
            items(items.size, key = { items[it].id }) { i ->
                val r = items[i]
                Box(Modifier.width(itemWidth).pressable({ scope.launch { state.animateScrollToItem(i) } }, haptic = false), contentAlignment = Alignment.Center) {
                    item(r, i == centre) {
                        val info = state.layoutInfo
                        val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                        val it2 = info.visibleItemsInfo.firstOrNull { it.index == i }
                        if (it2 == null) 1f else ((it2.offset + it2.size / 2f - mid) / (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1) * 2f).coerceIn(-1f, 1f)
                    }
                }
            }
        }
    }
}

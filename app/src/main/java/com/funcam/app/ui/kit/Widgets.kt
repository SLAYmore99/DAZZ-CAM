package com.funcam.app.ui.kit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val springy = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)

/** A row of dots, the DOT edition's divider and slider track. */
fun androidx.compose.ui.graphics.drawscope.DrawScope.dotLine(color: Color, y: Float, from: Float = 0f, to: Float = size.width, pitch: Float = 6.dp.toPx(), r: Float = 1.dp.toPx()) {
    var x = from + pitch / 2f
    while (x < to) { drawCircle(color, r, Offset(x, y)); x += pitch }
}

/** Press feedback: the element shrinks slightly (DOT, GLASS) or travels down like a key (FIELD). */
@Composable
fun Modifier.pressable(onClick: () -> Unit, enabled: Boolean = true, haptic: Boolean = true): Modifier {
    val skin = LocalSkin.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val k by animateFloatAsState(if (pressed) 1f else 0f, if (skin.kind == Kind.FIELD) tween(60) else springy, label = "press")
    val h = LocalHapticFeedback.current
    return this.graphicsLayer {
        if (skin.kind == Kind.FIELD) translationY = k * 2.5.dp.toPx() else { scaleX = 1f - 0.1f * k; scaleY = 1f - 0.1f * k }
    }.clickable(src, null, enabled = enabled) { if (haptic) h.tick(); onClick() }
}

/** The raised key surface of the FIELD edition. */
fun Modifier.keySurface(skin: Skin, shape: Shape = RoundedCornerShape(11.dp), on: Boolean = false): Modifier = this
    .shadow(if (on) 0.dp else 5.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
    .drawBehind { if (!on) drawRoundRect(skin.line, Offset(0f, 2.5.dp.toPx()), size, CornerRadius(11.dp.toPx())) }
    .clip(shape)
    .background(if (on) Brush.verticalGradient(listOf(skin.faint, skin.card)) else Brush.verticalGradient(listOf(lerp(skin.card, Color.White, if (skin.dark) 0.08f else 0.6f), skin.card)))

/** Translucent glass surface of the GLASS edition. */
fun Modifier.glass(shape: Shape, tint: Color = Color.White.copy(alpha = 0.14f)): Modifier = this
    .clip(shape).background(tint)
    .border(0.6.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.08f))), shape)

@Composable
fun KSwitch(on: Boolean) {
    val s = LocalSkin.current
    val k by animateFloatAsState(if (on) 1f else 0f, springy, label = "sw")
    when (s.kind) {
        Kind.DOT -> Box(Modifier.size(46.dp, 26.dp).clip(CircleShape).background(lerp(Color.Transparent, s.ink, k)).border(1.dp, lerp(s.dim, s.ink, k), CircleShape)) {
            Box(Modifier.offset { IntOffset((5 + 20 * k).dp.roundToPx(), 5.dp.roundToPx()) }.size(16.dp).background(lerp(s.dim, s.accent, k), CircleShape))
        }
        Kind.GLASS -> Box(Modifier.size(51.dp, 31.dp).clip(CircleShape).background(lerp(Color(0x66787880), Color(0xFF30D158), k))) {
            Box(Modifier.offset { IntOffset((2 + 20 * k).dp.roundToPx(), 2.dp.roundToPx()) }.size(27.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape))
        }
        Kind.FIELD -> Box(Modifier.size(46.dp, 24.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF151515))) {
            Box(Modifier.offset { IntOffset((2 + 20 * k).dp.roundToPx(), 2.dp.roundToPx()) }.size(22.dp, 20.dp).clip(RoundedCornerShape(4.dp))
                .background(Brush.verticalGradient(if (on) listOf(Color(0xFFFF8A55), s.accent) else listOf(Color(0xFFFAFAFA), Color(0xFFC9C9C9)))))
        }
    }
}

/** A setting with a switch. GLASS groups rows into rounded cards; the others separate them with a rule. */
@Composable
fun ToggleRow(title: String, sub: String?, on: Boolean, first: Boolean = false, last: Boolean = false, trailing: (@Composable () -> Unit)? = null, onChange: (Boolean) -> Unit) {
    val s = LocalSkin.current
    val h = LocalHapticFeedback.current
    val shape = RoundedCornerShape(if (first) 18.dp else 0.dp, if (first) 18.dp else 0.dp, if (last) 18.dp else 0.dp, if (last) 18.dp else 0.dp)
    Row(
        Modifier.fillMaxWidth().padding(bottom = if (s.kind == Kind.GLASS) 1.dp else 0.dp)
            .then(if (s.kind == Kind.GLASS) Modifier.clip(shape).background(s.card) else Modifier)
            .clickable { h.tick(); onChange(!on) }
            .drawBehind {
                if (s.kind == Kind.DOT) dotLine(s.line, size.height - 1f)
                if (s.kind == Kind.FIELD) drawRect(s.line, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
            }
            .padding(horizontal = if (s.kind == Kind.GLASS) 16.dp else 0.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            if (s.kind == Kind.DOT) Label(title, color = s.ink, size = 12.sp)
            else Body(title, color = s.ink, size = if (s.kind == Kind.GLASS) 16.sp else 14.sp, maxLines = 1)
            if (sub != null) Body(sub, size = if (s.kind == Kind.DOT) 10.sp else 12.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 2)
        }
        if (trailing != null) trailing() else KSwitch(on)
    }
}

@Composable
fun KSection(title: String, trailing: String? = null) {
    val s = LocalSkin.current
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 9.dp, start = if (s.kind == Kind.GLASS) 6.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
        if (s.kind == Kind.FIELD) { Box(Modifier.size(5.dp).background(s.accent, CircleShape)); Spacer(Modifier.width(8.dp)) }
        Label(title, size = if (s.kind == Kind.GLASS) 13.sp else 9.5.sp, weight = if (s.kind == Kind.GLASS) FontWeight.SemiBold else FontWeight.Bold)
        if (s.kind == Kind.DOT) Canvas(Modifier.weight(1f).padding(horizontal = 10.dp).height(2.dp)) { dotLine(s.faint, size.height / 2f) } else Spacer(Modifier.weight(1f))
        if (trailing != null) Label(trailing, color = if (s.kind == Kind.DOT) s.accent else s.dim, size = 9.5.sp)
    }
}

/** A choice chip. */
@Composable
fun KPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val s = LocalSkin.current
    val base = Modifier.pressable(onClick)
    when (s.kind) {
        Kind.DOT -> Box(base.height(34.dp).clip(CircleShape).background(if (selected) s.ink else Color.Transparent).border(1.dp, if (selected) s.ink else s.faint, CircleShape)
            .padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Display(text, color = if (selected) s.bg else s.dim, size = 15.sp) }
        Kind.GLASS -> Box(base.height(36.dp).clip(CircleShape).background(if (selected) s.accent else s.card).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Body(text, color = if (selected) s.onAccent else s.ink, size = 14.sp, maxLines = 1)
        }
        Kind.FIELD -> Box(base.height(36.dp).then(if (selected) Modifier.clip(RoundedCornerShape(9.dp)).background(s.accent) else Modifier.keySurface(s, RoundedCornerShape(9.dp)))
            .padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text(text, color = if (selected) Color.White else s.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = s.mono)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PillRow(content: @Composable () -> Unit) {
    FlowRow(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

/** A lens / effect tile with a status light. */
@Composable
fun KTile(title: String, sub: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val s = LocalSkin.current
    val shape = RoundedCornerShape(when (s.kind) { Kind.DOT -> 16.dp; Kind.GLASS -> 24.dp; Kind.FIELD -> 11.dp })
    val fg = when (s.kind) { Kind.DOT -> if (on) s.bg else s.ink; Kind.GLASS -> if (on) s.onAccent else s.ink; Kind.FIELD -> s.ink }
    val surface = when (s.kind) {
        Kind.DOT -> Modifier.clip(shape).background(if (on) s.ink else Color.Transparent).border(1.dp, if (on) s.ink else s.line, shape)
        Kind.GLASS -> if (on) Modifier.clip(shape).background(s.accent) else Modifier.glass(shape, s.card)
        Kind.FIELD -> Modifier.keySurface(s, shape, on)
    }
    Column(modifier.pressable(onClick).height(96.dp).then(surface).padding(horizontal = 10.dp, vertical = 11.dp)) {
        when (s.kind) {
            Kind.GLASS -> Box(Modifier.size(20.dp).border(2.dp, if (on) s.onAccent else s.faint, CircleShape).padding(5.dp).background(if (on) s.onAccent else Color.Transparent, CircleShape))
            else -> Box(Modifier.size(6.dp).background(if (on) s.accent else s.faint, CircleShape))
        }
        Spacer(Modifier.weight(1f))
        if (s.kind == Kind.DOT) Display(title, color = fg, size = 17.sp)
        else Body(if (s.kind == Kind.FIELD) title.uppercase() else title, color = fg, size = if (s.kind == Kind.GLASS) 15.sp else 12.5.sp, maxLines = 1)
        Body(sub, color = fg.copy(alpha = 0.6f), size = 9.sp, maxLines = 2, modifier = Modifier.padding(top = 2.dp))
    }
}

/** A slider drawn in the skin's style. Drag anywhere on it; double-tap returns to [neutral]. */
@Composable
fun KSlider(value: Float, min: Float, max: Float, neutral: Float? = null, onChange: (Float) -> Unit) {
    val s = LocalSkin.current
    val h = LocalHapticFeedback.current
    val cur by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val pad = 14.dp
    fun at(x: Float, w: Float, padPx: Float) {
        var v = min + ((x - padPx) / (w - 2 * padPx)).coerceIn(0f, 1f) * (max - min)
        v = (v * 20f).roundToInt() / 20f
        if (neutral != null && kotlin.math.abs(v - neutral) < (max - min) * 0.03f) v = neutral
        if (v != cur) { if (v == neutral) h.thud(); change(v) }
    }
    Canvas(
        Modifier.fillMaxWidth().height(34.dp)
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { if (neutral != null) { h.thud(); change(neutral) } }, onTap = { at(it.x, size.width.toFloat(), pad.toPx()) }) }
            .pointerInput(Unit) { detectDragGestures(onDragStart = { at(it.x, size.width.toFloat(), pad.toPx()) }) { c, _ -> c.consume(); at(c.position.x, size.width.toFloat(), pad.toPx()) } },
    ) {
        val p = pad.toPx()
        val k = ((value - min) / (max - min)).coerceIn(0f, 1f)
        val x = p + k * (size.width - 2 * p)
        val y = size.height / 2f
        when (s.kind) {
            Kind.DOT -> {
                dotLine(s.faint, y, pitch = 7.dp.toPx(), r = 1.2.dp.toPx())
                dotLine(s.ink, y, 0f, x, 7.dp.toPx(), 1.5.dp.toPx())
                drawCircle(if (value == neutral) s.ink else s.accent, 9.dp.toPx(), Offset(x, y))
            }
            Kind.GLASS -> {
                drawRoundRect(Color(0x66787880), Offset(0f, y - 3.dp.toPx()), Size(size.width, 6.dp.toPx()), CornerRadius(3.dp.toPx()))
                drawRoundRect(s.accent, Offset(0f, y - 3.dp.toPx()), Size(x, 6.dp.toPx()), CornerRadius(3.dp.toPx()))
                drawCircle(Color.Black.copy(alpha = 0.25f), 14.dp.toPx(), Offset(x, y + 2.dp.toPx()))
                drawCircle(Color.White, 13.dp.toPx(), Offset(x, y))
            }
            Kind.FIELD -> {
                drawRoundRect(Color(0xFF151515), Offset(0f, y - 4.dp.toPx()), Size(size.width, 8.dp.toPx()), CornerRadius(4.dp.toPx()))
                drawRect(s.accent, Offset(3.dp.toPx(), y - 1.dp.toPx()), Size((x - 3.dp.toPx()).coerceAtLeast(0f), 2.dp.toPx()))
                val kw = 30.dp.toPx(); val kh = 28.dp.toPx()
                drawRoundRect(s.line, Offset(x - kw / 2, y - kh / 2 + 2.5.dp.toPx()), Size(kw, kh), CornerRadius(6.dp.toPx()))
                drawRoundRect(Brush.verticalGradient(listOf(lerp(s.card, Color.White, if (s.dark) 0.1f else 0.6f), s.card), y - kh / 2, y + kh / 2), Offset(x - kw / 2, y - kh / 2), Size(kw, kh), CornerRadius(6.dp.toPx()))
                drawRect(Color.Black.copy(alpha = 0.4f), Offset(x - 1.dp.toPx(), y - kh / 2 + 4.dp.toPx()), Size(2.dp.toPx(), kh - 8.dp.toPx()))
            }
        }
    }
}

/** Round icon button in the skin's style. */
@Composable
fun KIconButton(icon: ImageVector, desc: String, modifier: Modifier = Modifier, size: Dp = 40.dp, active: Boolean = false, tag: String? = null, onClick: () -> Unit) {
    val s = LocalSkin.current
    val surface = when (s.kind) {
        Kind.DOT -> Modifier.clip(CircleShape).border(1.dp, if (active) s.accent else s.faint, CircleShape)
        Kind.GLASS -> if (active) Modifier.clip(CircleShape).background(s.accent) else Modifier.glass(CircleShape)
        Kind.FIELD -> Modifier.keySurface(s, if (size > 50.dp) CircleShape else RoundedCornerShape(11.dp))
    }
    Box(modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier).pressable(onClick).size(size).then(surface), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = when { active && s.kind == Kind.GLASS -> s.onAccent; active -> s.accent; else -> s.ink }, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * Bottom sheet drawn inside the screen (not a dialog), so each edition fully styles it:
 * slides up on a spring, drag the header down or tap outside to close.
 */
@Composable
fun BoxScope.KSheet(visible: Boolean, onClose: () -> Unit, title: String, sub: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val s = LocalSkin.current
    BackHandler(visible) { onClose() }
    AnimatedVisibility(visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (s.kind == Kind.GLASS) 0.4f else 0.6f)).pointerInput(Unit) { detectTapGestures { onClose() } })
    }
    AnimatedVisibility(
        visible, Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(spring(dampingRatio = if (s.kind == Kind.GLASS) 0.78f else 0.9f, stiffness = 320f)) { it },
        exit = slideOutVertically(tween(220)) { it },
    ) {
        var drag by remember { mutableFloatStateOf(0f) }
        val inset = if (s.kind == Kind.GLASS) 6.dp else 0.dp
        val shape = if (s.kind == Kind.GLASS) RoundedCornerShape(s.sheetCorner) else RoundedCornerShape(s.sheetCorner, s.sheetCorner, 0.dp, 0.dp)
        Column(
            Modifier.padding(horizontal = inset).then(if (s.kind == Kind.GLASS) Modifier.navigationBarsPadding().padding(bottom = inset) else Modifier)
                .fillMaxWidth().fillMaxHeight(0.86f)
                .offset { IntOffset(0, drag.roundToInt().coerceAtLeast(0)) }
                .clip(shape).background(s.panel)
                .then(when (s.kind) {
                    Kind.GLASS -> Modifier.border(0.6.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.06f))), shape)
                    Kind.DOT -> Modifier.border(1.dp, s.line, shape)
                    Kind.FIELD -> Modifier
                })
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Column(
                Modifier.fillMaxWidth().pointerInput(Unit) {
                    detectVerticalDragGestures(onDragEnd = { if (drag > 90.dp.toPx()) onClose(); drag = 0f }, onDragCancel = { drag = 0f }) { _, d -> drag += d }
                }.padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 6.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)) {
                    when (s.kind) {
                        Kind.DOT -> Canvas(Modifier.size(34.dp, 4.dp)) { dotLine(s.dim, size.height / 2, pitch = 8.dp.toPx(), r = 1.6.dp.toPx()) }
                        Kind.GLASS -> Box(Modifier.size(38.dp, 5.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
                        Kind.FIELD -> Box(Modifier.size(44.dp, 6.dp).background(Color(0xFF151515), CircleShape))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Display(title, Modifier.weight(1f), size = when (s.kind) { Kind.DOT -> 34.sp; Kind.GLASS -> 26.sp; Kind.FIELD -> 22.sp })
                    KIconButton(Icons.Rounded.Close, "Close", size = 34.dp, onClick = onClose)
                }
                if (sub != null) Body(sub, Modifier.padding(top = 5.dp), size = if (s.kind == Kind.DOT) 10.sp else 12.5.sp, maxLines = 3)
            }
            if (s.kind == Kind.FIELD) Box(Modifier.fillMaxWidth().height(1.dp).background(s.line))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = if (s.kind == Kind.GLASS) 16.dp else 20.dp)
                .then(if (s.kind == Kind.GLASS) Modifier else Modifier.navigationBarsPadding()).padding(bottom = 24.dp)) { content() }
        }
    }
}

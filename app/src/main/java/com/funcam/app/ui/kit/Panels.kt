package com.funcam.app.ui.kit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funcam.app.model.Border
import com.funcam.app.model.Category
import com.funcam.app.model.FlashGel
import com.funcam.app.model.InstantFrame
import com.funcam.app.model.Recipe
import com.funcam.app.model.Tweak
import com.funcam.app.model.shutterLabel
import com.funcam.app.ui.CamViewModel
import com.funcam.app.ui.CameraIcon

/** A camera's look on the live scene; the drawn camera body stands in until the render arrives. */
@Composable
fun LookThumb(vm: CamViewModel, r: Recipe, modifier: Modifier = Modifier) {
    val bmp = vm.lookPreviews[r.id]
    Box(modifier.background(Color(r.bodyColor).copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
        if (bmp != null) Image(bmp.asImageBitmap(), r.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else CameraIcon(r, width = 46.dp)
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val s = LocalSkin.current
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).then(when (s.kind) {
            Kind.DOT -> Modifier.border(1.dp, s.faint, CircleShape).padding(3.dp)
            Kind.GLASS -> Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x47767680)).padding(2.dp)
            Kind.FIELD -> Modifier
        }),
        horizontalArrangement = Arrangement.spacedBy(if (s.kind == Kind.FIELD) 6.dp else 0.dp),
    ) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            val m = when (s.kind) {
                Kind.DOT -> Modifier.clip(CircleShape).background(if (sel) s.ink else Color.Transparent)
                Kind.GLASS -> Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) Color.White.copy(alpha = 0.26f) else Color.Transparent)
                Kind.FIELD -> if (sel) Modifier.clip(RoundedCornerShape(9.dp)).background(s.ink) else Modifier.keySurface(s, RoundedCornerShape(9.dp))
            }
            Box(Modifier.weight(1f).pressable({ onSelect(i) }).height(34.dp).then(m), contentAlignment = Alignment.Center) {
                Label(o, color = when { s.kind == Kind.GLASS -> s.ink; sel -> s.bg; else -> s.dim }, size = if (s.kind == Kind.GLASS) 13.sp else 10.sp, weight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BoxScope.CamerasPanel(vm: CamViewModel, visible: Boolean, onClose: () -> Unit) {
    val s = LocalSkin.current
    var video by remember(visible) { mutableStateOf(vm.isVideoMode) }
    LaunchedEffect(visible, video) { if (visible) vm.refreshLookPreviews(video) }
    KSheet(visible, onClose, "Cameras", "Each one is shown on your scene, live.") {
        Segmented(listOf("Photo", "Video"), if (video) 1 else 0) { video = it == 1 }
        val list = vm.cameras(video)
        val groups = listOf("★ Favourites" to list.filter { it.id in vm.favourites }) + Category.entries.map { c -> c.label to list.filter { it.category == c } }
        for ((name, items) in groups) {
            if (items.isEmpty()) continue
            KSection(name, items.size.toString().padStart(2, '0'))
            items.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { i, r ->
                        val sel = r.id == vm.recipe.id
                        val fav = r.id in vm.favourites
                        Column(Modifier.weight(1f).pressable({ vm.selectRecipe(r); onClose() })) {
                            val shape = RoundedCornerShape(when (s.kind) { Kind.DOT -> 14.dp; Kind.GLASS -> 20.dp; Kind.FIELD -> 3.dp })
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(0.8f)
                                    .then(if (s.kind == Kind.FIELD) Modifier.rotate(if (sel) 0f else (i - 1) * 1.1f).shadow(4.dp, shape).background(Color(0xFFF6F3EA)).padding(4.dp) else Modifier)
                                    .then(if (sel) Modifier.border(if (s.kind == Kind.DOT) 1.5.dp else 2.5.dp, if (s.kind == Kind.DOT) s.ink else s.accent, shape) else Modifier)
                                    .clip(shape),
                            ) {
                                LookThumb(vm, r, Modifier.fillMaxSize())
                                Box(Modifier.align(Alignment.TopEnd).pressable({ vm.toggleFavourite(r.id) }).size(34.dp), contentAlignment = Alignment.Center) {
                                    Icon(if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Favourite", tint = if (fav) s.accent else Color.White.copy(alpha = 0.85f), modifier = Modifier.size(17.dp))
                                }
                                if (r.v4) Box(Modifier.align(Alignment.BottomStart).padding(6.dp).background(s.accent, RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                    androidx.compose.material3.Text("NEW", color = s.onAccent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Label(r.name, Modifier.padding(top = 7.dp), color = if (sel && s.kind != Kind.DOT) s.accent else s.ink, size = if (s.kind == Kind.GLASS) 12.5.sp else 10.5.sp, weight = FontWeight.Bold)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private val LENSES = listOf(
    Triple("mist", "Mist", "Soft glowing highlights, gentler contrast"), Triple("streak", "Streak", "Blue anamorphic flares through lights"),
    Triple("star", "Star", "Starbursts on bright lights"), Triple("prism", "Prism", "Rainbow ghost reflections"),
    Triple("fishW", "Fisheye", "Bulging ultra-wide"), Triple("fishC", "Circle", "Circular fisheye in black"),
    Triple("nd", "ND", "Darker, for slow shutter in daylight"), Triple("double", "Double", "Two shots in one frame"),
    Triple("leaks", "Leaks", "Warm light leaks on film cameras"),
)

@Composable
fun BoxScope.LensPanel(vm: CamViewModel, visible: Boolean, onClose: () -> Unit) {
    val s = LocalSkin.current
    KSheet(visible, onClose, "Lenses", "Stack these on any camera.") {
        val a = vm.acc
        Spacer(Modifier.height(10.dp))
        LENSES.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { (k, name, sub) ->
                    val on = when (k) { "mist" -> a.mist; "streak" -> a.streak; "star" -> a.star; "prism" -> a.prism; "fishW" -> a.fisheyeW; "fishC" -> a.fisheyeC; "nd" -> a.nd; "double" -> a.doubleExposure; else -> a.lightLeaks }
                    KTile(name, sub, on, Modifier.weight(1f)) {
                        vm.updateAcc(when (k) {
                            "mist" -> a.copy(mist = !on); "streak" -> a.copy(streak = !on); "star" -> a.copy(star = !on); "prism" -> a.copy(prism = !on)
                            "fishW" -> a.copy(fisheyeW = !on, fisheyeC = false); "fishC" -> a.copy(fisheyeC = !on, fisheyeW = false)
                            "nd" -> a.copy(nd = !on); "double" -> a.copy(doubleExposure = !on); else -> a.copy(lightLeaks = !on)
                        })
                    }
                }
            }
        }
        KSection("Flash colour")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FlashGel.entries.forEach { g ->
                Box(Modifier.pressable({ vm.updateAcc(a.copy(gel = g)); if (!vm.flashOn) vm.toggleFlash() }).size(36.dp)
                    .then(if (a.gel == g) Modifier.border(2.dp, if (s.kind == Kind.FIELD) s.accent else s.ink, CircleShape).padding(4.dp) else Modifier)
                    .background(Color(g.color.x, g.color.y, g.color.z), CircleShape))
            }
        }
    }
}

@Composable
fun BoxScope.TunePanel(vm: CamViewModel, visible: Boolean, onClose: () -> Unit) {
    val s = LocalSkin.current
    val r = vm.recipe
    val t = vm.tweak
    KSheet(visible, onClose, r.name, r.description) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { KSection("Look") }
            Box(Modifier.pressable({ vm.updateTweak(Tweak()) }).padding(start = 12.dp, top = 10.dp)) { Label("Reset", color = if (t.isDefault) s.faint else s.accent, size = if (s.kind == Kind.GLASS) 14.sp else 10.sp, weight = FontWeight.Bold) }
        }
        val items = listOf<Triple<String, Float, (Float) -> Tweak>>(
            Triple("Intensity", t.intensity) { t.copy(intensity = it) }, Triple("Grain", t.grain) { t.copy(grain = it) }, Triple("Leaks", t.leaks) { t.copy(leaks = it) },
            Triple("Glow", t.glow) { t.copy(glow = it) }, Triple("Vignette", t.vignette) { t.copy(vignette = it) }, Triple("Softness", t.soft) { t.copy(soft = it) },
        )
        items.forEachIndexed { i, (name, v, set) ->
            val first = i == 0; val last = i == items.lastIndex
            Column(
                if (s.kind == Kind.GLASS) Modifier.padding(bottom = 1.dp).clip(RoundedCornerShape(if (first) 18.dp else 0.dp, if (first) 18.dp else 0.dp, if (last) 18.dp else 0.dp, if (last) 18.dp else 0.dp))
                    .background(s.card).padding(horizontal = 16.dp, vertical = 8.dp) else Modifier.padding(vertical = 6.dp),
            ) {
                Row {
                    if (s.kind == Kind.GLASS) Body(name, Modifier.weight(1f), color = s.ink, size = 14.sp) else Label(name, Modifier.weight(1f))
                    if (s.kind == Kind.DOT) Display("${(v * 100).toInt()}%", size = 16.sp) else Label("${(v * 100).toInt()}%", size = 12.sp)
                }
                KSlider(v, 0f, if (i == 0) 1.5f else 2f, 1f) { vm.updateTweak(set(it)) }
            }
        }
        if (r.ratios.size > 1) {
            KSection("Frame shape")
            PillRow { r.ratios.forEachIndexed { i, ratio -> KPill(ratio.label, i == vm.ratioIndex.coerceIn(0, r.ratios.lastIndex)) { vm.setRatio(i) } } }
        }
        KSection("Shutter")
        PillRow { listOf(0f, 1 / 15f, 1 / 8f, 1 / 4f, 1 / 2f, 1f).forEach { sp -> KPill(if (sp == 0f) "AUTO" else shutterLabel(sp), vm.shutter == sp) { vm.setManualValues(shutter = sp) } } }
        Body("A slow shutter smears motion into trails. It stays on across cameras until you turn it off.", Modifier.padding(vertical = 8.dp))
        ToggleRow("Date stamp", "Burn the date into shots", vm.stampOn, first = true) { vm.stampOn = it; vm.saveSettings() }
        ToggleRow("Frame", "Film frame or instant border", vm.borderOn, last = true) { vm.borderOn = it; vm.saveSettings() }
        if (r.border == Border.INSTANT) {
            KSection("Print colour")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InstantFrame.entries.forEach { f ->
                    Box(Modifier.pressable({ vm.instantFrame = f; vm.saveSettings() }).size(36.dp)
                        .then(if (vm.instantFrame == f) Modifier.border(2.dp, s.accent, RoundedCornerShape(10.dp)).padding(4.dp) else Modifier)
                        .background(Color(f.color), RoundedCornerShape(7.dp)).border(0.5.dp, s.faint, RoundedCornerShape(7.dp)))
                }
            }
        }
    }
}

@Composable
fun BoxScope.SettingsPanel(
    vm: CamViewModel, visible: Boolean, onClose: () -> Unit, onImport: () -> Unit, onDarkroom: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    KSheet(visible, onClose, "Settings") {
        KSection("Viewfinder")
        ToggleRow("Grid", "Rule-of-thirds lines", vm.grid, first = true) { vm.grid = it; vm.saveSettings() }
        ToggleRow("Level", "Shows when the horizon is straight", vm.levelOn) { vm.setLevel(it) }
        ToggleRow("Histogram", "Live brightness graph", vm.histogramOn, last = true) { vm.setHistogram(it) }
        KSection("Capture")
        ToggleRow("Date stamp", "Burn the date or camcorder overlay into shots", vm.stampOn, first = true) { vm.stampOn = it; vm.saveSettings() }
        ToggleRow("Frames", "Instant prints, film edges and slide mounts", vm.borderOn) { vm.borderOn = it; vm.saveSettings() }
        ToggleRow("Keep negatives", "Store originals so the Darkroom can re-develop them", vm.saveNegatives) { vm.saveNegatives = it; vm.saveSettings() }
        ToggleRow("Shutter sound", "Click and record sounds", vm.shutterSound) { vm.shutterSound = it; vm.saveSettings() }
        ToggleRow("Record audio", "Microphone sound in videos", vm.recordAudio) { vm.recordAudio = it; vm.saveSettings() }
        ToggleRow("Stabilization", if (vm.caps.stabilization) "Steadier preview and video" else "Not supported on this camera", vm.stabilize && vm.caps.stabilization, last = true) {
            vm.stabilize = it; vm.rebindForStabilization()
        }
        KSection("Tools")
        ToggleRow("Develop a photo", "Pick one from your gallery and run it through this camera", false, first = true, trailing = { Label("Open", color = LocalSkin.current.accent, weight = FontWeight.Bold) }) { onClose(); onImport() }
        ToggleRow("Darkroom", "Re-develop saved negatives with any camera", false, trailing = { Label("Open", color = LocalSkin.current.accent, weight = FontWeight.Bold) }) { onClose(); onDarkroom() }
        ToggleRow("Rotate preview", "Only if the viewfinder looks sideways on your phone", false, last = true, trailing = { Label("${vm.previewRotationFix * 90}°", color = LocalSkin.current.accent, size = 12.sp, weight = FontWeight.Bold) }) { vm.cyclePreviewRotation() }
        extra()
        Body("FUN CAM is free and open source (MIT). 51 cameras, every lens and effect included. No ads, no tracking.", Modifier.padding(top = 16.dp).testTag("about"))
    }
}

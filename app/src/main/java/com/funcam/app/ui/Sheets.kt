package com.funcam.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funcam.app.model.Accessories
import com.funcam.app.model.Cameras
import com.funcam.app.model.Category
import com.funcam.app.model.FlashGel
import com.funcam.app.model.InstantFrame
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPickerSheet(vm: CamViewModel, onClose: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cats = Category.entries
    val listState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // flat list: header + cameras per category
    val sections = cats.map { c -> c to Cameras.inCategory(c) }
    val headerIndex = remember {
        var i = 0
        sections.associate { (c, l) -> c to i.also { i += 1 + l.size } }
    }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = state, containerColor = FC.bg) {
        Column(Modifier.padding(bottom = 12.dp)) {
            Text("Cameras", color = FC.text, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 18.dp))
            Text("${Cameras.all.size} cameras · all free", color = FC.dim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                cats.forEach { c ->
                    Text(
                        c.label, color = FC.text, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(FC.panel2)
                            .clickable { scope.launch { listState.animateScrollToItem(headerIndex[c] ?: 0) } }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            LazyVerticalGrid(
                state = listState, columns = GridCells.Fixed(3), modifier = Modifier.heightIn(max = 600.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                sections.forEach { (c, list) ->
                    item(span = { GridItemSpan(3) }, key = "h_" + c.name) {
                        Text(c.label.uppercase(), color = FC.amber, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
                    }
                    items(list, key = { it.id }) { r ->
                        val sel = r.id == vm.recipe.id
                        Column(
                            Modifier.clip(RoundedCornerShape(14.dp)).background(if (sel) FC.panel2 else FC.panel)
                                .border(if (sel) 1.5.dp else 0.dp, if (sel) FC.amber else Color.Transparent, RoundedCornerShape(14.dp))
                                .clickable { vm.selectRecipe(r); onClose() }.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CameraIcon(r, width = 84.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(r.name, color = if (sel) FC.amber else FC.text, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessoriesSheet(vm: CamViewModel, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = FC.bg) {
        val a = vm.acc
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 28.dp)) {
            Text("Lenses & filters", color = FC.text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("Stack them on any camera.", color = FC.dim, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            val toggles: List<Triple<String, String, Pair<Boolean, (Boolean) -> Accessories>>> = listOf(
                Triple("Fisheye W", "Full-frame bulging ultra-wide", a.fisheyeW to { v: Boolean -> a.copy(fisheyeW = v, fisheyeC = false) }),
                Triple("Fisheye C", "Circular fisheye in a black frame", a.fisheyeC to { v: Boolean -> a.copy(fisheyeC = v, fisheyeW = false) }),
                Triple("Prism", "Rainbow ghost reflections", a.prism to { v: Boolean -> a.copy(prism = v) }),
                Triple("Star", "Starbursts on bright lights", a.star to { v: Boolean -> a.copy(star = v) }),
                Triple("ND filter", "Darker exposure for slow shutters in daylight", a.nd to { v: Boolean -> a.copy(nd = v) }),
                Triple("Light leaks", "Random warm leaks on film cameras", a.lightLeaks to { v: Boolean -> a.copy(lightLeaks = v) }),
            )
            toggles.forEach { (title, sub, pair) -> ToggleRow(title, sub, pair.first) { vm.updateAcc(pair.second(it)) } }
            Spacer(Modifier.height(10.dp))
            Text("Flash colour", color = FC.text, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlashGel.entries.forEach { g ->
                    val c = Color(g.color.x, g.color.y, g.color.z)
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(c)
                            .border(if (a.gel == g) 3.dp else 1.dp, if (a.gel == g) FC.amber else FC.dim, CircleShape)
                            .clickable { vm.updateAcc(a.copy(gel = g)); if (!vm.flashOn) vm.toggleFlash() },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(vm: CamViewModel, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = { vm.saveSettings(); onClose() }, containerColor = FC.bg) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
            Text("Settings", color = FC.text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            ToggleRow("Date stamp", "Burn the date / camcorder overlay into shots", vm.stampOn) { vm.stampOn = it; vm.saveSettings() }
            ToggleRow("Frames", "Instant prints, film rebates and slide mounts", vm.borderOn) { vm.borderOn = it; vm.saveSettings() }
            ToggleRow("Keep negatives", "Store originals so you can re-develop them in the Darkroom", vm.saveNegatives) { vm.saveNegatives = it; vm.saveSettings() }
            ToggleRow("Shutter sound", "Click and record sounds", vm.shutterSound) { vm.shutterSound = it; vm.saveSettings() }
            ToggleRow("Record audio", "Microphone sound in videos", vm.recordAudio) { vm.recordAudio = it; vm.saveSettings() }
            Text("Instant frame", color = FC.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InstantFrame.entries.forEach { f ->
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(6.dp)).background(Color(f.color))
                            .border(if (vm.instantFrame == f) 3.dp else 1.dp, if (vm.instantFrame == f) FC.amber else FC.dim, RoundedCornerShape(6.dp))
                            .clickable { vm.instantFrame = f; vm.saveSettings() },
                    )
                }
            }
            ToggleRow(
                "Stabilization",
                if (vm.caps.stabilization) "Steadier preview and video" else "Not supported on this camera",
                vm.stabilize && vm.caps.stabilization,
            ) { vm.stabilize = it; vm.rebindForStabilization() }
            Row(
                Modifier.fillMaxWidth().clickable { vm.cyclePreviewRotation() }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Rotate preview", color = FC.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Only if the viewfinder looks sideways on your phone", color = FC.dim, fontSize = 12.sp)
                }
                Text("${vm.previewRotationFix * 90}°", color = FC.amber, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "FUN CAM is free and open source (MIT). Every camera and accessory is included — no subscriptions, no ads, no tracking.",
                color = FC.dim, fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun ToggleRow(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = FC.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(sub, color = FC.dim, fontSize = 12.sp)
        }
        Switch(
            checked = value, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = FC.amber, checkedThumbColor = Color.Black),
        )
    }
}

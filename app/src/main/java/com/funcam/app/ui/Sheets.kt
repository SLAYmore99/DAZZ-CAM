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
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPickerSheet(vm: CamViewModel, onClose: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cats = Category.entries
    var tab by remember { mutableIntStateOf(cats.indexOf(vm.recipe.category).coerceAtLeast(0)) }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = state, containerColor = FC.bg) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Cameras", color = FC.text, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 18.dp))
            Text("Every camera is free in FUN CAM.", color = FC.dim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp))
            ScrollableTabRow(selectedTabIndex = tab, containerColor = FC.bg, contentColor = FC.amber, edgePadding = 12.dp) {
                cats.forEachIndexed { i, c ->
                    Tab(selected = i == tab, onClick = { tab = i }, text = {
                        Text(c.label, fontWeight = FontWeight.Bold, color = if (i == tab) FC.amber else FC.text)
                    })
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), modifier = Modifier.heightIn(max = 520.dp),
                contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(Cameras.inCategory(cats[tab]), key = { it.id }) { r ->
                    val sel = r.id == vm.recipe.id
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(FC.panel)
                            .border(if (sel) 1.5.dp else 0.dp, if (sel) FC.amber else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { vm.selectRecipe(r); onClose() }.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CameraIcon(r, width = 76.dp)
                        Text(r.name, color = if (sel) FC.amber else FC.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(r.description, color = FC.dim, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 3, lineHeight = 11.sp)
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
            Text("Accessories", color = FC.text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("Stack lenses and filters on any camera.", color = FC.dim, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            val toggles: List<Triple<String, String, Pair<Boolean, (Boolean) -> Accessories>>> = listOf(
                Triple("Fisheye W", "Full-frame bulging ultra-wide", a.fisheyeW to { v: Boolean -> a.copy(fisheyeW = v, fisheyeC = false) }),
                Triple("Fisheye C", "Circular fisheye in a black frame", a.fisheyeC to { v: Boolean -> a.copy(fisheyeC = v, fisheyeW = false) }),
                Triple("Prism", "Rainbow ghost reflections", a.prism to { v: Boolean -> a.copy(prism = v) }),
                Triple("Star filter", "Starbursts on bright lights", a.star to { v: Boolean -> a.copy(star = v) }),
                Triple("ND filter", "Darker exposure for slow shutters in daylight", a.nd to { v: Boolean -> a.copy(nd = v) }),
                Triple("Double exposure", "Layer two shots in one frame", a.doubleExposure to { v: Boolean -> a.copy(doubleExposure = v) }),
                Triple("Light leaks", "Random warm leaks on film bodies", a.lightLeaks to { v: Boolean -> a.copy(lightLeaks = v) }),
            )
            toggles.forEach { (title, sub, pair) ->
                ToggleRow(title, sub, pair.first) { vm.updateAcc(pair.second(it)) }
            }
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

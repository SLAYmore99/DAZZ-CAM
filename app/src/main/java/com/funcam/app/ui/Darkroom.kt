package com.funcam.app.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funcam.app.media.Negative
import com.funcam.app.media.Negatives
import com.funcam.app.model.CaptureMode
import com.funcam.app.model.Cameras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Re-develop stored negatives with any photo camera, exposure and white balance. */
@Composable
fun Darkroom(vm: CamViewModel, onClose: () -> Unit) {
    var list by remember { mutableStateOf(vm.negatives()) }
    var selected by remember { mutableStateOf<Negative?>(null) }
    BackHandler { if (selected != null) selected = null else onClose() }

    Box(Modifier.fillMaxSize().background(FC.bg).systemBarsPadding()) {
        val sel = selected
        if (sel == null) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = FC.text) }
                    Text("Darkroom", color = FC.text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    "Your negatives. Tap one to re-develop it with another camera, exposure or colour temperature.",
                    color = FC.dim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp),
                )
                if (list.isEmpty()) {
                    Text("No negatives yet — take some photos first.", color = FC.dim, modifier = Modifier.padding(24.dp))
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(list, key = { it.id }) { n ->
                        var thumb by remember(n.id) { mutableStateOf<Bitmap?>(null) }
                        LaunchedEffect(n.id) { thumb = withContext(Dispatchers.IO) { Negatives.thumb(n) } }
                        Box(
                            Modifier.aspectRatio(0.75f).clip(RoundedCornerShape(6.dp)).background(FC.panel)
                                .clickable { selected = n },
                        ) {
                            thumb?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                            Text(
                                Cameras.byId(n.recipeId).name, color = Color.White, fontSize = 9.sp,
                                modifier = Modifier.align(Alignment.BottomStart).background(Color(0x88000000)).padding(3.dp),
                            )
                        }
                    }
                }
            }
        } else {
            DevelopEditor(vm, sel, onBack = { selected = null }, onDeleted = {
                vm.deleteNegative(sel); list = vm.negatives(); selected = null
            })
        }
    }
}

@Composable
private fun DevelopEditor(vm: CamViewModel, n: Negative, onBack: () -> Unit, onDeleted: () -> Unit) {
    val photoCams = remember(n.recipeId) {
        val original = Cameras.byId(n.recipeId)
        if (original.capture != CaptureMode.SINGLE) listOf(original)
        else Cameras.all.filter { !it.isVideo && it.capture == CaptureMode.SINGLE }
    }
    var recipe by remember { mutableStateOf(Cameras.byId(n.recipeId)) }
    var exp by remember { mutableFloatStateOf(n.exposure) }
    var tmp by remember { mutableFloatStateOf(n.temp) }
    var tnt by remember { mutableFloatStateOf(n.tint) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(recipe, exp, tmp, tnt) {
        delay(120) // debounce slider drags
        preview = vm.preview(n, recipe, exp, tmp, tnt)
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = FC.text) }
            Text("Develop", color = FC.text, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            IconButton(onClick = onDeleted) { Icon(Icons.Filled.Delete, "Delete negative", tint = FC.dim) }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
            val p = preview
            if (p != null) Image(p.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            else CircularProgressIndicator(color = FC.amber)
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(photoCams, key = { it.id }) { r ->
                val sel = r.id == recipe.id
                Column(
                    Modifier.width(64.dp).clip(RoundedCornerShape(10.dp)).background(FC.panel)
                        .border(if (sel) 1.5.dp else 0.dp, if (sel) FC.amber else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { recipe = r }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CameraIcon(r, width = 50.dp)
                    Text(r.name, color = if (sel) FC.amber else FC.text, fontSize = 9.sp, maxLines = 1)
                }
            }
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
            LabeledSlider("Exposure", exp) { exp = it }
            LabeledSlider("Temperature", tmp) { tmp = it }
            LabeledSlider("Tint", tnt) { tnt = it }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    working = true
                    scope.launch {
                        val ok = vm.redevelop(n, recipe, exp, tmp, tnt)
                        vm.message = if (ok) "Saved to gallery" else "Couldn't save"
                        working = false
                    }
                },
                enabled = !working,
                colors = ButtonDefaults.buttonColors(containerColor = FC.amber, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            ) { Text(if (working) "Developing…" else "Develop & save", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = FC.text, fontSize = 12.sp, modifier = Modifier.width(92.dp))
        Slider(value = value, onValueChange = onChange, valueRange = -2f..2f, modifier = Modifier.weight(1f))
        Text("%+.1f".format(value), color = FC.amber, fontSize = 12.sp, modifier = Modifier.width(40.dp))
    }
}

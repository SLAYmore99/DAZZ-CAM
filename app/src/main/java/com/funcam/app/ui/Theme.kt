package com.funcam.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object FC {
    val bg = Color(0xFF0D0D0E)
    val panel = Color(0xFF19191B)
    val panel2 = Color(0xFF242427)
    val amber = Color(0xFFFFB23F)
    val text = Color(0xFFEDE6DA)
    val dim = Color(0xFF8D877E)
    val red = Color(0xFFE5383B)
}

@Composable
fun FunCamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = FC.amber, onPrimary = Color.Black, background = FC.bg, surface = FC.panel,
            onSurface = FC.text, onBackground = FC.text, secondary = FC.amber,
            surfaceContainerLow = FC.panel, surfaceContainer = FC.panel, surfaceContainerHigh = FC.panel2,
        ),
        content = content,
    )
}

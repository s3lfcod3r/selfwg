package com.selfwg.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.selfwg.app.R

// Marken-Schrift (Orbitron 800, statisch instanziert) + Logo-Farben.
val Orbitron = FontFamily(Font(R.font.orbitron, FontWeight.Bold))
val SelfIce = Color(0xFF9DBDD0)
val SelfTeal = Color(0xFF33A78C)

/** Zweifarbiger SelfWG-Schriftzug wie im Logo. */
@Composable
fun SelfWgWordmark(fontSize: TextUnit = 22.sp) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = SelfIce)) { append("Self") }
            withStyle(SpanStyle(color = SelfTeal)) { append("WG") }
        },
        fontFamily = Orbitron,
        fontSize = fontSize
    )
}

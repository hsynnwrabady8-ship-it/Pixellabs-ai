package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BUILTIN_TEXT_PRESETS
import com.example.data.BezierNode
import com.example.data.CustomUserFont
import com.example.data.GradientDirectionPreset
import com.example.data.GradientStop
import com.example.data.ImageLayer
import com.example.data.MaskPresetMode
import com.example.data.PerspectiveQuad
import com.example.data.Point2D
import com.example.data.ShapeKind
import com.example.data.ShapeLayer
import com.example.data.StudioFont
import com.example.data.TextColorSpan
import com.example.data.TextFontSpan
import com.example.data.TextLayer
import com.example.data.TextStylePreset
import com.example.ui.theme.LocalStudioPalette
import kotlin.math.roundToInt

private val ULTRA_SWATCHES = listOf(
    "#FFFFFF", "#000000", "#F2C100", "#FACC15", "#EF4444", "#F97316", "#F59E0B",
    "#10B981", "#06B6D4", "#38BDF8", "#3B82F6", "#8B5CF6", "#EC4899", "#F43F5E",
    "#84CC16", "#14B8A6", "#6366F1", "#A855F7", "#1E293B", "#332200"
)

// ============================================================================
// 1. PIXELLAB 6-GRID RELATIVE POSITION SNAP PANEL
// ============================================================================
@Composable
fun RelativePosition6GridPanel(
    onSnapPosition: (newX: Float?, newY: Float?) -> Unit,
    onCenterLayer: (horizontal: Boolean, vertical: Boolean) -> Unit
) {
    val palette = LocalStudioPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("rel_pos_grid_panel"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1 of PixelLab 6-Grid: Top-Left, Top-Center, Top-Right, Center-H
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PixelLabRelGridButton(
                label = "↖ بالا چپ",
                modifier = Modifier.weight(1f),
                onClick = { onSnapPosition(-0.36f, -0.36f) }
            )
            PixelLabRelGridButton(
                label = "↑ بالا وسط",
                modifier = Modifier.weight(1f),
                isPrimary = true,
                onClick = { onSnapPosition(null, -0.36f) }
            )
            PixelLabRelGridButton(
                label = "↗ بالا راست",
                modifier = Modifier.weight(1f),
                onClick = { onSnapPosition(0.36f, -0.36f) }
            )
            PixelLabRelGridButton(
                label = "↔ وسط افقی",
                modifier = Modifier.weight(1f),
                isAccent = true,
                onClick = { onCenterLayer(true, false) }
            )
        }

        // Row 2 of PixelLab 6-Grid: Left-Center, Dead-Center, Right-Center, Center-V
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PixelLabRelGridButton(
                label = "← چپ بوم",
                modifier = Modifier.weight(1f),
                isPrimary = true,
                onClick = { onSnapPosition(-0.36f, null) }
            )
            PixelLabRelGridButton(
                label = "🎯 مرکز دقیق",
                modifier = Modifier.weight(1f),
                isAccent = true,
                onClick = { onCenterLayer(true, true) }
            )
            PixelLabRelGridButton(
                label = "→ راست بوم",
                modifier = Modifier.weight(1f),
                isPrimary = true,
                onClick = { onSnapPosition(0.36f, null) }
            )
            PixelLabRelGridButton(
                label = "↕ وسط عمودی",
                modifier = Modifier.weight(1f),
                isAccent = true,
                onClick = { onCenterLayer(false, true) }
            )
        }

        // Row 3: Bottom-Left, Bottom-Center, Bottom-Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PixelLabRelGridButton(
                label = "↙ پایین چپ",
                modifier = Modifier.weight(1f),
                onClick = { onSnapPosition(-0.36f, 0.36f) }
            )
            PixelLabRelGridButton(
                label = "↓ پایین وسط",
                modifier = Modifier.weight(1f),
                isPrimary = true,
                onClick = { onSnapPosition(null, 0.36f) }
            )
            PixelLabRelGridButton(
                label = "↘ پایین راست",
                modifier = Modifier.weight(1f),
                onClick = { onSnapPosition(0.36f, 0.36f) }
            )
        }
    }
}

@Composable
private fun PixelLabRelGridButton(
    label: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isAccent: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val bg = when {
        isAccent -> palette.primaryAccent
        isPrimary -> palette.primaryAccent.copy(alpha = 0.18f)
        else -> palette.surfaceVariant
    }
    val fg = when {
        isAccent -> Color.Black
        isPrimary -> palette.primaryAccent
        else -> palette.textPrimary
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isAccent || isPrimary) palette.primaryAccent else palette.border),
        modifier = modifier
            .height(34.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isAccent || isPrimary) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                maxLines = 1
            )
        }
    }
}

// ============================================================================
// 2. MULTI-STOP GRADIENT BUILDER, EYEDROPPER & SUBSTRING COLOR/FONT PANEL
// ============================================================================
@Composable
fun TextColorGradientAndSpanPanel(
    textLayer: TextLayer,
    customUserFonts: List<CustomUserFont> = emptyList(),
    smartPaletteHexes: List<String> = emptyList(),
    isEyedropperActive: Boolean = false,
    onToggleEyedropper: () -> Unit = {},
    onExtractSmartPalette: () -> Unit = {},
    onUpdateTextLayer: ((TextLayer) -> TextLayer) -> Unit
) {
    val palette = LocalStudioPalette.current
    var colorTab by remember { mutableIntStateOf(0) } // 0: Solid, 1: Multi-Stop Gradient, 2: Word/Range Color, 3: Word/Range Font
    val textLen = textLayer.text.length.coerceAtLeast(1)
    var spanStart by remember(textLayer.id) { mutableIntStateOf(0) }
    var spanEnd by remember(textLayer.id, textLen) {
        val firstSpace = textLayer.text.indexOf(' ').takeIf { it > 0 } ?: textLen
        mutableIntStateOf(firstSpace.coerceIn(1, textLen))
    }
    var selectedStopIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Mode Selector + Eyedropper + Smart Palette
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                0 to "تک‌رنگ",
                1 to "گرادیان چندنقطه‌ای (+)",
                2 to "رنگ بخشی متن",
                3 to "فونت بخشی متن"
            ).forEach { (idx, label) ->
                val isSel = colorTab == idx
                Surface(
                    color = if (isSel) palette.primaryAccent else palette.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { colorTab = idx }
                ) {
                    Text(
                        text = label,
                        color = if (isSel) Color.Black else palette.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Live Canvas Eyedropper Button
            Surface(
                color = if (isEyedropperActive) Color(0xFFF59E0B) else palette.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isEyedropperActive) Color(0xFFFDE047) else palette.border),
                modifier = Modifier
                    .clickable { onToggleEyedropper() }
                    .testTag("activate_eyedropper_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Colorize,
                        contentDescription = "قطره‌چکان",
                        tint = if (isEyedropperActive) Color.Black else palette.primaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEyedropperActive) "لمس بوم..." else "قطره‌چکان",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEyedropperActive) Color.Black else palette.primaryAccent
                    )
                }
            }

            // Extract Smart Palette from Canvas Image
            Surface(
                color = Color(0xFF1E1B4B),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF818CF8)),
                modifier = Modifier
                    .clickable { onExtractSmartPalette() }
                    .testTag("extract_smart_palette_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = Color(0xFFC7D2FE),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "پالت هوشمند بوم",
                        fontSize = 10.sp,
                        color = Color(0xFFC7D2FE),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Smart Extracted Palette Row (if extracted)
        if (smartPaletteHexes.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text("پالت بوم:", fontSize = 9.sp, color = palette.secondaryAccent)
                }
                items(smartPaletteHexes) { hex ->
                    val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.White)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.White, CircleShape)
                            .clickable {
                                onUpdateTextLayer { it.copy(colorHex = hex, useGradientFill = false) }
                            }
                    )
                }
            }
        }

        when (colorTab) {
            0 -> {
                // Solid Color Swatches
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ULTRA_SWATCHES) { hex ->
                        val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.White)
                        val isSel = textLayer.colorHex.equals(hex, ignoreCase = true) && !textLayer.useGradientFill
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    if (isSel) 3.dp else 1.dp,
                                    if (isSel) palette.primaryAccent else Color.Gray,
                                    CircleShape
                                )
                                .clickable {
                                    onUpdateTextLayer { it.copy(colorHex = hex, useGradientFill = false) }
                                }
                        )
                    }
                }
            }

            1 -> {
                // Multi-Stop Custom Gradient Builder (6 PixelLab Presets + Multi-Stop Bar)
                val activeStops = if (textLayer.gradientStops.size >= 2) {
                    textLayer.gradientStops
                } else {
                    listOf(
                        GradientStop(0f, textLayer.colorHex),
                        GradientStop(1f, textLayer.gradientSecondColorHex)
                    )
                }
                val safeStopIdx = selectedStopIndex.coerceIn(0, activeStops.lastIndex)
                val currentStop = activeStops[safeStopIdx]

                // 6 Direction Presets
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(GradientDirectionPreset.entries) { dir ->
                        val isDirSel = textLayer.gradientDirection == dir && textLayer.useGradientFill
                        Surface(
                            color = if (isDirSel) palette.primaryAccent.copy(alpha = 0.25f) else palette.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isDirSel) palette.primaryAccent else palette.border),
                            modifier = Modifier.clickable {
                                onUpdateTextLayer {
                                    it.copy(
                                        useGradientFill = true,
                                        gradientDirection = dir,
                                        gradientStops = activeStops
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = dir.labelFa,
                                fontSize = 9.sp,
                                color = if (isDirSel) palette.primaryAccent else palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Multi-stop nodes row + Add/Remove stop buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(activeStops) { idx, stop ->
                            val isStopSel = idx == safeStopIdx
                            val stopCol = runCatching { Color(android.graphics.Color.parseColor(stop.colorHex)) }.getOrDefault(Color.White)
                            Surface(
                                color = if (isStopSel) palette.primaryAccent.copy(alpha = 0.22f) else palette.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(if (isStopSel) 1.5.dp else 1.dp, if (isStopSel) palette.primaryAccent else palette.border),
                                modifier = Modifier.clickable { selectedStopIndex = idx }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(stopCol)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${(stop.position * 100).roundToInt()}%",
                                        fontSize = 9.sp,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // + Add Stop
                    Surface(
                        color = palette.primaryAccent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable {
                                if (activeStops.size < 6) {
                                    val updated = (activeStops + GradientStop(0.5f, "#38BDF8")).sortedBy { it.position }
                                    onUpdateTextLayer {
                                        it.copy(useGradientFill = true, gradientStops = updated)
                                    }
                                    selectedStopIndex = updated.lastIndex
                                }
                            }
                            .testTag("add_gradient_stop_btn")
                    ) {
                        Text(
                            text = "+ نقطه",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    // - Remove Stop
                    if (activeStops.size > 2) {
                        Surface(
                            color = palette.danger.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable {
                                val updated = activeStops.filterIndexed { i, _ -> i != safeStopIdx }
                                selectedStopIndex = 0
                                onUpdateTextLayer {
                                    it.copy(useGradientFill = true, gradientStops = updated)
                                }
                            }
                        ) {
                            Text(
                                text = "حذف",
                                fontSize = 9.sp,
                                color = palette.danger,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Selected Stop Position Slider & Color Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مکان نقطه:", fontSize = 9.sp, color = palette.textSecondary)
                    Slider(
                        value = currentStop.position,
                        onValueChange = { pos ->
                            val updated = activeStops.toMutableList().apply {
                                this[safeStopIdx] = currentStop.copy(position = pos)
                            }
                            onUpdateTextLayer {
                                it.copy(useGradientFill = true, gradientStops = updated)
                            }
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .padding(horizontal = 6.dp)
                    )
                    LazyRow(
                        modifier = Modifier.weight(1.2f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        items(ULTRA_SWATCHES) { hex ->
                            val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.White)
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        val updated = activeStops.toMutableList().apply {
                                            this[safeStopIdx] = currentStop.copy(colorHex = hex)
                                        }
                                        onUpdateTextLayer {
                                            it.copy(
                                                useGradientFill = true,
                                                gradientStops = updated,
                                                colorHex = updated.first().colorHex,
                                                gradientSecondColorHex = updated.last().colorHex
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            2, 3 -> {
                // Substring Selection Helper (Word Chips + Start/End Sliders)
                val wordRanges = remember(textLayer.text) {
                    Regex("\\S+").findAll(textLayer.text).map { it.range.first to (it.range.last + 1) }.toList()
                }
                val safeS = spanStart.coerceIn(0, textLen)
                val safeE = spanEnd.coerceIn(safeS, textLen)
                val selectedSubstring = textLayer.text.substring(safeS, safeE)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بازه انتخابی ($safeS..$safeE): «$selectedSubstring»",
                        fontSize = 10.sp,
                        color = palette.primaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                    if (colorTab == 2 && textLayer.colorSpans.isNotEmpty()) {
                        Text(
                            text = "پاکسازی رنگ‌های بخشی",
                            fontSize = 9.sp,
                            color = palette.danger,
                            modifier = Modifier.clickable {
                                onUpdateTextLayer { it.copy(colorSpans = emptyList()) }
                            }
                        )
                    } else if (colorTab == 3 && textLayer.fontSpans.isNotEmpty()) {
                        Text(
                            text = "پاکسازی فونت‌های بخشی",
                            fontSize = 9.sp,
                            color = palette.danger,
                            modifier = Modifier.clickable {
                                onUpdateTextLayer { it.copy(fontSpans = emptyList()) }
                            }
                        )
                    }
                }

                // Word Quick Chips + Character Range Sliders
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(wordRanges) { (wStart, wEnd) ->
                        val word = textLayer.text.substring(wStart, wEnd)
                        val isWordSel = spanStart == wStart && spanEnd == wEnd
                        Surface(
                            color = if (isWordSel) palette.primaryAccent.copy(alpha = 0.25f) else palette.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isWordSel) palette.primaryAccent else palette.border),
                            modifier = Modifier.clickable {
                                spanStart = wStart
                                spanEnd = wEnd
                            }
                        ) {
                            Text(
                                text = word,
                                fontSize = 10.sp,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (colorTab == 2) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        items(ULTRA_SWATCHES) { hex ->
                            val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.White)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        if (safeE > safeS) {
                                            onUpdateTextLayer { layer ->
                                                val filtered = layer.colorSpans.filterNot { it.start == safeS && it.end == safeE }
                                                layer.copy(colorSpans = filtered + TextColorSpan(safeS, safeE, hex))
                                            }
                                        }
                                    }
                            )
                        }
                    }
                } else {
                    // Substring Font Picker (`TextFontSpan`)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        items(StudioFont.entries) { f ->
                            Surface(
                                color = palette.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier.clickable {
                                    if (safeE > safeS) {
                                        onUpdateTextLayer { layer ->
                                            val filtered = layer.fontSpans.filterNot { it.start == safeS && it.end == safeE }
                                            layer.copy(
                                                fontSpans = filtered + TextFontSpan(
                                                    start = safeS,
                                                    end = safeE,
                                                    fontId = f.id
                                                )
                                            )
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    text = f.displayName,
                                    fontFamily = FontFamily(Font(f.fontRes)),
                                    fontSize = 11.sp,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. 4-POINT FREE PERSPECTIVE WARP PANEL (Matrix.setPolyToPoly)
// ============================================================================
@Composable
fun Perspective4PointWarpPanel(
    quad: PerspectiveQuad,
    skewX: Float = 0f,
    skewY: Float = 0f,
    onUpdateQuad: (PerspectiveQuad) -> Unit,
    onUpdateSkew: ((Float, Float) -> Unit)? = null
) {
    val palette = LocalStudioPalette.current
    var selectedCorner by remember { mutableIntStateOf(0) } // 0: TL, 1: TR, 2: BR, 3: BL

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("perspective_4point_panel"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Switch(
                    checked = quad.enabled,
                    onCheckedChange = { en -> onUpdateQuad(quad.copy(enabled = en)) }
                )
                Text(
                    text = "پرسپکتیو آزاد ۴ نقطه‌ای (4-Point Warp)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }

            // Presets & Reset
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = palette.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable {
                        onUpdateQuad(
                            PerspectiveQuad(
                                enabled = true,
                                topLeftX = 0f, topLeftY = -0.12f,
                                topRightX = 0f, topRightY = 0.14f,
                                bottomRightX = 0f, bottomRightY = -0.14f,
                                bottomLeftX = 0f, bottomLeftY = 0.12f
                            )
                        )
                    }
                ) {
                    Text("تابلو چپ", fontSize = 9.sp, color = palette.primaryAccent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
                Surface(
                    color = palette.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable {
                        onUpdateQuad(
                            PerspectiveQuad(
                                enabled = true,
                                topLeftX = 0.14f, topLeftY = 0f,
                                topRightX = -0.14f, topRightY = 0f,
                                bottomRightX = 0.10f, bottomRightY = 0f,
                                bottomLeftX = -0.10f, bottomLeftY = 0f
                            )
                        )
                    }
                ) {
                    Text("جاده 3D", fontSize = 9.sp, color = palette.secondaryAccent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
                Surface(
                    color = palette.danger.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable {
                        onUpdateQuad(PerspectiveQuad(enabled = false))
                        onUpdateSkew?.invoke(0f, 0f)
                    }
                ) {
                    Text("ریست", fontSize = 9.sp, color = palette.danger, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }
        }

        // Corner Selector Chips (TL, TR, BR, BL)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0 to "↖ گوشه بالا-چپ",
                1 to "↗ گوشه بالا-راست",
                2 to "↘ گوشه پایین-راست",
                3 to "↙ گوشه پایین-چپ"
            ).forEach { (idx, title) ->
                val isSel = selectedCorner == idx
                Surface(
                    color = if (isSel) palette.primaryAccent else palette.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedCorner = idx }
                ) {
                    Text(
                        text = title,
                        fontSize = 9.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) Color.Black else palette.textPrimary,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                        maxLines = 1
                    )
                }
            }
        }

        val curDx = when (selectedCorner) {
            0 -> quad.topLeftX
            1 -> quad.topRightX
            2 -> quad.bottomRightX
            else -> quad.bottomLeftX
        }
        val curDy = when (selectedCorner) {
            0 -> quad.topLeftY
            1 -> quad.topRightY
            2 -> quad.bottomRightY
            else -> quad.bottomLeftY
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("افقی X:", fontSize = 10.sp, color = palette.textSecondary)
            Slider(
                value = curDx,
                onValueChange = { nx ->
                    val next = when (selectedCorner) {
                        0 -> quad.copy(enabled = true, topLeftX = nx)
                        1 -> quad.copy(enabled = true, topRightX = nx)
                        2 -> quad.copy(enabled = true, bottomRightX = nx)
                        else -> quad.copy(enabled = true, bottomLeftX = nx)
                    }
                    onUpdateQuad(next)
                },
                valueRange = -0.45f..0.45f,
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .padding(horizontal = 4.dp)
            )
            Text("عمودی Y:", fontSize = 10.sp, color = palette.textSecondary)
            Slider(
                value = curDy,
                onValueChange = { ny ->
                    val next = when (selectedCorner) {
                        0 -> quad.copy(enabled = true, topLeftY = ny)
                        1 -> quad.copy(enabled = true, topRightY = ny)
                        2 -> quad.copy(enabled = true, bottomRightY = ny)
                        else -> quad.copy(enabled = true, bottomLeftY = ny)
                    }
                    onUpdateQuad(next)
                },
                valueRange = -0.45f..0.45f,
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

// ============================================================================
// 4. BEZIER MASK (IN / OUT) & CLIPPING PANEL
// ============================================================================
@Composable
fun BezierMaskInspectorPanel(
    maskMode: MaskPresetMode,
    maskInvert: Boolean,
    maskControlX: Float,
    maskControlY: Float,
    maskAngle: Float,
    onUpdateMask: (MaskPresetMode, Boolean, Float, Float, Float) -> Unit
) {
    val palette = LocalStudioPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("bezier_mask_panel"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Mask Mode Selector + In/Out Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaskPresetMode.entries.forEach { m ->
                val isSel = maskMode == m
                Surface(
                    color = if (isSel) palette.primaryAccent else palette.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        onUpdateMask(m, maskInvert, maskControlX, maskControlY, maskAngle)
                    }
                ) {
                    Text(
                        text = m.labelFa,
                        color = if (isSel) Color.Black else palette.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // PixelLab Mask Action: In (داخل) vs Out (خارج)
            Surface(
                color = if (!maskInvert) palette.secondaryAccent else palette.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable {
                    onUpdateMask(maskMode, false, maskControlX, maskControlY, maskAngle)
                }
            ) {
                Text(
                    text = "In (حذف داخل)",
                    fontSize = 10.sp,
                    color = if (!maskInvert) Color.Black else palette.textPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Surface(
                color = if (maskInvert) palette.secondaryAccent else palette.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable {
                    onUpdateMask(maskMode, true, maskControlX, maskControlY, maskAngle)
                }
            ) {
                Text(
                    text = "Out (حذف خارج)",
                    fontSize = 10.sp,
                    color = if (maskInvert) Color.Black else palette.textPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (maskMode != MaskPresetMode.NONE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("قوس افقی:", fontSize = 9.sp, color = palette.textSecondary)
                Slider(
                    value = maskControlX,
                    onValueChange = { cx ->
                        onUpdateMask(maskMode, maskInvert, cx, maskControlY, maskAngle)
                    },
                    valueRange = 0.05f..0.95f,
                    modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 4.dp)
                )
                Text("عمق برش Y:", fontSize = 9.sp, color = palette.textSecondary)
                Slider(
                    value = maskControlY,
                    onValueChange = { cy ->
                        onUpdateMask(maskMode, maskInvert, maskControlX, cy, maskAngle)
                    },
                    valueRange = 0.05f..0.95f,
                    modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 4.dp)
                )
                Text("زاویه:", fontSize = 9.sp, color = palette.textSecondary)
                Slider(
                    value = maskAngle,
                    onValueChange = { ang ->
                        onUpdateMask(maskMode, maskInvert, maskControlX, maskControlY, ang)
                    },
                    valueRange = -180f..180f,
                    modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 4.dp)
                )
            }
        }
    }
}

// ============================================================================
// 5. BEZIER CURVE VECTOR PATH EDITOR PANEL (ShapeKind.BEZIER_PATH)
// ============================================================================
@Composable
fun BezierVectorPathEditorPanel(
    shapeLayer: ShapeLayer,
    onUpdateShapeLayer: ((ShapeLayer) -> ShapeLayer) -> Unit
) {
    val palette = LocalStudioPalette.current
    val nodes = shapeLayer.bezierNodes.ifEmpty {
        listOf(
            BezierNode(x = -0.35f, y = 0.15f, ctrlX = -0.20f, ctrlY = -0.35f),
            BezierNode(x = 0.0f, y = -0.25f, ctrlX = 0.20f, ctrlY = -0.25f),
            BezierNode(x = 0.35f, y = 0.15f, ctrlX = 0.0f, ctrlY = 0.35f)
        )
    }
    var selectedNodeIdx by remember(nodes.size) { mutableIntStateOf(0.coerceAtMost(nodes.lastIndex)) }
    val safeIdx = selectedNodeIdx.coerceIn(0, nodes.lastIndex)
    val curNode = nodes[safeIdx]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("bezier_vector_editor_panel"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top Row: Closed Path Toggle, Fill Toggle, Add/Delete Node
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (shapeLayer.isBezierClosed) palette.primaryAccent else palette.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable {
                    onUpdateShapeLayer { it.copy(shapeKind = ShapeKind.BEZIER_PATH, isBezierClosed = !it.isBezierClosed, bezierNodes = nodes) }
                }
            ) {
                Text(
                    text = if (shapeLayer.isBezierClosed) "مسیر بسته (Closed)" else "مسیر باز (Open)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (shapeLayer.isBezierClosed) Color.Black else palette.textPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = if (shapeLayer.isBezierFilled) palette.secondaryAccent else palette.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable {
                    onUpdateShapeLayer { it.copy(shapeKind = ShapeKind.BEZIER_PATH, isBezierFilled = !it.isBezierFilled, bezierNodes = nodes) }
                }
            ) {
                Text(
                    text = if (shapeLayer.isBezierFilled) "پر کردن داخل (Fill)" else "فقط خط دور (Stroke)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (shapeLayer.isBezierFilled) Color.Black else palette.textPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Add Node
            Surface(
                color = palette.primaryAccent.copy(alpha = 0.22f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, palette.primaryAccent),
                modifier = Modifier
                    .clickable {
                        val newNode = BezierNode(
                            x = (curNode.x + 0.18f).coerceIn(-0.45f, 0.45f),
                            y = (curNode.y + 0.12f).coerceIn(-0.45f, 0.45f),
                            ctrlX = (curNode.x + 0.25f).coerceIn(-0.48f, 0.48f),
                            ctrlY = (curNode.y - 0.15f).coerceIn(-0.48f, 0.48f)
                        )
                        val updated = nodes + newNode
                        onUpdateShapeLayer {
                            it.copy(shapeKind = ShapeKind.BEZIER_PATH, bezierNodes = updated)
                        }
                        selectedNodeIdx = updated.lastIndex
                    }
                    .testTag("bezier_add_node_btn")
            ) {
                Text(
                    text = "+ افزودن گره",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Delete Node
            if (nodes.size > 2) {
                Surface(
                    color = palette.danger.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable {
                            val updated = nodes.filterIndexed { index, _ -> index != safeIdx }
                            selectedNodeIdx = 0
                            onUpdateShapeLayer {
                                it.copy(shapeKind = ShapeKind.BEZIER_PATH, bezierNodes = updated)
                            }
                        }
                        .testTag("bezier_remove_node_btn")
                ) {
                    Text(
                        text = "- حذف گره",
                        fontSize = 10.sp,
                        color = palette.danger,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Node Selector Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(nodes) { idx, _ ->
                val isSel = idx == safeIdx
                Surface(
                    color = if (isSel) palette.primaryAccent else palette.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable { selectedNodeIdx = idx }
                ) {
                    Text(
                        text = "گره ${idx + 1}",
                        fontSize = 9.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) Color.Black else palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Node Position (X/Y) & Bezier Handle Tangents (Curve X / Curve Y)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("مکان X:", fontSize = 9.sp, color = palette.textSecondary)
            Slider(
                value = curNode.x,
                onValueChange = { nx ->
                    val updated = nodes.toMutableList().apply {
                        this[safeIdx] = curNode.copy(x = nx)
                    }
                    onUpdateShapeLayer { it.copy(shapeKind = ShapeKind.BEZIER_PATH, bezierNodes = updated) }
                },
                valueRange = -0.48f..0.48f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 2.dp)
            )
            Text("Y:", fontSize = 9.sp, color = palette.textSecondary)
            Slider(
                value = curNode.y,
                onValueChange = { ny ->
                    val updated = nodes.toMutableList().apply {
                        this[safeIdx] = curNode.copy(y = ny)
                    }
                    onUpdateShapeLayer { it.copy(shapeKind = ShapeKind.BEZIER_PATH, bezierNodes = updated) }
                },
                valueRange = -0.48f..0.48f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 2.dp)
            )
            Text("بازوی قوس:", fontSize = 9.sp, color = palette.primaryAccent)
            Slider(
                value = curNode.ctrlY,
                onValueChange = { cy ->
                    val updated = nodes.toMutableList().apply {
                        this[safeIdx] = curNode.copy(ctrlY = cy)
                    }
                    onUpdateShapeLayer { it.copy(shapeKind = ShapeKind.BEZIER_PATH, bezierNodes = updated) }
                },
                valueRange = -0.48f..0.48f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 2.dp)
            )
        }
    }
}

// ============================================================================
// 6. FULL 3D LIGHTING & EXTRUSION PANEL (Ambient, Specular, Bevel, Light Angle)
// ============================================================================
@Composable
fun Text3dFullLightingPanel(
    textLayer: TextLayer,
    onUpdateTextLayer: ((TextLayer) -> TextLayer) -> Unit
) {
    val palette = LocalStudioPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("text_3d_lighting_panel"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = textLayer.is3dEnabled,
                    onCheckedChange = { en -> onUpdateTextLayer { it.copy(is3dEnabled = en) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = palette.primaryAccent)
                )
                Surface(
                    color = if (textLayer.isPerspective3d) palette.primaryAccent else palette.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onUpdateTextLayer { it.copy(is3dEnabled = true, isPerspective3d = true) } }
                ) {
                    Text(
                        "Perspective (عمق مرکزی)",
                        fontSize = 10.sp,
                        color = if (textLayer.isPerspective3d) Color.Black else palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = if (!textLayer.isPerspective3d) palette.primaryAccent else palette.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onUpdateTextLayer { it.copy(is3dEnabled = true, isPerspective3d = false) } }
                ) {
                    Text(
                        "Oblique (مورب کلاسیک)",
                        fontSize = 10.sp,
                        color = if (!textLayer.isPerspective3d) Color.Black else palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("نور 3D:", fontSize = 10.sp, color = palette.secondaryAccent)
                Switch(
                    checked = textLayer.simulateLighting3d,
                    onCheckedChange = { sim -> onUpdateTextLayer { it.copy(is3dEnabled = true, simulateLighting3d = sim) } }
                )
            }
        }

        // Depth, Angle, Darken
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("عمق:", fontSize = 9.sp, color = palette.textSecondary)
            Slider(
                value = textLayer.depth3d.toFloat(),
                onValueChange = { d -> onUpdateTextLayer { it.copy(is3dEnabled = true, depth3d = d.toInt()) } },
                valueRange = 1f..60f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 4.dp)
            )
            Text("زاویه:", fontSize = 9.sp, color = palette.textSecondary)
            Slider(
                value = textLayer.obliqueAngle3d,
                onValueChange = { ang -> onUpdateTextLayer { it.copy(is3dEnabled = true, obliqueAngle3d = ang) } },
                valueRange = 0f..360f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 4.dp)
            )
            Text("تیرگی:", fontSize = 9.sp, color = palette.textSecondary)
            Slider(
                value = textLayer.darken3d,
                onValueChange = { dk -> onUpdateTextLayer { it.copy(is3dEnabled = true, darken3d = dk) } },
                valueRange = 0f..0.95f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 4.dp)
            )
        }

        // 3D Lighting Parameters: Light Angle, Ambient, Specular, Bevel
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("زاویه نور:", fontSize = 9.sp, color = palette.primaryAccent)
            Slider(
                value = textLayer.lightAngle3d,
                onValueChange = { la -> onUpdateTextLayer { it.copy(is3dEnabled = true, simulateLighting3d = true, lightAngle3d = la) } },
                valueRange = 0f..360f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 3.dp)
            )
            Text("نور محیط:", fontSize = 9.sp, color = palette.primaryAccent)
            Slider(
                value = textLayer.ambientLight3d,
                onValueChange = { amb -> onUpdateTextLayer { it.copy(is3dEnabled = true, simulateLighting3d = true, ambientLight3d = amb) } },
                valueRange = 0.05f..1f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 3.dp)
            )
            Text("براقیت:", fontSize = 9.sp, color = palette.secondaryAccent)
            Slider(
                value = textLayer.specularHardness3d,
                onValueChange = { sp -> onUpdateTextLayer { it.copy(is3dEnabled = true, simulateLighting3d = true, specularHardness3d = sp) } },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 3.dp)
            )
            Text("پخ لبه:", fontSize = 9.sp, color = palette.secondaryAccent)
            Slider(
                value = textLayer.bevel3d,
                onValueChange = { bv -> onUpdateTextLayer { it.copy(is3dEnabled = true, simulateLighting3d = true, bevel3d = bv) } },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 3.dp)
            )
        }
    }
}

// ============================================================================
// 7. BEYOND-PIXELLAB ULTRA PRO EFFECTS (Double Stroke, RGB Glitch, Glass Pill)
// ============================================================================
@Composable
fun BeyondPixelLabUltraTextPanel(
    textLayer: TextLayer,
    onUpdateTextLayer: ((TextLayer) -> TextLayer) -> Unit
) {
    val palette = LocalStudioPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("beyond_pixellab_ultra_panel"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 1. Double Outer Contour Stroke (خط دور دوم یوتیوبی)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "خط دور دوم (Double Stroke):",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryAccent
            )
            Spacer(modifier = Modifier.width(6.dp))
            Switch(
                checked = textLayer.secondStrokeEnabled,
                onCheckedChange = { en -> onUpdateTextLayer { it.copy(secondStrokeEnabled = en) } }
            )
            Slider(
                value = textLayer.secondStrokeWidth,
                onValueChange = { w ->
                    onUpdateTextLayer { it.copy(secondStrokeEnabled = w > 0f, secondStrokeWidth = w) }
                },
                valueRange = 0f..28f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 6.dp)
            )
            LazyRow(
                modifier = Modifier.weight(1.1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(ULTRA_SWATCHES) { hex ->
                    val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.White)
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable {
                                onUpdateTextLayer { it.copy(secondStrokeEnabled = true, secondStrokeColorHex = hex) }
                            }
                    )
                }
            }
        }

        // 2. RGB Chromatic Glitch & Glass Pill Border
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "گلیچ نئون RGB:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00F0FF)
            )
            Slider(
                value = textLayer.glitchOffsetPx,
                onValueChange = { g -> onUpdateTextLayer { it.copy(glitchOffsetPx = g) } },
                valueRange = 0f..24f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 6.dp)
            )
            Text(
                text = "کادر شیشه‌ای (Glass Pill):",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = palette.secondaryAccent
            )
            Spacer(modifier = Modifier.width(6.dp))
            Switch(
                checked = textLayer.backgroundPillGlassBlur,
                onCheckedChange = { b ->
                    onUpdateTextLayer {
                        it.copy(
                            backgroundPillEnabled = if (b) true else it.backgroundPillEnabled,
                            backgroundPillGlassBlur = b,
                            backgroundPillStrokeEnabled = b
                        )
                    }
                }
            )
        }
    }
}

// ============================================================================
// 8. MY SAVED TEXT STYLES ("استایل‌های من") & BUILT-IN PRESETS BROWSER
// ============================================================================
@Composable
fun MySavedTextStylesPanel(
    savedUserPresets: List<TextStylePreset>,
    onApplyPreset: (TextStylePreset) -> Unit,
    onSaveCurrentLayerAsPreset: () -> Unit,
    onDeleteUserPreset: (String) -> Unit
) {
    val palette = LocalStudioPalette.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .testTag("my_saved_styles_panel"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "بانک «استایل‌های من» و قالب‌های آماده پیکسل‌لب",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryAccent
            )
            Button(
                onClick = onSaveCurrentLayerAsPreset,
                colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier
                    .height(28.dp)
                    .testTag("save_my_text_style_btn")
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ذخیره استایل لایه فعلی", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(savedUserPresets) { preset ->
                Card(
                    modifier = Modifier.clickable { onApplyPreset(preset) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    border = BorderStroke(1.dp, Color(0xFFA855F7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "★ ${preset.titleFa}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE9D5FF)
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = palette.danger,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onDeleteUserPreset(preset.id) }
                        )
                    }
                }
            }

            items(BUILTIN_TEXT_PRESETS) { preset ->
                Card(
                    modifier = Modifier.clickable { onApplyPreset(preset) },
                    colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant),
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = preset.titleFa,
                        fontSize = 11.sp,
                        color = palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

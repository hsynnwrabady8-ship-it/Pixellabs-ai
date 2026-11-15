package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.canvas.StudioCanvasRenderer

val STUDIO_COLOR_PALETTE = listOf(
    "#FFFFFF", "#F8FAFC", "#94A3B8", "#1E293B", "#0F172A", "#000000",
    "#38BDF8", "#0284C7", "#0369A1", "#A855F7", "#7E22CE", "#4C1D95",
    "#F43F5E", "#E11D48", "#FB7185", "#FDE047", "#F59E0B", "#D97706",
    "#34D399", "#10B981", "#047857", "#FB923C", "#6366F1", "#EC4899"
)

@Composable
fun ColorSwatchRow(
    label: String,
    selectedHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFE2E8F0)
            )
            Text(
                text = selectedHex.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF38BDF8)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(STUDIO_COLOR_PALETTE) { hex ->
                val parsed = Color(StudioCanvasRenderer.parseHexColor(hex))
                val isSelected = selectedHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(parsed)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF38BDF8) else Color(0x55FFFFFF),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "انتخاب شده",
                            tint = if (hex == "#FFFFFF" || hex == "#F8FAFC" || hex == "#FDE047") Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudioSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueSuffix: String = "",
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFE2E8F0)
            )
            Text(
                text = "${String.format(java.util.Locale.US, "%.1f", value)}$valueSuffix",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF38BDF8),
                activeTrackColor = Color(0xFF38BDF8),
                inactiveTrackColor = Color(0xFF2E3A4F)
            ),
            modifier = Modifier.height(32.dp)
        )
    }
}

@Composable
fun StudioSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF04131D),
                checkedTrackColor = Color(0xFF38BDF8),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}

@Composable
fun SubSectionChipBar(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items.size) { index ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color(0xFF38BDF8) else Color(0xFF1C2331))
                    .border(
                        width = 1.dp,
                        color = if (selected) Color(0xFF38BDF8) else Color(0xFF2E3A4F),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color(0xFF04131D) else Color(0xFFCBD5E1),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

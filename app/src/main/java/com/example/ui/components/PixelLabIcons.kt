package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalStudioPalette

/**
 * Authentic PixelLab Tab 1 Icon: Triple overlapping circles (My Projects / Presets).
 */
@Composable
fun PixelLabPresetsTabIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val r = size.minDimension * 0.28f
        val strokeWidth = 1.8f.dp.toPx()
        val c = center

        // Top circle
        drawCircle(
            color = tint,
            radius = r,
            center = Offset(c.x, c.y - r * 0.52f),
            style = Stroke(width = strokeWidth)
        )
        // Bottom-left circle
        drawCircle(
            color = tint,
            radius = r,
            center = Offset(c.x - r * 0.55f, c.y + r * 0.45f),
            style = Stroke(width = strokeWidth)
        )
        // Bottom-right circle
        drawCircle(
            color = tint,
            radius = r,
            center = Offset(c.x + r * 0.55f, c.y + r * 0.45f),
            style = Stroke(width = strokeWidth)
        )
    }
}

/**
 * Authentic PixelLab Tab 2 Icon: Bold stylized letter 'A' (Text Layer Tools).
 */
@Composable
fun PixelLabTextTabIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "A",
            color = tint,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Authentic PixelLab Tab 3 Icon: Geometric Hexagon (Stickers, Import, Draw, Shapes, Bezier, Arrow).
 */
@Composable
fun PixelLabHexagonTabIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2f.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.5f, 2f)
            lineTo(w - 2f, h * 0.26f)
            lineTo(w - 2f, h * 0.74f)
            lineTo(w * 0.5f, h - 2f)
            lineTo(2f, h * 0.74f)
            lineTo(2f, h * 0.26f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Authentic PixelLab Tab 4 Icon: Dual overlapping squares (Canvas Background & Size).
 */
@Composable
fun PixelLabCanvasBgTabIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val s = size.minDimension * 0.62f
        val stroke = 1.8f.dp.toPx()

        // Background square
        drawRoundRect(
            color = tint.copy(alpha = 0.55f),
            topLeft = Offset(size.width - s - 1f, 1f),
            size = androidx.compose.ui.geometry.Size(s, s),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f.dp.toPx()),
            style = Stroke(width = stroke)
        )
        // Foreground square
        drawRoundRect(
            color = tint,
            topLeft = Offset(1f, size.height - s - 1f),
            size = androidx.compose.ui.geometry.Size(s, s),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f.dp.toPx()),
            style = Stroke(width = stroke)
        )
    }
}

/**
 * Authentic PixelLab Tab 5 Icon: Magic wand with sparkle rays (Post-FX Filters).
 */
@Composable
fun PixelLabWandTabIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2f.dp.toPx()

        // Diagonal wand stick
        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.8f),
            end = Offset(w * 0.65f, h * 0.35f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Sparkle star at the tip
        val tipX = w * 0.72f
        val tipY = h * 0.28f
        val ray = w * 0.18f

        drawLine(color = tint, start = Offset(tipX - ray, tipY), end = Offset(tipX + ray, tipY), strokeWidth = 1.6f.dp.toPx())
        drawLine(color = tint, start = Offset(tipX, tipY - ray), end = Offset(tipX, tipY + ray), strokeWidth = 1.6f.dp.toPx())
        drawCircle(color = tint, radius = 2f.dp.toPx(), center = Offset(tipX, tipY))
    }
}

/**
 * A standard PixelLab Tool Grid Button (Icon on top, label below, 48dp minimum touch target).
 */
@Composable
fun PixelLabToolItem(
    label: String,
    icon: ImageVector? = null,
    customIcon: (@Composable () -> Unit)? = null,
    badgeText: String? = null,
    isActive: Boolean = false,
    testTag: String = "",
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val itemBg = if (isActive) palette.primaryAccent.copy(alpha = 0.2f) else Color.Transparent
    val tint = if (isActive) palette.primaryAccent else palette.textPrimary

    Column(
        modifier = Modifier
            .width(72.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(itemBg)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (customIcon != null) {
                customIcon()
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(palette.secondaryAccent, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isActive) palette.primaryAccent else palette.textSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The authentic PixelLab Sub-Editor Header with Apply (✔) and Discard (✖) buttons.
 */
@Composable
fun PixelLabSubEditorHeader(
    title: String,
    subtitle: String? = null,
    onApply: () -> Unit,
    onDiscard: () -> Unit
) {
    val palette = LocalStudioPalette.current

    Surface(
        color = palette.surfaceVariant.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Discard (Cross)
            IconButton(
                onClick = onDiscard,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("tool_discard_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "انصراف",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }

            // Apply (Checkmark)
            IconButton(
                onClick = onApply,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("tool_apply_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "تایید",
                    tint = palette.primaryAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

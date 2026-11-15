package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DrawingLayer
import com.example.data.ImageLayer
import com.example.data.ShapeLayer
import com.example.data.StudioLayer
import com.example.data.TextLayer
import com.example.ui.StudioBottomTab
import com.example.ui.agent.GeminiModelPreset
import com.example.ui.agent.formatModelShortBadge
import com.example.ui.theme.LocalStudioPalette

@Composable
fun StudioTopBar(
    isZenFullscreenMode: Boolean,
    selectedLayer: StudioLayer?,
    canUndo: Boolean,
    canRedo: Boolean,
    isCanvasExpandedMode: Boolean,
    showGrid: Boolean,
    isReelsSafeZoneVisible: Boolean = false,
    canvasZoomScale: Float = 1.0f,
    isZoomPanModeActive: Boolean = false,
    totalLayersCount: Int,
    statusBanner: String?,
    selectedModelName: String = "gemini-3.8-flash",
    onSelectModelName: (String) -> Unit = {},
    onEditSelectedLayer: () -> Unit,
    onDeleteSelectedLayer: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleCanvasExpanded: () -> Unit,
    onToggleZoomPanMode: () -> Unit = {},
    onToggleGrid: () -> Unit,
    onToggleReelsSafeZone: () -> Unit = {},
    onOpenLayersDrawer: () -> Unit,
    onAddLayer: () -> Unit,
    onOpenSaveDialog: () -> Unit,
    onShareCanvas: () -> Unit,
    onRandomQuote: () -> Unit,
    onToggleZenFullscreen: () -> Unit,
    onOpenOptionsDialog: () -> Unit,
    onClearStatusBanner: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val zoomPercent = (canvasZoomScale * 100f).toInt().coerceIn(100, 500)
    var isModelMenuExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !isZenFullscreenMode,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Material 3 Top-Center Floating Island Toolbar
            Surface(
                color = palette.surface.copy(alpha = 0.94f),
                tonalElevation = 10.dp,
                shadowElevation = 10.dp,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            palette.primaryAccent.copy(alpha = 0.45f),
                            palette.secondaryAccent.copy(alpha = 0.35f),
                            palette.primaryAccent.copy(alpha = 0.45f)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("studio_top_floating_island")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Cluster: Quick Action Pill & Gemini Model Selector Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = palette.surfaceVariant.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, palette.border)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                            ) {
                                IconButton(
                                    onClick = onEditSelectedLayer,
                                    enabled = selectedLayer is TextLayer,
                                    modifier = Modifier.size(32.dp).testTag("top_quick_edit_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "ویرایش لایه",
                                        tint = if (selectedLayer != null) palette.primaryAccent else palette.textSecondary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onDeleteSelectedLayer,
                                    enabled = selectedLayer != null,
                                    modifier = Modifier.size(32.dp).testTag("top_quick_delete_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف لایه انتخابی",
                                        tint = if (selectedLayer != null) palette.danger else palette.textSecondary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Material 3 Gemini Model Selector Pill (Manual Switcher, Never Force-Locked)
                        Box {
                            Surface(
                                color = palette.surfaceVariant.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, palette.primaryAccent.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { isModelMenuExpanded = true }
                                    .testTag("gemini_model_selector_pill")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "انتخاب مدل جمینای",
                                        tint = palette.primaryAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = formatModelShortBadge(selectedModelName),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primaryAccent
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = palette.primaryAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isModelMenuExpanded,
                                onDismissRequest = { isModelMenuExpanded = false },
                                modifier = Modifier.background(palette.surfaceElevated)
                            ) {
                                GeminiModelPreset.entries.forEach { preset ->
                                    val isCurrent = selectedModelName.equals(preset.modelId, ignoreCase = true)
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = preset.titleFa,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isCurrent) palette.primaryAccent else palette.textPrimary
                                                )
                                                Text(
                                                    text = preset.subtitleFa,
                                                    fontSize = 10.sp,
                                                    color = palette.textSecondary
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "فعال",
                                                    tint = palette.primaryAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSelectModelName(preset.modelId)
                                            isModelMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Right Cluster: Scrollable Global Canvas Actions inside Floating Island
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        IconButton(
                            onClick = onUndo,
                            enabled = canUndo,
                            modifier = Modifier.size(34.dp).testTag("undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "بازگشت به عقب (Undo)",
                                tint = if (canUndo) palette.textPrimary else palette.textSecondary.copy(alpha = 0.35f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onRedo,
                            enabled = canRedo,
                            modifier = Modifier.size(34.dp).testTag("redo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "انجام مجدد (Redo)",
                                tint = if (canRedo) palette.textPrimary else palette.textSecondary.copy(alpha = 0.35f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        // PixelLab Magnifier Zoom Toggle & Percentage Badge (100%..500%)
                        Surface(
                            color = if (isZoomPanModeActive || zoomPercent > 100) {
                                palette.primaryAccent.copy(alpha = 0.22f)
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = if (isZoomPanModeActive || zoomPercent > 100) {
                                BorderStroke(1.dp, palette.primaryAccent)
                            } else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onToggleZoomPanMode() }
                                .padding(horizontal = 5.dp, vertical = 4.dp)
                                .testTag("zoom_toggle_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = "ابزار زوم و جابه‌جایی بوم ($zoomPercent%)",
                                    tint = if (isZoomPanModeActive || zoomPercent > 100) palette.primaryAccent else palette.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "$zoomPercent%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isZoomPanModeActive || zoomPercent > 100) palette.primaryAccent else palette.textSecondary,
                                    modifier = Modifier.testTag("zoom_percent_label")
                                )
                            }
                        }
                        IconButton(
                            onClick = onToggleCanvasExpanded,
                            modifier = Modifier.size(32.dp).testTag("toggle_canvas_expand_button")
                        ) {
                            Icon(
                                imageVector = if (isCanvasExpandedMode) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = "تناسب بوم (Fit)",
                                tint = if (isCanvasExpandedMode) palette.secondaryAccent else palette.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleGrid,
                            modifier = Modifier.size(32.dp).testTag("grid_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "شبکه راهنما (Grid)",
                                tint = if (showGrid) palette.primaryAccent else palette.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleReelsSafeZone,
                            modifier = Modifier.size(34.dp).testTag("reels_safe_zone_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "کادر امن ریلز و استوری اینستاگرام",
                                tint = if (isReelsSafeZoneVisible) palette.primaryAccent else palette.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenLayersDrawer,
                            modifier = Modifier.size(34.dp).testTag("open_layers_drawer_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = palette.primaryAccent,
                                        contentColor = Color(0xFF04131D)
                                    ) {
                                        Text("$totalLayersCount")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "پنل لایه‌ها (Layers)",
                                    tint = palette.textPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onAddLayer,
                            modifier = Modifier.size(34.dp).testTag("top_add_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "افزودن المان (+)",
                                tint = palette.primaryAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenSaveDialog,
                            modifier = Modifier.size(34.dp).testTag("top_save_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = "ذخیره در گالری یا خروجی PLP",
                                tint = palette.primaryAccent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onShareCanvas,
                            modifier = Modifier.size(34.dp).testTag("share_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "اشتراک‌گذاری (Share)",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onRandomQuote,
                            modifier = Modifier.size(34.dp).testTag("quotes_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = "نقل‌قول‌ها (Quotes)",
                                tint = palette.secondaryAccent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleZenFullscreen,
                            modifier = Modifier.size(34.dp).testTag("zen_fullscreen_button")
                        ) {
                            Icon(
                                imageVector = if (isZenFullscreenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "مشاهده خالص تمام‌صفحه (Zen Mode)",
                                tint = if (isZenFullscreenMode) palette.primaryAccent else palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenOptionsDialog,
                            modifier = Modifier.size(34.dp).testTag("menu_more_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "تنظیمات بیشتر (Options)",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // Optional non-overlapping status banner below the floating island
            AnimatedVisibility(visible = statusBanner != null) {
                Surface(
                    color = Color(0xFF0C4A6E),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, palette.primaryAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClearStatusBanner() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusBanner ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE0F2FE),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "بستن",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Direct Inline Canvas Property Chips Bar
 * Appears directly above the canvas when an object (TextLayer, ShapeLayer, ImageLayer, DrawingLayer)
 * is selected, giving 1-tap access to its key properties without opening deep menus.
 */
@Composable
fun InlineCanvasPropertyChips(
    selectedLayer: StudioLayer?,
    isZenFullscreenMode: Boolean,
    onEditText: () -> Unit,
    onToggle3d: () -> Unit,
    onCenterHorizontally: () -> Unit,
    onBringToFront: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onDuplicateLayer: () -> Unit,
    onDeleteLayer: () -> Unit,
    onOpenInspectorForLayer: (StudioBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current

    AnimatedVisibility(
        visible = selectedLayer != null && !isZenFullscreenMode,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier
    ) {
        val layer = selectedLayer ?: return@AnimatedVisibility
        Surface(
            color = palette.surfaceElevated.copy(alpha = 0.94f),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, palette.primaryAccent.copy(alpha = 0.45f)),
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("inline_canvas_property_bar")
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Active Layer Type & Name Badge
                Surface(
                    color = palette.primaryAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.primaryAccent.copy(alpha = 0.55f))
                ) {
                    val badgeLabel = when (layer) {
                        is TextLayer -> "🔤 «${layer.text.replace("\n", " ").take(14)}»"
                        is ShapeLayer -> "🔷 ${layer.shapeKind.labelFa}"
                        is ImageLayer -> "🖼️ تصویر"
                        is DrawingLayer -> "🖌️ نقاشی"
                    }
                    Text(
                        text = badgeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                when (layer) {
                    is TextLayer -> {
                        InlinePropertyChip(
                            label = "✏️ ویرایش متن",
                            accentColor = palette.primaryAccent,
                            onClick = onEditText,
                            testTag = "inline_chip_edit_text"
                        )
                        InlinePropertyChip(
                            label = if (layer.is3dEnabled) "🧊 ۳بعدی: فعال" else "🧊 ۳بعدی: خاموش",
                            accentColor = if (layer.is3dEnabled) Color(0xFF34D399) else palette.textSecondary,
                            onClick = onToggle3d,
                            testTag = "inline_chip_toggle_3d"
                        )
                        InlinePropertyChip(
                            label = "🎨 فونت و رنگ",
                            accentColor = palette.secondaryAccent,
                            onClick = { onOpenInspectorForLayer(StudioBottomTab.TEXT) },
                            testTag = "inline_chip_open_text_inspector"
                        )
                    }
                    is ShapeLayer -> {
                        InlinePropertyChip(
                            label = if (layer.is3dExtruded) "🧊 حجم ۳بعدی: فعال" else "🧊 حجم ۳بعدی: خاموش",
                            accentColor = if (layer.is3dExtruded) Color(0xFF34D399) else palette.textSecondary,
                            onClick = onToggle3d,
                            testTag = "inline_chip_toggle_3d"
                        )
                        InlinePropertyChip(
                            label = "🔷 استایل شکل",
                            accentColor = palette.secondaryAccent,
                            onClick = { onOpenInspectorForLayer(StudioBottomTab.OBJECTS) },
                            testTag = "inline_chip_open_shape_inspector"
                        )
                    }
                    is ImageLayer -> {
                        InlinePropertyChip(
                            label = "✂️ برش و ماسک",
                            accentColor = palette.primaryAccent,
                            onClick = { onOpenInspectorForLayer(StudioBottomTab.OBJECTS) },
                            testTag = "inline_chip_crop_image"
                        )
                        InlinePropertyChip(
                            label = "↔️ قرینه افقی",
                            accentColor = if (layer.flipHorizontal) Color(0xFF34D399) else palette.textSecondary,
                            onClick = onFlipHorizontal,
                            testTag = "inline_chip_flip_h"
                        )
                    }
                    is DrawingLayer -> {
                        InlinePropertyChip(
                            label = "🖌️ تنظیمات قلم",
                            accentColor = palette.primaryAccent,
                            onClick = { onOpenInspectorForLayer(StudioBottomTab.OBJECTS) },
                            testTag = "inline_chip_drawing_tools"
                        )
                    }
                }

                InlinePropertyChip(
                    label = "🎯 وسط‌چین",
                    accentColor = palette.textPrimary,
                    onClick = onCenterHorizontally,
                    testTag = "inline_chip_center_layer"
                )
                InlinePropertyChip(
                    label = "⬆️ روترین",
                    accentColor = palette.textPrimary,
                    onClick = onBringToFront,
                    testTag = "inline_chip_bring_front"
                )
                InlinePropertyChip(
                    label = "📑 کپی",
                    accentColor = Color(0xFF34D399),
                    onClick = onDuplicateLayer,
                    testTag = "inline_chip_duplicate"
                )
                InlinePropertyChip(
                    label = "🗑️ حذف",
                    accentColor = palette.danger,
                    onClick = onDeleteLayer,
                    testTag = "inline_chip_delete"
                )
            }
        }
    }
}

@Composable
private fun InlinePropertyChip(
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalStudioPalette.current
    Surface(
        color = palette.surfaceVariant.copy(alpha = 0.88f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StudioBottomControls(
    isZenFullscreenMode: Boolean,
    isInspectorCollapsed: Boolean,
    activeTab: StudioBottomTab,
    totalLayersCount: Int,
    onToggleInspectorCollapsed: () -> Unit,
    onSelectTab: (StudioBottomTab) -> Unit,
    onOpenLayerDrawer: () -> Unit,
    inspectorContent: @Composable () -> Unit
) {
    val palette = LocalStudioPalette.current

    AnimatedVisibility(
        visible = !isZenFullscreenMode,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // A. Collapsible Material 3 Floating Bottom Docking Sheet
            AnimatedVisibility(
                visible = !isInspectorCollapsed,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = palette.surface.copy(alpha = 0.97f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                palette.border,
                                palette.primaryAccent.copy(alpha = 0.35f),
                                palette.border
                            )
                        )
                    ),
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .height(216.dp)
                ) {
                    Column {
                        // Header handle & collapse toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(palette.primaryAccent.copy(alpha = 0.65f))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (activeTab) {
                                        StudioBottomTab.PRESETS -> "پروژه‌ها و خروجی رسمی PLP"
                                        StudioBottomTab.TEXT -> "تایپوگرافی، متن سه‌بعدی و فونت‌ها"
                                        StudioBottomTab.OBJECTS -> "اشکال هندسی، استیکرها و نقاشی"
                                        StudioBottomTab.CANVAS_BG -> "تنظیمات پس‌زمینه و رنگ بوم"
                                        StudioBottomTab.POST_FX -> "فیلترها و افکت‌های پایانی استودیو"
                                        StudioBottomTab.AI_STUDIO -> "ترمینال ایجنت هوشمند و استودیو AI"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            }

                            IconButton(
                                onClick = onToggleInspectorCollapsed,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "بستن پنل ابزار",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Inspector Content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            inspectorContent()
                        }
                    }
                }
            }

            // B. Material 3 Floating Bottom Dock Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = palette.surface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, palette.border),
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            Triple(StudioBottomTab.PRESETS, "tab_presets", "پروژه‌ها"),
                            Triple(StudioBottomTab.TEXT, "tab_text", "متن"),
                            Triple(StudioBottomTab.OBJECTS, "tab_objects", "اشکال"),
                            Triple(StudioBottomTab.CANVAS_BG, "tab_canvas_bg", "پس‌زمینه"),
                            Triple(StudioBottomTab.POST_FX, "tab_post_fx", "افکت‌ها"),
                            Triple(StudioBottomTab.AI_STUDIO, "tab_ai_studio", "ایجنت")
                        )

                        tabs.forEach { (tab, tag, label) ->
                            val isSelected = activeTab == tab && !isInspectorCollapsed
                            val activeTint = if (tab == StudioBottomTab.AI_STUDIO) palette.secondaryAccent else palette.primaryAccent
                            val tabTint = if (isSelected) activeTint else palette.textSecondary

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) activeTint.copy(alpha = 0.18f) else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) activeTint.copy(alpha = 0.45f) else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        if (activeTab == tab && !isInspectorCollapsed) {
                                            onToggleInspectorCollapsed()
                                        } else {
                                            onSelectTab(tab)
                                        }
                                    }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                                    .testTag(tag),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                                    when (tab) {
                                        StudioBottomTab.PRESETS -> PixelLabPresetsTabIcon(tint = tabTint)
                                        StudioBottomTab.TEXT -> PixelLabTextTabIcon(tint = tabTint)
                                        StudioBottomTab.OBJECTS -> PixelLabHexagonTabIcon(tint = tabTint)
                                        StudioBottomTab.CANVAS_BG -> PixelLabCanvasBgTabIcon(tint = tabTint)
                                        StudioBottomTab.POST_FX -> PixelLabWandTabIcon(tint = tabTint)
                                        StudioBottomTab.AI_STUDIO -> Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "ایجنت هوشمند",
                                            tint = tabTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tabTint,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        // Quick Right-Side Layers Drawer trigger with live badge
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenLayerDrawer() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("dock_quick_layers_button"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = palette.secondaryAccent,
                                        contentColor = Color.White
                                    ) {
                                        Text("$totalLayersCount")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "لایه‌ها",
                                    tint = palette.secondaryAccent,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "لایه‌ها",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = palette.secondaryAccent
                            )
                        }

                        // Collapse / Expand Pill Toggle
                        IconButton(
                            onClick = onToggleInspectorCollapsed,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("dock_collapse_expand_toggle")
                        ) {
                            Icon(
                                imageVector = if (isInspectorCollapsed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isInspectorCollapsed) "نمایش ابزارها" else "بستن ابزارها",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


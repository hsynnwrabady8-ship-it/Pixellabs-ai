package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DrawingLayer
import com.example.data.ImageLayer
import com.example.data.LayerBlendMode
import com.example.data.ShapeLayer
import com.example.data.StudioLayer
import com.example.data.TextLayer

@Composable
fun LayerDrawerSheet(
    layers: List<StudioLayer>,
    selectedLayerId: String?,
    hasCopiedStyle: Boolean = false,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMergeVisibleLayers: () -> Unit,
    onMergeSelectedLayers: (Set<String>) -> Unit = {},
    onDeleteSelectedLayers: (Set<String>) -> Unit = {},
    onGroupSelectedLayers: (Set<String>) -> Unit = {},
    onUngroupSelectedLayers: (Set<String>) -> Unit = {},
    onCopyLayerStyle: (String) -> Unit = {},
    onPasteLayerStyle: (String) -> Unit = {},
    onUpdateSelectedOpacity: (Float) -> Unit,
    onUpdateSelectedBlendMode: (LayerBlendMode) -> Unit,
    onUnlockAllLayers: () -> Unit = {},
    onClearAllLayers: () -> Unit = {},
    onResetCanvas: () -> Unit = {},
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val selectedLayer = layers.find { it.id == selectedLayerId }
    val palette = com.example.ui.theme.LocalStudioPalette.current

    var isMultiSelectMode by remember { mutableStateOf(false) }
    val checkedLayerIds = remember { mutableStateListOf<String>() }

    // Material 3 Right-Side Slide-In Drawer (In RTL layout, CenterStart is the physical Right side)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99040812))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("right_layer_drawer_overlay")
    ) {
        Surface(
            color = palette.surface.copy(alpha = 0.98f),
            contentColor = palette.textPrimary,
            tonalElevation = 12.dp,
            shadowElevation = 20.dp,
            shape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp),
            border = BorderStroke(1.dp, palette.primaryAccent.copy(alpha = 0.35f)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .widthIn(max = 365.dp)
                .fillMaxWidth(0.88f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .testTag("right_layer_drawer_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "مدیریت لایه‌های بوم (${layers.size} لایه)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = if (isMultiSelectMode) {
                                "${checkedLayerIds.size} لایه برای ادغام یا عملیات گروهی انتخاب شده"
                            } else {
                                "کشوی کناری لایه‌ها • از بالا (رویی‌ترین) به پایین"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMultiSelectMode) Color(0xFFFDE047) else Color(0xFF94A3B8)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_layers_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن کشوی لایه‌ها",
                            tint = palette.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            isMultiSelectMode = !isMultiSelectMode
                            if (!isMultiSelectMode) checkedLayerIds.clear()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMultiSelectMode) Color(0xFF0284C7) else Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("toggle_multiselect_layers_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "انتخاب چندلایه",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isMultiSelectMode) "لغو انتخاب" else "انتخاب چندلایه",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Button(
                        onClick = {
                            if (isMultiSelectMode && checkedLayerIds.size >= 2) {
                                onMergeSelectedLayers(checkedLayerIds.toSet())
                                checkedLayerIds.clear()
                                isMultiSelectMode = false
                            } else {
                                onMergeVisibleLayers()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color(0xFF38BDF8)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("merge_layers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MergeType,
                            contentDescription = "ادغام لایه‌ها",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isMultiSelectMode && checkedLayerIds.size >= 2) {
                                "ادغام (${checkedLayerIds.size})"
                            } else {
                                "ادغام همه"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

            // PixelLab Multi-Layer Action Bar when Multi-Select Mode is active
            if (isMultiSelectMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (checkedLayerIds.size == layers.size) {
                                    checkedLayerIds.clear()
                                } else {
                                    checkedLayerIds.clear()
                                    checkedLayerIds.addAll(layers.map { it.id })
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFFCBD5E1)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (checkedLayerIds.size == layers.size) "لغو همه" else "انتخاب همه",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Button(
                            onClick = {
                                if (checkedLayerIds.size >= 2) {
                                    onMergeSelectedLayers(checkedLayerIds.toSet())
                                    checkedLayerIds.clear()
                                    isMultiSelectMode = false
                                }
                            },
                            enabled = checkedLayerIds.size >= 2,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_merge_selected_layers_btn")
                        ) {
                            Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ادغام لایه‌های تیک‌خورده (${checkedLayerIds.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val anyInGroup = layers.any { it.id in checkedLayerIds && it.groupId != null }
                        Button(
                            onClick = {
                                if (anyInGroup) {
                                    onUngroupSelectedLayers(checkedLayerIds.toSet())
                                } else if (checkedLayerIds.size >= 2) {
                                    onGroupSelectedLayers(checkedLayerIds.toSet())
                                }
                                checkedLayerIds.clear()
                                isMultiSelectMode = false
                            },
                            enabled = anyInGroup || checkedLayerIds.size >= 2,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (anyInGroup) Color(0xFF4C1D95) else Color(0xFF4338CA),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("group_selected_layers_btn")
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (anyInGroup) "تفکیک گروه" else "گروه‌بندی",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (checkedLayerIds.isNotEmpty()) {
                                    onDeleteSelectedLayers(checkedLayerIds.toSet())
                                    checkedLayerIds.clear()
                                    isMultiSelectMode = false
                                }
                            },
                            enabled = checkedLayerIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3F1D2B),
                                contentColor = Color(0xFFF87171)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("حذف (${checkedLayerIds.size})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Canvas & Layer Management Bar + Copy/Paste Style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedLayer != null) {
                    Button(
                        onClick = { onCopyLayerStyle(selectedLayer.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E1B4B),
                            contentColor = Color(0xFFE9D5FF)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_layer_style_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("کپی استایل", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }

                    Button(
                        onClick = { onPasteLayerStyle(selectedLayer.id) },
                        enabled = hasCopiedStyle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF064E3B),
                            contentColor = Color(0xFF6EE7B7)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("paste_layer_style_btn")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("پیست استایل", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }

                Button(
                    onClick = onUnlockAllLayers,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFFFBBF24)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("unlock_all_layers_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "باز کردن قفل همه",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("باز کردن قفل‌ها", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }

                Button(
                    onClick = onClearAllLayers,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3F1D2B),
                        contentColor = Color(0xFFF87171)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clear_all_layers_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف همه لایه‌ها",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("حذف همه", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Layer Opacity & Blend Mode Inspector inside Layer Sheet
            if (selectedLayer != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2331)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        StudioSliderRow(
                            label = "شفافیت لایه فعال (${selectedLayer.name})",
                            value = selectedLayer.opacity * 100f,
                            valueRange = 5f..100f,
                            valueSuffix = "%",
                            onValueChange = { onUpdateSelectedOpacity(it / 100f) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "حالت ترکیب رنگ (Blend Mode):",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(LayerBlendMode.entries) { mode ->
                                val isCurrent = selectedLayer.blendMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) Color(0xFF38BDF8) else Color(0xFF0F172A))
                                        .clickable { onUpdateSelectedBlendMode(mode) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = mode.labelFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrent) Color(0xFF04131D) else Color.White,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Display layers from top-most (last in list) to bottom-most (first in list)
            val reversedLayers = layers.reversed()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reversedLayers, key = { it.id }) { layer ->
                    val isSelected = layer.id == selectedLayerId
                    val isCheckedForMerge = layer.id in checkedLayerIds
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected || isCheckedForMerge) 1.5.dp else 1.dp,
                                color = when {
                                    isCheckedForMerge -> Color(0xFFFDE047)
                                    isSelected -> Color(0xFF38BDF8)
                                    else -> Color(0xFF2E3A4F)
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                if (isMultiSelectMode) {
                                    if (isCheckedForMerge) checkedLayerIds.remove(layer.id)
                                    else checkedLayerIds.add(layer.id)
                                } else {
                                    onSelectLayer(layer.id)
                                }
                            }
                            .testTag("layer_item_${layer.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isCheckedForMerge -> Color(0xFF27241D)
                                isSelected -> Color(0xFF1E293B)
                                else -> Color(0xFF161D2A)
                            }
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isMultiSelectMode) {
                                Checkbox(
                                    checked = isCheckedForMerge,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (layer.id !in checkedLayerIds) checkedLayerIds.add(layer.id)
                                        } else {
                                            checkedLayerIds.remove(layer.id)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFFFACC15),
                                        checkmarkColor = Color(0xFF090D16),
                                        uncheckedColor = Color(0xFF94A3B8)
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (layer) {
                                    is TextLayer -> Icons.Default.Title
                                    is ShapeLayer -> Icons.Default.Hexagon
                                    is ImageLayer -> Icons.Default.Image
                                    is DrawingLayer -> Icons.Default.Brush
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = layer.name,
                                    tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = layer.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitle = when (layer) {
                                    is TextLayer -> layer.text.replace("\n", " ")
                                    is ShapeLayer -> layer.shapeKind.labelFa
                                    is ImageLayer -> "تصویر / استیکر"
                                    is DrawingLayer -> "${layer.strokes.size} مسیر قلم"
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { onMoveUp(layer.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "انتقال به لایه بالاتر",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMoveDown(layer.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "انتقال به لایه پایین‌تر",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onToggleVisibility(layer.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "نمایش یا مخفی کردن لایه",
                                    tint = if (layer.isVisible) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onToggleLock(layer.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "قفل کردن لایه",
                                    tint = if (layer.isLocked) Color(0xFFFBBF24) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDuplicate(layer.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "کپی لایه",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDelete(layer.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف لایه",
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}


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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BUILTIN_PIXELLAB_QUOTES
import com.example.data.BUILTIN_PIXELLAB_STICKERS
import com.example.data.BUILTIN_REELS_TEMPLATES
import com.example.data.BUILTIN_TEXT_PRESETS
import com.example.data.BackgroundType
import com.example.data.BuiltinStickerItem
import com.example.data.CanvasAspectPreset
import com.example.data.ReelsCoverTemplate
import com.example.data.CanvasBackground
import com.example.data.CanvasPostFx
import com.example.data.CustomUserFont
import com.example.data.DEFAULT_ASPECT_PRESETS
import com.example.data.CropAspectPreset
import com.example.data.DrawingLayer
import com.example.data.ImageLayer
import com.example.data.MaskPresetMode
import com.example.data.ShapeKind
import com.example.data.ShapeLayer
import com.example.data.StudioFont
import com.example.data.StudioLayer
import com.example.data.StudioProjectDoc
import com.example.data.StudioQuoteItem
import com.example.data.TextAlignMode
import com.example.data.TextColorSpan
import com.example.data.TextLayer
import com.example.data.TextStylePreset
import com.example.data.TexturePreset
import com.example.ui.AiTaskUiState
import com.example.ui.CloudProjectsUiState
import com.example.ui.agent.PlpLearnedStyleDNA
import com.example.ui.theme.LocalStudioPalette
import java.io.File
import kotlin.math.roundToInt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

// ============================================================================
// TAB 1: PRESETS, QUOTES, ASPECT RATIOS & PLP / CLOUD PROJECTS
// ============================================================================
@Composable
fun PresetsAndPlpInspector(
    projectTitle: String,
    canvasWidth: Int = 1080,
    canvasHeight: Int = 1080,
    cloudState: CloudProjectsUiState,
    localPlpFiles: List<File>,
    learnedPlpStyles: List<PlpLearnedStyleDNA> = emptyList(),
    activeLearnedStyle: PlpLearnedStyleDNA? = null,
    isReelsSafeZoneVisible: Boolean = false,
    onToggleReelsSafeZone: () -> Unit = {},
    onApplyReelsTemplate: (ReelsCoverTemplate) -> Unit = {},
    onGenerateReelsCover: (topic: String, category: String) -> Unit = { _, _ -> },
    onApplyMemeGenerator: () -> Unit = {},
    onApplyTextPreset: (TextStylePreset) -> Unit = {},
    onApplyQuote: (StudioQuoteItem) -> Unit = {},
    onSelectAspectPreset: (CanvasAspectPreset) -> Unit = {},
    onOpenCustomSizeDialog: () -> Unit = {},
    onSaveCloudAndPlp: () -> Unit = {},
    onExportPlpFile: () -> Unit = {},
    onImportPlpFile: () -> Unit = {},
    onOpenLocalPlp: (File) -> Unit = {},
    onLearnCurrentCanvasStyle: () -> Unit = {},
    onLearnFromLocalPlpFile: (File) -> Unit = {},
    onGenerateFromLearnedStyle: (PlpLearnedStyleDNA, String, String) -> Unit = { _, _, _ -> },
    onApplyLearnedStyleToCanvas: (PlpLearnedStyleDNA) -> Unit = {},
    onSelectLearnedStyle: (PlpLearnedStyleDNA) -> Unit = {},
    onDeleteLearnedStyle: (String) -> Unit = {},
    onLoadCloudDoc: (StudioProjectDoc) -> Unit = {},
    onDeleteCloudDoc: (String) -> Unit = {},
    onOpenCloudSignIn: () -> Unit = {}
) {
    val palette = LocalStudioPalette.current
    var subTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Quick Category Selector (Scrollable to comfortably house 5 tabs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf("کاور ریلز اینستا", "قالب‌های سه‌بعدی", "نقل‌قول‌های ادبی", "فایل‌های PLP و ابر", "ابعاد و نسبت")
            tabs.forEachIndexed { idx, title ->
                val isSelected = subTab == idx
                Surface(
                    color = if (isSelected) palette.primaryAccent.copy(alpha = 0.22f) else palette.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) BorderStroke(1.dp, palette.primaryAccent) else null,
                    modifier = Modifier.clickable { subTab = idx }
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) palette.primaryAccent else palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                        maxLines = 1
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (subTab) {
                0 -> {
                    var reelsTopic by remember { mutableStateOf("") }
                    var selectedCategory by remember { mutableStateOf("تکنولوژی") }
                    val categories = listOf("تکنولوژی", "بلاگری", "کسب‌وکار", "فروشگاهی", "سینمایی")

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Safe Zone Toggle & Quick Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isReelsSafeZoneVisible) palette.primaryAccent.copy(alpha = 0.22f) else palette.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isReelsSafeZoneVisible) palette.primaryAccent else palette.border),
                                modifier = Modifier
                                    .clickable { onToggleReelsSafeZone() }
                                    .testTag("toggle_reels_safe_zone_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Crop,
                                        contentDescription = null,
                                        tint = if (isReelsSafeZoneVisible) palette.primaryAccent else palette.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isReelsSafeZoneVisible) "ناحیه امن ریلز: فعال (کادر ۱:۱)" else "نمایش ناحیه امن ریلز و استوری",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isReelsSafeZoneVisible) palette.primaryAccent else palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "کادر امن اینستاگرام",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryAccent,
                                fontSize = 10.sp
                            )
                        }

                        // AI Smart Reels Cover Generator Card
                        Surface(
                            color = palette.surfaceVariant.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, palette.primaryAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = palette.primaryAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "طراحی خودکار کاور ریلز با هوش مصنوعی",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                    }
                                    Text(
                                        text = "ابعاد ۹:۱۶",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.primaryAccent,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = reelsTopic,
                                        onValueChange = { reelsTopic = it },
                                        placeholder = {
                                            Text("موضوع ریلز (مثلاً: ۱۰ ترفند هوش مصنوعی ۲۰۲۶)", fontSize = 10.sp, color = palette.textSecondary)
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("reels_topic_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = palette.primaryAccent,
                                            unfocusedBorderColor = palette.border,
                                            focusedTextColor = palette.textPrimary,
                                            unfocusedTextColor = palette.textPrimary
                                        ),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Button(
                                        onClick = {
                                            val t = reelsTopic.ifBlank { "رازهای موفقیت ۲۰۲۶" }
                                            onGenerateReelsCover(t, selectedCategory)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.primaryAccent,
                                            contentColor = Color(0xFF04131D)
                                        ),
                                        modifier = Modifier
                                            .height(44.dp)
                                            .testTag("generate_reels_cover_btn")
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("طراحی ریلز", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    categories.forEach { cat ->
                                        val isCatSelected = selectedCategory == cat
                                        Surface(
                                            color = if (isCatSelected) palette.secondaryAccent.copy(alpha = 0.25f) else palette.surface,
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, if (isCatSelected) palette.secondaryAccent else palette.border),
                                            modifier = Modifier.clickable { selectedCategory = cat }
                                        ) {
                                            Text(
                                                text = cat,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCatSelected) palette.secondaryAccent else palette.textSecondary,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Built-in Template Cards
                        Text(
                            text = "قالب‌های آماده ریلز (چیدمان متمرکز در کادر امن ۱:۱):",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryAccent,
                            fontWeight = FontWeight.Bold
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(BUILTIN_REELS_TEMPLATES) { template ->
                                Card(
                                    modifier = Modifier
                                        .width(165.dp)
                                        .clickable { onApplyReelsTemplate(template) },
                                    colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, palette.border)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = Color(android.graphics.Color.parseColor(template.badgeColorHex)).copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = template.badgeLabel,
                                                    color = Color(android.graphics.Color.parseColor(template.badgeColorHex)),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = template.categoryFa,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary,
                                                fontSize = 8.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = template.headline,
                                            fontFamily = FontFamily(Font(template.font.fontRes)),
                                            color = Color(android.graphics.Color.parseColor(template.headlinePrimaryHex)),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = template.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary,
                                            fontSize = 8.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { onApplyReelsTemplate(template) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(26.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = palette.primaryAccent.copy(alpha = 0.2f),
                                                contentColor = palette.primaryAccent
                                            )
                                        ) {
                                            Text("اعمال قالب ۹:۱۶", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(BUILTIN_TEXT_PRESETS) { preset ->
                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(115.dp)
                                    .clickable { onApplyTextPreset(preset) },
                                colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = preset.titleFa,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily(Font(preset.font.fontRes)),
                                        color = Color(android.graphics.Color.parseColor(preset.primaryHex)),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = preset.sampleText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(BUILTIN_PIXELLAB_QUOTES) { quote ->
                            Card(
                                modifier = Modifier
                                    .width(190.dp)
                                    .height(115.dp)
                                    .clickable { onApplyQuote(quote) },
                                colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = quote.categoryFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.secondaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = quote.text,
                                        fontFamily = FontFamily(Font(quote.recommendedFont.fontRes)),
                                        color = palette.textPrimary,
                                        fontSize = 11.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> {
                    var cloneHeadlineInput by remember { mutableStateOf("") }
                    var cloneSubtitleInput by remember { mutableStateOf("") }
                    val selectedStyle = activeLearnedStyle ?: learnedPlpStyles.firstOrNull()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onSaveCloudAndPlp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_plp_cloud_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primaryAccent,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ذخیره PLP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onExportPlpFile,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_plp_file_button"),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("خروجی .plp", fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = onImportPlpFile,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_plp_file_button"),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("باز کردن PLP", fontSize = 10.sp)
                            }
                        }

                        // AI PLP Style DNA Learning & Cloning Card
                        Surface(
                            color = palette.surfaceVariant.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, palette.secondaryAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = palette.secondaryAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "موتور هوشمند یادگیری استایل PLP (Style DNA)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                    }

                                    Surface(
                                        color = palette.secondaryAccent.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, palette.secondaryAccent),
                                        modifier = Modifier
                                            .clickable { onLearnCurrentCanvasStyle() }
                                            .testTag("learn_current_canvas_style_btn")
                                    ) {
                                        Text(
                                            text = "🧠 یادگیری استایل بوم فعلی",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.secondaryAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (learnedPlpStyles.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        contentPadding = PaddingValues(vertical = 2.dp)
                                    ) {
                                        items(learnedPlpStyles, key = { it.id }) { styleDna ->
                                            val isSelected = selectedStyle?.id == styleDna.id
                                            Card(
                                                modifier = Modifier
                                                    .width(195.dp)
                                                    .clickable { onSelectLearnedStyle(styleDna) },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFF1E1B4B) else palette.surface
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) palette.primaryAccent else palette.border
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(7.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            val c1 = runCatching { Color(android.graphics.Color.parseColor(styleDna.bgPrimaryHex)) }.getOrDefault(Color.DarkGray)
                                                            val c2 = runCatching { Color(android.graphics.Color.parseColor(styleDna.headlineStyle.colorHex)) }.getOrDefault(Color.Yellow)
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(12.dp)
                                                                    .clip(CircleShape)
                                                                    .background(c1)
                                                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                                            )
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(12.dp)
                                                                    .clip(CircleShape)
                                                                    .background(c2)
                                                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                                            )
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(
                                                                text = styleDna.aspectLabel,
                                                                fontSize = 8.sp,
                                                                color = palette.primaryAccent,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        if (learnedPlpStyles.size > 1) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "حذف استایل",
                                                                tint = palette.textSecondary,
                                                                modifier = Modifier
                                                                    .size(13.dp)
                                                                    .clickable { onDeleteLearnedStyle(styleDna.id) }
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = styleDna.styleName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = styleDna.toSummaryFa(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.textSecondary,
                                                        fontSize = 8.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Surface(
                                                            color = palette.primaryAccent.copy(alpha = 0.22f),
                                                            shape = RoundedCornerShape(6.dp),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable {
                                                                    val h = cloneHeadlineInput.ifBlank {
                                                                        styleDna.sampleHeadlineText.ifBlank { "تیتر جدید با استایل PLP" }
                                                                    }
                                                                    val s = cloneSubtitleInput.ifBlank {
                                                                        styleDna.sampleSubtitleText.ifBlank { "طراحی‌شده با موتور یادگیری استایل" }
                                                                    }
                                                                    onGenerateFromLearnedStyle(styleDna, h, s)
                                                                }
                                                        ) {
                                                            Text(
                                                                text = "✨ ساخت مشابه",
                                                                color = palette.primaryAccent,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier.padding(vertical = 4.dp)
                                                            )
                                                        }

                                                        Surface(
                                                            color = palette.secondaryAccent.copy(alpha = 0.22f),
                                                            shape = RoundedCornerShape(6.dp),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable { onApplyLearnedStyleToCanvas(styleDna) }
                                                        ) {
                                                            Text(
                                                                text = "🎨 اعمال به بوم",
                                                                color = palette.secondaryAccent,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier.padding(vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Quick Inputs to Generate a Brand New Project with the Active Learned Style
                                if (selectedStyle != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = cloneHeadlineInput,
                                            onValueChange = { cloneHeadlineInput = it },
                                            placeholder = {
                                                Text(
                                                    "تیتر جدید (مثلاً: ${selectedStyle.sampleHeadlineText.ifBlank { "آموزش هوش مصنوعی" }})",
                                                    fontSize = 9.sp,
                                                    color = palette.textSecondary
                                                )
                                            },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .testTag("plp_clone_headline_input"),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        OutlinedTextField(
                                            value = cloneSubtitleInput,
                                            onValueChange = { cloneSubtitleInput = it },
                                            placeholder = {
                                                Text(
                                                    "زیرنویس جدید (اختیاری)",
                                                    fontSize = 9.sp,
                                                    color = palette.textSecondary
                                                )
                                            },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(0.85f)
                                                .height(42.dp)
                                                .testTag("plp_clone_subtitle_input"),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        Button(
                                            onClick = {
                                                val h = cloneHeadlineInput.ifBlank {
                                                    selectedStyle.sampleHeadlineText.ifBlank { "تیتر اختصاصی جدید" }
                                                }
                                                val s = cloneSubtitleInput.ifBlank {
                                                    selectedStyle.sampleSubtitleText
                                                }
                                                onGenerateFromLearnedStyle(selectedStyle, h, s)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = palette.primaryAccent,
                                                contentColor = Color.Black
                                            ),
                                            modifier = Modifier
                                                .height(42.dp)
                                                .testTag("plp_clone_generate_btn")
                                        ) {
                                            Text("ساخت با استایل", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (localPlpFiles.isNotEmpty()) {
                            Text(
                                text = "فایل‌های PLP دستگاه (${localPlpFiles.size}) - ضربه برای باز کردن یا یادگیری استایل:",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryAccent
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(localPlpFiles) { file ->
                                    Card(
                                        modifier = Modifier.clickable { onOpenLocalPlp(file) },
                                        colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = palette.secondaryAccent, modifier = Modifier.size(14.dp))
                                            Text(file.name, style = MaterialTheme.typography.labelSmall, color = palette.textPrimary, fontSize = 10.sp)
                                            Surface(
                                                color = palette.primaryAccent.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.clickable { onLearnFromLocalPlpFile(file) }
                                            ) {
                                                Text(
                                                    text = "🧠 یادگیری",
                                                    fontSize = 8.sp,
                                                    color = palette.primaryAccent,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Card(
                                modifier = Modifier
                                    .width(136.dp)
                                    .height(115.dp)
                                    .clickable { onOpenCustomSizeDialog() }
                                    .testTag("open_custom_canvas_size_card"),
                                colors = CardDefaults.cardColors(containerColor = palette.primaryAccent.copy(alpha = 0.16f)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, palette.primaryAccent)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoSizeSelectActual,
                                        contentDescription = "ابعاد سفارشی",
                                        tint = palette.primaryAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ابعاد سفارشی (px)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.primaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "فعلی: ${canvasWidth}×${canvasHeight} px",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textPrimary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        items(DEFAULT_ASPECT_PRESETS) { preset ->
                            val isCurrent = canvasWidth == preset.width && canvasHeight == preset.height
                            Card(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(115.dp)
                                    .clickable { onSelectAspectPreset(preset) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) palette.primaryAccent.copy(alpha = 0.2f) else palette.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(if (isCurrent) 1.5.dp else 1.dp, if (isCurrent) palette.primaryAccent else palette.border)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = preset.ratioLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.primaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = preset.nameFa, style = MaterialTheme.typography.labelSmall, color = palette.textPrimary)
                                    Text(text = "${preset.width}×${preset.height}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: PIXELLAB 'A' TEXT 2-COLUMN TOOL GRID & SUB-EDITORS
// ============================================================================
enum class TextToolSubEditor {
    STYLES, EDIT, SIZE, COLOR, TEXTURE, FONT, STYLE, CURVE, BACKGROUND, ALIGN, SPACING, LINE_SPACING,
    STROKE, SHADOW, INNER_SHADOW, EMBOSS, PERSPECTIVE, ROTATE_3D, TEXT_3D, SHADOW_3D, REFLECTION,
    MASK, OPACITY, ROTATE, PADDING, POSITION, RELATIVE_POSITION, ULTRA_FX
}

private val PIXELLAB_SWATCHES = listOf(
    "#FFFFFF", "#000000", "#F2C100", "#FACC15", "#EF4444", "#F97316", "#F59E0B",
    "#10B981", "#06B6D4", "#38BDF8", "#3B82F6", "#8B5CF6", "#EC4899", "#F43F5E",
    "#84CC16", "#14B8A6", "#6366F1", "#A855F7", "#1E293B", "#332200"
)

@Composable
fun TextLayerInspector(
    selectedLayer: StudioLayer?,
    customUserFonts: List<CustomUserFont> = emptyList(),
    savedUserPresets: List<TextStylePreset> = emptyList(),
    smartPaletteHexes: List<String> = emptyList(),
    isEyedropperActive: Boolean = false,
    onToggleEyedropper: () -> Unit = {},
    onExtractSmartPalette: () -> Unit = {},
    onApplyTextPreset: (TextStylePreset) -> Unit = {},
    onSaveCurrentTextStyle: () -> Unit = {},
    onDeleteSavedTextStyle: (String) -> Unit = {},
    onAddTextLayer: () -> Unit,
    onOpenEditTextDialog: () -> Unit,
    onUpdateTextLayer: ((TextLayer) -> TextLayer) -> Unit,
    onCenterLayer: (Boolean, Boolean) -> Unit,
    onImportCustomFont: () -> Unit = {},
    onPickCustomTextureImage: () -> Unit = {},
    onApplyQuote: (StudioQuoteItem) -> Unit = {},
    onDuplicateLayer: () -> Unit = {},
    onDeleteLayer: () -> Unit = {},
    onNudgeLayer: (Float, Float) -> Unit = { _, _ -> },
    onBringToFront: () -> Unit = {},
    onSendToBack: () -> Unit = {}
) {
    val palette = LocalStudioPalette.current
    var activeSubEditor by remember { mutableStateOf<TextToolSubEditor?>(null) }

    if (selectedLayer !is TextLayer) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAddTextLayer,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("add_text_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryAccent,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("افزودن متن (Text +)", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { onApplyQuote(BUILTIN_PIXELLAB_QUOTES.first()) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("add_quote_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("نقل‌قول (Quotes)")
            }
        }
        return
    }

    val textLayer = selectedLayer

    if (activeSubEditor != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (activeSubEditor) {
                TextToolSubEditor.STYLES -> {
                    PixelLabSubEditorHeader(
                        title = "استایل‌های من و قالب‌های پیکسل‌لب (Styles)",
                        subtitle = "${savedUserPresets.size + BUILTIN_TEXT_PRESETS.size} استایل",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    MySavedTextStylesPanel(
                        savedUserPresets = savedUserPresets,
                        onApplyPreset = onApplyTextPreset,
                        onSaveCurrentLayerAsPreset = onSaveCurrentTextStyle,
                        onDeleteUserPreset = onDeleteSavedTextStyle
                    )
                }

                TextToolSubEditor.SIZE -> {
                    PixelLabSubEditorHeader(
                        title = "اندازه متن (Size)",
                        subtitle = "${textLayer.fontSizeSp.roundToInt()} sp",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = {
                            if (textLayer.fontSizeSp > 10f) onUpdateTextLayer { it.copy(fontSizeSp = it.fontSizeSp - 2f) }
                        }) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "-")
                        }
                        Slider(
                            value = textLayer.fontSizeSp,
                            onValueChange = { s -> onUpdateTextLayer { it.copy(fontSizeSp = s) } },
                            valueRange = 10f..140f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                        )
                        IconButton(onClick = {
                            if (textLayer.fontSizeSp < 140f) onUpdateTextLayer { it.copy(fontSizeSp = it.fontSizeSp + 2f) }
                        }) {
                            Icon(Icons.Default.FormatSize, contentDescription = "+")
                        }
                    }
                }

                TextToolSubEditor.POSITION -> {
                    PixelLabSubEditorHeader(
                        title = "موقعیت دقیق (Position)",
                        subtitle = "تنظیم پیکسلی با جهت‌ها",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    var stepSize by remember { mutableStateOf(0.015f) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "گام حرکت",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                fontSize = 10.sp
                            )
                            listOf(
                                0.005f to "۱ پیکسل",
                                0.015f to "۵ پیکسل",
                                0.035f to "۱۰ پیکسل"
                            ).forEach { (step, label) ->
                                val isSelected = stepSize == step
                                Surface(
                                    color = if (isSelected) palette.primaryAccent else palette.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) palette.primaryAccent else palette.border),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { stepSize = step }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.Black else palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier.size(108.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { onNudgeLayer(0f, -stepSize) },
                                modifier = Modifier.align(Alignment.TopCenter).size(36.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "بالا", tint = palette.primaryAccent, modifier = Modifier.size(22.dp))
                            }
                            IconButton(
                                onClick = { onNudgeLayer(0f, stepSize) },
                                modifier = Modifier.align(Alignment.BottomCenter).size(36.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "پایین", tint = palette.primaryAccent, modifier = Modifier.size(22.dp))
                            }
                            IconButton(
                                onClick = { onNudgeLayer(-stepSize, 0f) },
                                modifier = Modifier.align(Alignment.CenterStart).size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "چپ", tint = palette.primaryAccent, modifier = Modifier.size(22.dp))
                            }
                            IconButton(
                                onClick = { onNudgeLayer(stepSize, 0f) },
                                modifier = Modifier.align(Alignment.CenterEnd).size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "راست", tint = palette.primaryAccent, modifier = Modifier.size(22.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(palette.primaryAccent.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                TextToolSubEditor.RELATIVE_POSITION -> {
                    PixelLabSubEditorHeader(
                        title = "موقعیت نسبی ۹ جهته (Relative Position 6-Grid)",
                        subtitle = "تراز مغناطیسی دقیق به بوم",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    RelativePosition6GridPanel(
                        onSnapPosition = { nx, ny ->
                            onUpdateTextLayer {
                                it.copy(
                                    x = nx ?: it.x,
                                    y = ny ?: it.y
                                )
                            }
                        },
                        onCenterLayer = onCenterLayer
                    )
                }

                TextToolSubEditor.COLOR -> {
                    PixelLabSubEditorHeader(
                        title = "رنگ، گرادیان چندنقطه‌ای و رنگ/فونت بخشی (Color Pro)",
                        subtitle = if (textLayer.colorSpans.isNotEmpty() || textLayer.fontSpans.isNotEmpty()) {
                            "${textLayer.colorSpans.size + textLayer.fontSpans.size} بازه سفارشی"
                        } else {
                            textLayer.colorHex
                        },
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    TextColorGradientAndSpanPanel(
                        textLayer = textLayer,
                        customUserFonts = customUserFonts,
                        smartPaletteHexes = smartPaletteHexes,
                        isEyedropperActive = isEyedropperActive,
                        onToggleEyedropper = onToggleEyedropper,
                        onExtractSmartPalette = onExtractSmartPalette,
                        onUpdateTextLayer = onUpdateTextLayer
                    )
                }

                TextToolSubEditor.TEXTURE -> {
                    PixelLabSubEditorHeader(
                        title = "بافت متن (Texture)",
                        subtitle = textLayer.texturePreset.labelFa,
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Button(
                                onClick = onPickCustomTextureImage,
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصویر دلخواه", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        items(TexturePreset.entries) { preset ->
                            val isSel = textLayer.texturePreset == preset && textLayer.customTextureBitmap == null
                            Surface(
                                color = if (isSel) palette.primaryAccent else palette.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    onUpdateTextLayer { it.copy(texturePreset = preset, customTextureBitmap = null) }
                                }
                            ) {
                                Text(
                                    text = preset.labelFa,
                                    color = if (isSel) Color.Black else palette.textPrimary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مقیاس بافت:", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Slider(
                            value = textLayer.textureScale,
                            onValueChange = { sc -> onUpdateTextLayer { it.copy(textureScale = sc) } },
                            valueRange = 0.3f..3.0f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                        )
                    }
                }

                TextToolSubEditor.CURVE -> {
                    PixelLabSubEditorHeader(
                        title = "منحنی کردن متن (Curve)",
                        subtitle = "${textLayer.curveAngle.roundToInt()}°",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("-360°", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Slider(
                            value = textLayer.curveAngle,
                            onValueChange = { deg -> onUpdateTextLayer { it.copy(curveAngle = deg) } },
                            valueRange = -360f..360f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                        )
                        Text("+360°", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }

                TextToolSubEditor.ROTATE -> {
                    PixelLabSubEditorHeader(
                        title = "چرخش متن (Rotate)",
                        subtitle = "${textLayer.rotationZ.roundToInt()}°",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { onUpdateTextLayer { it.copy(rotationZ = 0f) } }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("0°", fontSize = 11.sp)
                        }
                        Slider(
                            value = textLayer.rotationZ,
                            onValueChange = { r -> onUpdateTextLayer { it.copy(rotationZ = r) } },
                            valueRange = -180f..180f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                        )
                    }
                }

                TextToolSubEditor.ROTATE_3D -> {
                    PixelLabSubEditorHeader(
                        title = "چرخش سه‌بعدی X / Y / Z (3D Rotate)",
                        subtitle = "X:${textLayer.rotationX.roundToInt()}° Y:${textLayer.rotationY.roundToInt()}° Z:${textLayer.rotationZ.roundToInt()}°",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("محور X:", fontSize = 10.sp, color = palette.textSecondary, modifier = Modifier.width(44.dp))
                            Slider(
                                value = textLayer.rotationX,
                                onValueChange = { rx -> onUpdateTextLayer { it.copy(rotationX = rx) } },
                                valueRange = -75f..75f,
                                modifier = Modifier.weight(1f).height(26.dp),
                                colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                            )
                            Text("محور Y:", fontSize = 10.sp, color = palette.textSecondary, modifier = Modifier.width(44.dp))
                            Slider(
                                value = textLayer.rotationY,
                                onValueChange = { ry -> onUpdateTextLayer { it.copy(rotationY = ry) } },
                                valueRange = -75f..75f,
                                modifier = Modifier.weight(1f).height(26.dp),
                                colors = SliderDefaults.colors(thumbColor = palette.secondaryAccent, activeTrackColor = palette.secondaryAccent)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("محور Z:", fontSize = 10.sp, color = palette.textSecondary, modifier = Modifier.width(44.dp))
                            Slider(
                                value = textLayer.rotationZ,
                                onValueChange = { rz -> onUpdateTextLayer { it.copy(rotationZ = rz) } },
                                valueRange = -180f..180f,
                                modifier = Modifier.weight(1f).height(26.dp)
                            )
                            OutlinedButton(
                                onClick = { onUpdateTextLayer { it.copy(rotationX = 0f, rotationY = 0f, rotationZ = 0f) } },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("ریست 3D", fontSize = 9.sp)
                            }
                        }
                    }
                }

                TextToolSubEditor.TEXT_3D -> {
                    PixelLabSubEditorHeader(
                        title = "متن سه‌بعدی و نورپردازی کامل (3D Text & Lighting)",
                        subtitle = if (textLayer.is3dEnabled) "عمق: ${textLayer.depth3d} | نور: ${textLayer.lightAngle3d.roundToInt()}°" else "غیرفعال",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Text3dFullLightingPanel(
                        textLayer = textLayer,
                        onUpdateTextLayer = onUpdateTextLayer
                    )
                }

                TextToolSubEditor.SHADOW_3D -> {
                    PixelLabSubEditorHeader(
                        title = "سایه زمین سه‌بعدی (3D Shadow)",
                        subtitle = if (textLayer.shadow3dEnabled) "فعال" else "غیرفعال",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = textLayer.shadow3dEnabled,
                            onCheckedChange = { en -> onUpdateTextLayer { it.copy(shadow3dEnabled = en) } }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("شدت سایه:", fontSize = 11.sp, color = palette.textSecondary)
                        Slider(
                            value = textLayer.shadow3dAlpha,
                            onValueChange = { a -> onUpdateTextLayer { it.copy(shadow3dEnabled = true, shadow3dAlpha = a) } },
                            valueRange = 0.05f..1f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }
                }

                TextToolSubEditor.REFLECTION -> {
                    PixelLabSubEditorHeader(
                        title = "انعکاس آینه‌ای (Reflection)",
                        subtitle = if (textLayer.reflectionEnabled) "فاصله: ${textLayer.reflectionOffset.roundToInt()}" else "غیرفعال",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = textLayer.reflectionEnabled,
                            onCheckedChange = { en -> onUpdateTextLayer { it.copy(reflectionEnabled = en) } }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فاصله عمودی:", fontSize = 11.sp, color = palette.textSecondary)
                        Slider(
                            value = textLayer.reflectionOffset,
                            onValueChange = { off -> onUpdateTextLayer { it.copy(reflectionEnabled = true, reflectionOffset = off) } },
                            valueRange = -20f..60f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }
                }

                TextToolSubEditor.STROKE -> {
                    PixelLabSubEditorHeader(
                        title = "حاشیه متن و خط دور دوبل (Stroke & Double Contour)",
                        subtitle = if (textLayer.stroke.enabled) "${textLayer.stroke.width.roundToInt()} dp" else "غیرفعال",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = textLayer.stroke.enabled,
                            onCheckedChange = { en -> onUpdateTextLayer { it.copy(stroke = it.stroke.copy(enabled = en)) } }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ضخامت:", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Slider(
                            value = textLayer.stroke.width,
                            onValueChange = { w -> onUpdateTextLayer { it.copy(stroke = it.stroke.copy(enabled = w > 0f, width = w)) } },
                            valueRange = 0f..25f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                        )
                    }
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PIXELLAB_SWATCHES) { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                            val isSel = textLayer.stroke.colorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (isSel) 2.5.dp else 1.dp, if (isSel) palette.primaryAccent else Color.Gray, CircleShape)
                                    .clickable {
                                        onUpdateTextLayer { it.copy(stroke = it.stroke.copy(enabled = true, colorHex = hex)) }
                                    }
                            )
                        }
                    }
                }

                TextToolSubEditor.ULTRA_FX -> {
                    PixelLabSubEditorHeader(
                        title = "ابزارهای فراتر از پیکسل‌لب (Double Stroke • Glitch • Glass)",
                        subtitle = if (textLayer.secondStrokeEnabled || textLayer.glitchOffsetPx > 0f) "فعال" else "آماده",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    BeyondPixelLabUltraTextPanel(
                        textLayer = textLayer,
                        onUpdateTextLayer = onUpdateTextLayer
                    )
                }

                TextToolSubEditor.SHADOW -> {
                    PixelLabSubEditorHeader(
                        title = "سایه متن (Shadow)",
                        subtitle = if (textLayer.shadow.enabled) "${textLayer.shadow.blurRadius.roundToInt()} dp" else "غیرفعال",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = textLayer.shadow.enabled,
                            onCheckedChange = { en -> onUpdateTextLayer { it.copy(shadow = it.shadow.copy(enabled = en)) } }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بلور:", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Slider(
                            value = textLayer.shadow.blurRadius,
                            onValueChange = { r -> onUpdateTextLayer { it.copy(shadow = it.shadow.copy(enabled = r > 0f, blurRadius = r)) } },
                            valueRange = 0f..35f,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                            colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                        )
                        Text("Glow", fontSize = 10.sp, color = palette.textSecondary)
                        Switch(
                            checked = textLayer.shadow.outerGlow,
                            onCheckedChange = { g -> onUpdateTextLayer { it.copy(shadow = it.shadow.copy(enabled = true, outerGlow = g)) } }
                        )
                    }
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PIXELLAB_SWATCHES) { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        onUpdateTextLayer { it.copy(shadow = it.shadow.copy(enabled = true, colorHex = hex)) }
                                    }
                            )
                        }
                    }
                }

                TextToolSubEditor.INNER_SHADOW -> {
                    PixelLabSubEditorHeader(
                        title = "سایه داخلی (Inner Shadow)",
                        subtitle = if (textLayer.innerShadow.enabled) "${textLayer.innerShadow.blurRadius.roundToInt()} dp" else "غیرفعال",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = textLayer.innerShadow.enabled,
                            onCheckedChange = { en -> onUpdateTextLayer { it.copy(innerShadow = it.innerShadow.copy(enabled = en)) } }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بلور داخلی:", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Slider(
                            value = textLayer.innerShadow.blurRadius,
                            onValueChange = { r -> onUpdateTextLayer { it.copy(innerShadow = it.innerShadow.copy(enabled = r > 0f, blurRadius = r)) } },
                            valueRange = 0f..25f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }
                }

                TextToolSubEditor.EMBOSS -> {
                    PixelLabSubEditorHeader(
                        title = "برجسته‌سازی نوری (Emboss)",
                        subtitle = if (textLayer.emboss.enabled) "زاویه نور: ${textLayer.emboss.lightAngle.roundToInt()}°" else "غیرفعال",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = textLayer.emboss.enabled,
                            onCheckedChange = { en -> onUpdateTextLayer { it.copy(emboss = it.emboss.copy(enabled = en)) } }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("زاویه نور:", fontSize = 11.sp, color = palette.textSecondary)
                        Slider(
                            value = textLayer.emboss.lightAngle,
                            onValueChange = { ang -> onUpdateTextLayer { it.copy(emboss = it.emboss.copy(enabled = true, lightAngle = ang)) } },
                            valueRange = 0f..360f,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        )
                        Text("شدت:", fontSize = 11.sp, color = palette.textSecondary)
                        Slider(
                            value = textLayer.emboss.intensity,
                            onValueChange = { v -> onUpdateTextLayer { it.copy(emboss = it.emboss.copy(enabled = true, intensity = v)) } },
                            valueRange = 0.1f..1f,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        )
                    }
                }

                TextToolSubEditor.BACKGROUND -> {
                    PixelLabSubEditorHeader(
                        title = "کادر پس‌زمینه متن (Background Pill)",
                        subtitle = if (textLayer.backgroundPillEnabled) "گردی: ${textLayer.backgroundPillRadius.roundToInt()}" else "غیرفعال",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = textLayer.backgroundPillEnabled,
                            onCheckedChange = { en -> onUpdateTextLayer { it.copy(backgroundPillEnabled = en) } }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("پدینگ افقی:", fontSize = 10.sp, color = palette.textSecondary)
                        Slider(
                            value = textLayer.backgroundPillPadding,
                            onValueChange = { p ->
                                onUpdateTextLayer {
                                    it.copy(
                                        backgroundPillEnabled = true,
                                        backgroundPillPadding = p,
                                        backgroundPillPaddingLeft = p,
                                        backgroundPillPaddingRight = p
                                    )
                                }
                            },
                            valueRange = 0f..60f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Text("گردی:", fontSize = 10.sp, color = palette.textSecondary)
                        Slider(
                            value = textLayer.backgroundPillRadius,
                            onValueChange = { r -> onUpdateTextLayer { it.copy(backgroundPillEnabled = true, backgroundPillRadius = r) } },
                            valueRange = 0f..60f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                    }
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PIXELLAB_SWATCHES) { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        onUpdateTextLayer { it.copy(backgroundPillEnabled = true, backgroundPillColorHex = hex) }
                                    }
                            )
                        }
                    }
                }

                TextToolSubEditor.PADDING -> {
                    PixelLabSubEditorHeader(
                        title = "حاشیه داخلی متن (Padding)",
                        subtitle = "چپ: ${textLayer.paddingLeft.roundToInt()} | راست: ${textLayer.paddingRight.roundToInt()}",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("پدینگ چپ:", fontSize = 11.sp, color = palette.textSecondary)
                        Slider(
                            value = textLayer.paddingLeft,
                            onValueChange = { pl -> onUpdateTextLayer { it.copy(paddingLeft = pl) } },
                            valueRange = 0f..60f,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        )
                        Text("پدینگ راست:", fontSize = 11.sp, color = palette.textSecondary)
                        Slider(
                            value = textLayer.paddingRight,
                            onValueChange = { pr -> onUpdateTextLayer { it.copy(paddingRight = pr) } },
                            valueRange = 0f..60f,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        )
                    }
                }

                TextToolSubEditor.SPACING -> {
                    PixelLabSubEditorHeader(
                        title = "فاصله حروف (Spacing)",
                        subtitle = "${textLayer.letterSpacing.roundToInt()} px",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Slider(
                        value = textLayer.letterSpacing,
                        onValueChange = { sp -> onUpdateTextLayer { it.copy(letterSpacing = sp) } },
                        valueRange = -5f..30f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                    )
                }

                TextToolSubEditor.LINE_SPACING -> {
                    PixelLabSubEditorHeader(
                        title = "فاصله خطوط (Line Spacing)",
                        subtitle = "${(textLayer.lineSpacing * 100).roundToInt()}% (${String.format(java.util.Locale.US, "%.2f", textLayer.lineSpacing)}x)",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val nv = (textLayer.lineSpacing - 0.05f).coerceAtLeast(0.5f)
                                onUpdateTextLayer { it.copy(lineSpacing = nv) }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "کاهش فاصله خطوط", tint = Color.White)
                        }
                        Slider(
                            value = textLayer.lineSpacing,
                            onValueChange = { lm -> onUpdateTextLayer { it.copy(lineSpacing = lm) } },
                            valueRange = 0.5f..2.5f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                        )
                        IconButton(
                            onClick = {
                                val nv = (textLayer.lineSpacing + 0.05f).coerceAtMost(2.5f)
                                onUpdateTextLayer { it.copy(lineSpacing = nv) }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "افزایش فاصله خطوط", tint = Color.White)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            0.8f to "۸۰٪",
                            1.0f to "۱۰۰٪ (عادی)",
                            1.25f to "۱۲۵٪",
                            1.5f to "۱۵۰٪",
                            1.8f to "۱۸۰٪"
                        ).forEach { (presetVal, label) ->
                            val isSel = kotlin.math.abs(textLayer.lineSpacing - presetVal) < 0.04f
                            OutlinedButton(
                                onClick = { onUpdateTextLayer { it.copy(lineSpacing = presetVal) } },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                border = BorderStroke(1.dp, if (isSel) palette.primaryAccent else Color(0xFF334155)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSel) palette.primaryAccent.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Text(label, fontSize = 10.sp, color = if (isSel) palette.primaryAccent else Color(0xFF94A3B8))
                            }
                        }
                    }
                }

                TextToolSubEditor.OPACITY -> {
                    PixelLabSubEditorHeader(
                        title = "شفافیت (Opacity)",
                        subtitle = "${(textLayer.opacity * 100).roundToInt()}%",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Slider(
                        value = textLayer.opacity,
                        onValueChange = { a -> onUpdateTextLayer { it.copy(opacity = a) } },
                        valueRange = 0.05f..1f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                    )
                }

                TextToolSubEditor.MASK -> {
                    PixelLabSubEditorHeader(
                        title = "ماسک بزیه و برش داخل/خارج (Bezier Mask In/Out)",
                        subtitle = textLayer.maskMode.labelFa,
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    BezierMaskInspectorPanel(
                        maskMode = textLayer.maskMode,
                        maskInvert = textLayer.maskInvert,
                        maskControlX = textLayer.maskControlX,
                        maskControlY = textLayer.maskControlY,
                        maskAngle = textLayer.maskAngle,
                        onUpdateMask = { mode, inv, cx, cy, ang ->
                            onUpdateTextLayer {
                                it.copy(
                                    maskMode = mode,
                                    maskInvert = inv,
                                    maskControlX = cx,
                                    maskControlY = cy,
                                    maskAngle = ang
                                )
                            }
                        }
                    )
                }

                TextToolSubEditor.PERSPECTIVE -> {
                    PixelLabSubEditorHeader(
                        title = "پرسپکتیو چهارنقطه‌ای آزاد (4-Point Perspective Warp)",
                        subtitle = if (textLayer.perspectiveQuad.enabled) "۴ نقطه فعال" else "آماده",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Perspective4PointWarpPanel(
                        quad = textLayer.perspectiveQuad,
                        skewX = textLayer.skewX,
                        skewY = textLayer.skewY,
                        onUpdateQuad = { q -> onUpdateTextLayer { it.copy(perspectiveQuad = q) } },
                        onUpdateSkew = { sx, sy -> onUpdateTextLayer { it.copy(skewX = sx, skewY = sy) } }
                    )
                }

                TextToolSubEditor.FONT -> {
                    PixelLabSubEditorHeader(
                        title = "فونت و فونت‌های من (Font / My Fonts)",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Button(
                                onClick = onImportCustomFont,
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فونت من (.ttf)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        items(customUserFonts) { uf ->
                            val isSel = textLayer.customFontPath == uf.filePath
                            Card(
                                modifier = Modifier.clickable {
                                    onUpdateTextLayer { it.copy(customFontPath = uf.filePath) }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) palette.secondaryAccent.copy(alpha = 0.25f) else palette.surfaceVariant
                                ),
                                border = if (isSel) BorderStroke(1.5.dp, palette.secondaryAccent) else null
                            ) {
                                Text(
                                    text = "★ ${uf.displayName}",
                                    color = if (isSel) palette.secondaryAccent else palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        items(StudioFont.entries) { f ->
                            val isSel = textLayer.fontId == f.id && textLayer.customFontPath.isEmpty()
                            Card(
                                modifier = Modifier.clickable {
                                    onUpdateTextLayer { it.copy(fontId = f.id, customFontPath = "") }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) palette.primaryAccent.copy(alpha = 0.25f) else palette.surfaceVariant
                                ),
                                border = if (isSel) BorderStroke(1.5.dp, palette.primaryAccent) else null
                            ) {
                                Text(
                                    text = f.displayName,
                                    fontFamily = FontFamily(Font(f.fontRes)),
                                    color = if (isSel) palette.primaryAccent else palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                TextToolSubEditor.STYLE -> {
                    PixelLabSubEditorHeader(
                        title = "استایل فونت (B Style)",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { onUpdateTextLayer { it.copy(isBold = !it.isBold) } }) {
                            Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = if (textLayer.isBold) palette.primaryAccent else palette.textSecondary)
                        }
                        IconButton(onClick = { onUpdateTextLayer { it.copy(isItalic = !it.isItalic) } }) {
                            Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = if (textLayer.isItalic) palette.primaryAccent else palette.textSecondary)
                        }
                        IconButton(onClick = { onUpdateTextLayer { it.copy(isUnderline = !it.isUnderline) } }) {
                            Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", tint = if (textLayer.isUnderline) palette.primaryAccent else palette.textSecondary)
                        }
                    }
                }

                TextToolSubEditor.ALIGN -> {
                    PixelLabSubEditorHeader(
                        title = "ترازبندی متن و پاراگراف (Align & Justify)",
                        subtitle = textLayer.textAlignMode.labelFa,
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val alignOptions = listOf(
                            Triple(TextAlignMode.RIGHT, Icons.AutoMirrored.Filled.FormatAlignRight, "align_right_btn"),
                            Triple(TextAlignMode.CENTER, Icons.Default.FormatAlignCenter, "align_center_btn"),
                            Triple(TextAlignMode.LEFT, Icons.AutoMirrored.Filled.FormatAlignLeft, "align_left_btn"),
                            Triple(TextAlignMode.JUSTIFY, Icons.Default.FormatAlignJustify, "align_justify_btn")
                        )
                        alignOptions.forEach { (mode, iconVec, tag) ->
                            val isSelected = textLayer.textAlignMode == mode
                            Surface(
                                color = if (isSelected) palette.primaryAccent.copy(alpha = 0.22f) else palette.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) palette.primaryAccent else palette.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateTextLayer {
                                            it.copy(
                                                textAlignMode = mode,
                                                textAlignCenter = (mode == TextAlignMode.CENTER)
                                            )
                                        }
                                    }
                                    .testTag(tag)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = iconVec,
                                        contentDescription = mode.labelFa,
                                        tint = if (isSelected) palette.primaryAccent else palette.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = mode.labelFa,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) palette.primaryAccent else palette.textPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    PixelLabSubEditorHeader(
                        title = activeSubEditor?.name ?: "",
                        onApply = { activeSubEditor = null },
                        onDiscard = { activeSubEditor = null }
                    )
                }
            }
        }
        return
    }

    // AUTHENTIC PIXELLAB SCROLLABLE RIBBON / 2-ROW PALETTE FOR TEXT
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Column 0: Styles (Browse / Save) & Ultra FX (Double Stroke / Glitch / Glass)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(
                label = "Styles",
                icon = Icons.Default.Star,
                badgeText = "MY",
                testTag = "tool_text_styles",
                onClick = { activeSubEditor = TextToolSubEditor.STYLES }
            )
            PixelLabToolItem(
                label = "Ultra FX",
                icon = Icons.Default.AutoAwesome,
                badgeText = "PRO",
                isActive = textLayer.secondStrokeEnabled || textLayer.glitchOffsetPx > 0f || textLayer.backgroundPillGlassBlur,
                testTag = "tool_text_ultra_fx",
                onClick = { activeSubEditor = TextToolSubEditor.ULTRA_FX }
            )
        }

        // Column 1: Edit & Delete
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "Edit", icon = Icons.Default.Edit, testTag = "tool_text_edit", onClick = onOpenEditTextDialog)
            PixelLabToolItem(label = "Delete", icon = Icons.Default.Delete, testTag = "tool_text_delete", onClick = onDeleteLayer)
        }

        // Column 2: Copy & To Front
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "Copy", icon = Icons.Default.ContentCopy, onClick = onDuplicateLayer)
            PixelLabToolItem(label = "To Front", icon = Icons.Default.VerticalAlignTop, onClick = onBringToFront)
        }

        // Column 3: To Back & Position
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "To Back", icon = Icons.Default.VerticalAlignBottom, onClick = onSendToBack)
            PixelLabToolItem(label = "Position", icon = Icons.Default.OpenWith, onClick = { activeSubEditor = TextToolSubEditor.POSITION })
        }

        // Column 4: Relative 6-Grid & Size
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(
                label = "Relative",
                icon = Icons.Default.ZoomOutMap,
                testTag = "tool_text_relative_pos",
                onClick = { activeSubEditor = TextToolSubEditor.RELATIVE_POSITION }
            )
            PixelLabToolItem(label = "Size", icon = Icons.Default.FormatSize, onClick = { activeSubEditor = TextToolSubEditor.SIZE })
        }

        // Column 5: Padding & Color (Multi-stop + Eyedropper + Substring)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "Padding", icon = Icons.Default.Straighten, onClick = { activeSubEditor = TextToolSubEditor.PADDING })
            PixelLabToolItem(
                label = "Color+",
                icon = Icons.Default.Palette,
                isActive = textLayer.colorSpans.isNotEmpty() || textLayer.fontSpans.isNotEmpty() || textLayer.useGradientFill,
                testTag = "tool_text_color",
                onClick = { activeSubEditor = TextToolSubEditor.COLOR }
            )
        }

        // Column 6: Texture & Opacity
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "Texture", icon = Icons.Default.Texture, isActive = textLayer.texturePreset != TexturePreset.NONE || textLayer.customTextureBitmap != null, onClick = { activeSubEditor = TextToolSubEditor.TEXTURE })
            PixelLabToolItem(label = "Opacity", icon = Icons.Default.Opacity, onClick = { activeSubEditor = TextToolSubEditor.OPACITY })
        }

        // Column 7: Rotate & Bezier Mask
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "Rotate", icon = Icons.Default.RotateRight, onClick = { activeSubEditor = TextToolSubEditor.ROTATE })
            PixelLabToolItem(
                label = "Mask",
                icon = Icons.Default.Gesture,
                isActive = textLayer.maskMode != MaskPresetMode.NONE,
                testTag = "tool_text_mask",
                onClick = { activeSubEditor = TextToolSubEditor.MASK }
            )
        }

        // Column 8: Font & B Style
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "Font", icon = Icons.Default.FontDownload, isActive = textLayer.customFontPath.isNotEmpty(), onClick = { activeSubEditor = TextToolSubEditor.FONT })
            PixelLabToolItem(label = "B Style", icon = Icons.Default.FormatBold, isActive = textLayer.isBold || textLayer.isItalic || textLayer.isUnderline, onClick = { activeSubEditor = TextToolSubEditor.STYLE })
        }

        // Column 9: Curve & Background
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "Curve", icon = Icons.Default.Tonality, isActive = textLayer.curveAngle != 0f, onClick = { activeSubEditor = TextToolSubEditor.CURVE })
            PixelLabToolItem(label = "Background", icon = Icons.Default.FormatPaint, isActive = textLayer.backgroundPillEnabled, onClick = { activeSubEditor = TextToolSubEditor.BACKGROUND })
        }

        // Column 10: Align & Spacing
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "Align", icon = Icons.Default.FormatAlignCenter, onClick = { activeSubEditor = TextToolSubEditor.ALIGN })
            PixelLabToolItem(label = "Spacing", icon = Icons.Default.Straighten, onClick = { activeSubEditor = TextToolSubEditor.SPACING })
        }

        // Column 11: Line Space & Stroke
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "Line Space", icon = Icons.Default.FormatLineSpacing, onClick = { activeSubEditor = TextToolSubEditor.LINE_SPACING })
            PixelLabToolItem(label = "Stroke", icon = Icons.Default.LineWeight, isActive = textLayer.stroke.enabled, onClick = { activeSubEditor = TextToolSubEditor.STROKE })
        }

        // Column 12: Shadow & Inner Shadow
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "Shadow", icon = Icons.Default.FilterDrama, isActive = textLayer.shadow.enabled, onClick = { activeSubEditor = TextToolSubEditor.SHADOW })
            PixelLabToolItem(label = "Inner Shadow", icon = Icons.Default.Tonality, isActive = textLayer.innerShadow.enabled, onClick = { activeSubEditor = TextToolSubEditor.INNER_SHADOW })
        }

        // Column 13: Emboss & 4-Point Perspective
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "Emboss", icon = Icons.Default.LightMode, isActive = textLayer.emboss.enabled, onClick = { activeSubEditor = TextToolSubEditor.EMBOSS })
            PixelLabToolItem(
                label = "Perspective",
                icon = Icons.Default.Crop,
                isActive = textLayer.perspectiveQuad.enabled || textLayer.skewX != 0f || textLayer.skewY != 0f,
                testTag = "tool_text_perspective",
                onClick = { activeSubEditor = TextToolSubEditor.PERSPECTIVE }
            )
        }

        // Column 14: 3D Rotate & 3D Text + Lighting
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "3D Rotate", icon = Icons.Default.Rotate90DegreesCcw, isActive = textLayer.rotationX != 0f || textLayer.rotationY != 0f, onClick = { activeSubEditor = TextToolSubEditor.ROTATE_3D })
            PixelLabToolItem(
                label = "3D Text",
                icon = Icons.Default.ViewInAr,
                isActive = textLayer.is3dEnabled,
                testTag = "tool_text_3d",
                onClick = { activeSubEditor = TextToolSubEditor.TEXT_3D }
            )
        }

        // Column 15: 3D Shadow & Reflection
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PixelLabToolItem(label = "3D Shadow", icon = Icons.Default.ViewCarousel, isActive = textLayer.shadow3dEnabled, onClick = { activeSubEditor = TextToolSubEditor.SHADOW_3D })
            PixelLabToolItem(label = "Reflection", icon = Icons.Default.Flip, isActive = textLayer.reflectionEnabled, onClick = { activeSubEditor = TextToolSubEditor.REFLECTION })
        }
    }
}

// ============================================================================
// TAB 3: PIXELLAB HEXAGON OBJECTS, IMAGE/SHAPE PRO EDITORS & DRAWING TOOLS
// ============================================================================
private enum class ObjectToolSubEditor {
    SIZE, POSITION, RELATIVE_POSITION, COLOR_TINT, STROKE, SHADOW, ERASE_COLOR,
    COLOR_FILTERS, ROTATE_3D, SHAPE_3D, CROP_RADIUS, OPACITY, MASK, PERSPECTIVE, BEZIER_EDITOR, MANUAL_ERASER
}

@Composable
fun ObjectsAndDrawInspector(
    selectedLayer: StudioLayer?,
    isDrawingMode: Boolean,
    isEraserMode: Boolean = false,
    eraserBrushSize: Float = 28f,
    penColorHex: String,
    penWidth: Float,
    penNeon: Boolean,
    penDashed: Boolean,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onAddShape: (ShapeKind) -> Unit,
    onAddBuiltinSticker: (BuiltinStickerItem) -> Unit,
    onPickGalleryImageLayer: () -> Unit,
    onSetDrawingMode: (Boolean) -> Unit,
    onSetEraserMode: (Boolean) -> Unit = {},
    onSetEraserBrushSize: (Float) -> Unit = {},
    onClearEraserStrokes: () -> Unit = {},
    onApplyImageCrop: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onResetImageCrop: () -> Unit = {},
    onToggleImageFlip: (Boolean, Boolean) -> Unit = { _, _ -> },
    onUpdatePenOptions: (String, Float, Boolean, Boolean) -> Unit,
    onUpdateShapeLayer: ((ShapeLayer) -> ShapeLayer) -> Unit,
    onUpdateImageLayer: ((ImageLayer) -> ImageLayer) -> Unit,
    onDuplicateLayer: () -> Unit = {},
    onDeleteLayer: () -> Unit = {},
    onBringToFront: () -> Unit = {},
    onSendToBack: () -> Unit = {},
    onCenterLayer: (Boolean, Boolean) -> Unit = { _, _ -> },
    onNudgeLayer: (Float, Float) -> Unit = { _, _ -> }
) {
    val palette = LocalStudioPalette.current
    var showStickersSheet by remember { mutableStateOf(false) }
    var forceCreationToolbar by remember(selectedLayer?.id) { mutableStateOf(false) }
    var activeObjectEditor by remember(selectedLayer?.id) { mutableStateOf<ObjectToolSubEditor?>(null) }

    // 1. Pen / Bezier Freehand Drawing & Interactive Eraser Toolbar
    if (isDrawingMode) {
        Column(modifier = Modifier.fillMaxSize()) {
            PixelLabSubEditorHeader(
                title = if (isEraserMode) "پاک‌کن تعاملی نقاشی (Touch Eraser)" else "قلم طراحی آزاد و نئون (Draw)",
                subtitle = if (isEraserMode) "قطر پاک‌کن: ${eraserBrushSize.roundToInt()} px" else "قطر قلم: ${penWidth.roundToInt()} px",
                onApply = {
                    onSetEraserMode(false)
                    onSetDrawingMode(false)
                },
                onDiscard = {
                    onSetEraserMode(false)
                    onSetDrawingMode(false)
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle Pen vs Interactive Eraser
                Surface(
                    color = if (!isEraserMode) palette.primaryAccent else palette.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { onSetEraserMode(false) }
                        .testTag("draw_mode_pen_chip")
                ) {
                    Text(
                        text = "✏️ قلم",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isEraserMode) Color.Black else palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    color = if (isEraserMode) palette.primaryAccent else palette.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { onSetEraserMode(true) }
                        .testTag("draw_mode_eraser_chip")
                ) {
                    Text(
                        text = "🧹 پاک‌کن لمسی",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEraserMode) Color.Black else palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Live circular brush/eraser preview indicator
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.surfaceVariant)
                        .border(1.dp, palette.border, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val previewDiameterDp = ((if (isEraserMode) eraserBrushSize else penWidth).coerceIn(4f, 90f) * 0.36f).coerceIn(4f, 32f).dp
                    Box(
                        modifier = Modifier
                            .size(previewDiameterDp)
                            .clip(CircleShape)
                            .background(
                                if (isEraserMode) Color.White.copy(alpha = 0.25f)
                                else try { Color(android.graphics.Color.parseColor(penColorHex)) } catch (_: Exception) { palette.primaryAccent }
                            )
                            .border(
                                1.5.dp,
                                if (isEraserMode) palette.primaryAccent else Color.White,
                                CircleShape
                            )
                    )
                }

                Slider(
                    value = if (isEraserMode) eraserBrushSize else penWidth,
                    onValueChange = { w ->
                        if (isEraserMode) onSetEraserBrushSize(w)
                        else onUpdatePenOptions(penColorHex, w, penNeon, penDashed)
                    },
                    valueRange = if (isEraserMode) 4f..90f else 2f..36f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                )

                if (!isEraserMode) {
                    Text("نئون", fontSize = 10.sp, color = palette.textSecondary)
                    Switch(
                        checked = penNeon,
                        onCheckedChange = { n -> onUpdatePenOptions(penColorHex, penWidth, n, penDashed) }
                    )
                }
            }
            if (!isEraserMode) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PIXELLAB_SWATCHES) { hex ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.White }
                        val isSel = penColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(if (isSel) 2.5.dp else 1.dp, if (isSel) palette.primaryAccent else Color.Gray, CircleShape)
                                .clickable { onUpdatePenOptions(hex, penWidth, penNeon, penDashed) }
                        )
                    }
                }
            } else {
                Text(
                    text = "انگشت خود را مستقیم روی خطوط نقاشی در بوم بکشید تا با پیش‌نمایش دایره‌ای پاک شوند (پشتیبانی کامل از Undo / Redo).",
                    fontSize = 10.sp,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                )
            }
        }
        return
    }

    // 2. Stickers Picker Sheet
    if (showStickersSheet) {
        Column(modifier = Modifier.fillMaxSize()) {
            PixelLabSubEditorHeader(
                title = "استیکرها (Stickers)",
                onApply = { showStickersSheet = false },
                onDiscard = { showStickersSheet = false }
            )
            LazyRow(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BUILTIN_PIXELLAB_STICKERS) { sticker ->
                    Card(
                        modifier = Modifier.clickable {
                            onAddBuiltinSticker(sticker)
                            showStickersSheet = false
                            forceCreationToolbar = false
                        },
                        colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${sticker.symbolText} ${sticker.labelFa}",
                            color = palette.textPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
        return
    }

    // 3. Sub-editor for selected ImageLayer or ShapeLayer
    if (activeObjectEditor != null && (selectedLayer is ImageLayer || selectedLayer is ShapeLayer)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (activeObjectEditor) {
                ObjectToolSubEditor.BEZIER_EDITOR -> if (selectedLayer is ShapeLayer) {
                    PixelLabSubEditorHeader(
                        title = "ابزار منحنی بزیه وکتور (Bezier Vector Path)",
                        subtitle = "${selectedLayer.bezierNodes.size} گره کنترلی",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    BezierVectorPathEditorPanel(
                        shapeLayer = selectedLayer,
                        onUpdateShapeLayer = onUpdateShapeLayer
                    )
                }

                ObjectToolSubEditor.RELATIVE_POSITION -> {
                    PixelLabSubEditorHeader(
                        title = "موقعیت نسبی ۹ جهته (Relative Position 6-Grid)",
                        subtitle = "تراز مغناطیسی شکل و تصویر",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    RelativePosition6GridPanel(
                        onSnapPosition = { nx, ny ->
                            if (selectedLayer is ImageLayer) {
                                onUpdateImageLayer { it.copy(x = nx ?: it.x, y = ny ?: it.y) }
                            } else if (selectedLayer is ShapeLayer) {
                                onUpdateShapeLayer { it.copy(x = nx ?: it.x, y = ny ?: it.y) }
                            }
                        },
                        onCenterLayer = onCenterLayer
                    )
                }

                ObjectToolSubEditor.PERSPECTIVE -> {
                    val curQuad = if (selectedLayer is ImageLayer) selectedLayer.perspectiveQuad else (selectedLayer as ShapeLayer).perspectiveQuad
                    PixelLabSubEditorHeader(
                        title = "پرسپکتیو چهارنقطه‌ای آزاد (4-Point Perspective)",
                        subtitle = if (curQuad.enabled) "۴ نقطه فعال" else "آماده",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    Perspective4PointWarpPanel(
                        quad = curQuad,
                        onUpdateQuad = { q ->
                            if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(perspectiveQuad = q) }
                            else if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(perspectiveQuad = q) }
                        }
                    )
                }

                ObjectToolSubEditor.MASK -> {
                    val curMode = if (selectedLayer is ImageLayer) selectedLayer.maskMode else (selectedLayer as ShapeLayer).maskMode
                    val curInv = if (selectedLayer is ImageLayer) selectedLayer.maskInvert else (selectedLayer as ShapeLayer).maskInvert
                    val curCx = if (selectedLayer is ImageLayer) selectedLayer.maskControlX else (selectedLayer as ShapeLayer).maskControlX
                    val curCy = if (selectedLayer is ImageLayer) selectedLayer.maskControlY else (selectedLayer as ShapeLayer).maskControlY
                    val curAng = if (selectedLayer is ImageLayer) selectedLayer.maskAngle else (selectedLayer as ShapeLayer).maskAngle
                    PixelLabSubEditorHeader(
                        title = "ماسک بزیه داخل/خارج (Bezier Mask In/Out)",
                        subtitle = curMode.labelFa,
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    BezierMaskInspectorPanel(
                        maskMode = curMode,
                        maskInvert = curInv,
                        maskControlX = curCx,
                        maskControlY = curCy,
                        maskAngle = curAng,
                        onUpdateMask = { mode, inv, cx, cy, ang ->
                            if (selectedLayer is ImageLayer) {
                                onUpdateImageLayer {
                                    it.copy(
                                        maskMode = mode,
                                        maskInvert = inv,
                                        maskControlX = cx,
                                        maskControlY = cy,
                                        maskAngle = ang
                                    )
                                }
                            } else if (selectedLayer is ShapeLayer) {
                                onUpdateShapeLayer {
                                    it.copy(
                                        maskMode = mode,
                                        maskInvert = inv,
                                        maskControlX = cx,
                                        maskControlY = cy,
                                        maskAngle = ang
                                    )
                                }
                            }
                        }
                    )
                }

                ObjectToolSubEditor.MANUAL_ERASER -> if (selectedLayer is ImageLayer) {
                    val img = selectedLayer
                    PixelLabSubEditorHeader(
                        title = "پاک‌کن تعاملی تصویر روی بوم (Interactive Eraser)",
                        subtitle = "اندازه قلم: ${eraserBrushSize.roundToInt()} px | ${img.eraserStrokes.size} حرکت",
                        onApply = {
                            onSetEraserMode(false)
                            activeObjectEditor = null
                        },
                        onDiscard = {
                            onSetEraserMode(false)
                            activeObjectEditor = null
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular live brush size preview indicator
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.surfaceVariant)
                                .border(1.dp, palette.primaryAccent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .testTag("eraser_size_preview_circle"),
                            contentAlignment = Alignment.Center
                        ) {
                            val circleDp = (eraserBrushSize.coerceIn(4f, 90f) * 0.38f).coerceIn(5f, 36f).dp
                            Box(
                                modifier = Modifier
                                    .size(circleDp)
                                    .clip(CircleShape)
                                    .background(palette.primaryAccent.copy(alpha = 0.25f))
                                    .border(1.5.dp, palette.primaryAccent, CircleShape)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "اندازه قلم پاک‌کن: ${eraserBrushSize.roundToInt()} px",
                                fontSize = 10.sp,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = eraserBrushSize,
                                onValueChange = { onSetEraserBrushSize(it) },
                                valueRange = 4f..90f,
                                modifier = Modifier.testTag("eraser_brush_size_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.primaryAccent,
                                    activeTrackColor = palette.primaryAccent
                                )
                            )
                        }

                        Surface(
                            color = if (isEraserMode) palette.primaryAccent else palette.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, palette.primaryAccent),
                            modifier = Modifier
                                .clickable { onSetEraserMode(!isEraserMode) }
                                .testTag("toggle_touch_eraser_button")
                        ) {
                            Text(
                                text = if (isEraserMode) "🧹 لمس روی بوم: فعال" else "فعال‌سازی لمس پاک‌کن",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEraserMode) Color.Black else palette.primaryAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onUndo,
                            enabled = canUndo,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("واگرد (Undo)", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = onRedo,
                            enabled = canRedo,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("انجام مجدد (Redo)", fontSize = 10.sp)
                        }
                        if (img.eraserStrokes.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onClearEraserStrokes,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("پاکسازی تمام خطوط پاک‌کن", fontSize = 10.sp, color = palette.danger)
                            }
                        }
                    }
                }

                ObjectToolSubEditor.ERASE_COLOR -> if (selectedLayer is ImageLayer) {
                    val img = selectedLayer
                    PixelLabSubEditorHeader(
                        title = "حذف رنگ پس‌زمینه (Erase Color / Chroma Key)",
                        subtitle = if (img.eraseColorEnabled) "تلورانس: ${(img.eraseTolerance * 100).roundToInt()}%" else "غیرفعال",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = img.eraseColorEnabled,
                            onCheckedChange = { en -> onUpdateImageLayer { it.copy(eraseColorEnabled = en) } }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تلورانس:", fontSize = 10.sp, color = palette.textSecondary)
                        Slider(
                            value = img.eraseTolerance,
                            onValueChange = { t -> onUpdateImageLayer { it.copy(eraseColorEnabled = true, eraseTolerance = t) } },
                            valueRange = 0.02f..0.75f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                    }
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf("#FFFFFF", "#000000", "#00FF00", "#0000FF", "#FF0000", "#F2C100")) { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.White }
                            val isSel = img.eraseTargetColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (isSel) 2.5.dp else 1.dp, if (isSel) palette.primaryAccent else Color.Gray, CircleShape)
                                    .clickable {
                                        onUpdateImageLayer { it.copy(eraseColorEnabled = true, eraseTargetColorHex = hex) }
                                    }
                            )
                        }
                    }
                }

                ObjectToolSubEditor.COLOR_TINT -> {
                    PixelLabSubEditorHeader(
                        title = "رنگ روکش / پرکننده (Color / Tint)",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    if (selectedLayer is ImageLayer) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("فعال‌سازی روکش رنگی (Color Overlay):", fontSize = 11.sp, color = palette.textPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = selectedLayer.colorOverlayEnabled,
                                onCheckedChange = { en -> onUpdateImageLayer { it.copy(colorOverlayEnabled = en) } }
                            )
                        }
                    }
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PIXELLAB_SWATCHES) { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.White }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        if (selectedLayer is ImageLayer) {
                                            onUpdateImageLayer { it.copy(colorOverlayEnabled = true, colorOverlayHex = hex) }
                                        } else if (selectedLayer is ShapeLayer) {
                                            onUpdateShapeLayer { it.copy(fillColorHex = hex) }
                                        }
                                    }
                            )
                        }
                    }
                }

                ObjectToolSubEditor.STROKE -> {
                    val curStroke = if (selectedLayer is ImageLayer) selectedLayer.stroke else (selectedLayer as ShapeLayer).stroke
                    PixelLabSubEditorHeader(
                        title = "خط دور برش / شکل (Stroke)",
                        subtitle = if (curStroke.enabled) "${curStroke.width.roundToInt()} dp" else "غیرفعال",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = curStroke.enabled,
                            onCheckedChange = { en ->
                                if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(stroke = it.stroke.copy(enabled = en)) }
                                else if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(stroke = it.stroke.copy(enabled = en)) }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ضخامت:", fontSize = 11.sp, color = palette.textSecondary)
                        Slider(
                            value = curStroke.width,
                            onValueChange = { w ->
                                if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(stroke = it.stroke.copy(enabled = w > 0f, width = w)) }
                                else if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(stroke = it.stroke.copy(enabled = w > 0f, width = w)) }
                            },
                            valueRange = 0f..25f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PIXELLAB_SWATCHES) { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.White }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(stroke = it.stroke.copy(enabled = true, colorHex = hex)) }
                                        else if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(stroke = it.stroke.copy(enabled = true, colorHex = hex)) }
                                    }
                            )
                        }
                    }
                }

                ObjectToolSubEditor.SHADOW -> {
                    val curShadow = if (selectedLayer is ImageLayer) selectedLayer.shadow else (selectedLayer as ShapeLayer).shadow
                    PixelLabSubEditorHeader(
                        title = "سایه (Shadow)",
                        subtitle = if (curShadow.enabled) "${curShadow.blurRadius.roundToInt()} dp" else "غیرفعال",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = curShadow.enabled,
                            onCheckedChange = { en ->
                                if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(shadow = it.shadow.copy(enabled = en)) }
                                else if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(shadow = it.shadow.copy(enabled = en)) }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بلور:", fontSize = 11.sp, color = palette.textSecondary)
                        Slider(
                            value = curShadow.blurRadius,
                            onValueChange = { r ->
                                if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(shadow = it.shadow.copy(enabled = r > 0f, blurRadius = r)) }
                                else if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(shadow = it.shadow.copy(enabled = r > 0f, blurRadius = r)) }
                            },
                            valueRange = 0f..35f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PIXELLAB_SWATCHES) { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(shadow = it.shadow.copy(enabled = true, colorHex = hex)) }
                                        else if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(shadow = it.shadow.copy(enabled = true, colorHex = hex)) }
                                    }
                            )
                        }
                    }
                }

                ObjectToolSubEditor.COLOR_FILTERS -> if (selectedLayer is ImageLayer) {
                    val img = selectedLayer
                    PixelLabSubEditorHeader(
                        title = "فیلترهای رنگی تصویر (Color Filters)",
                        subtitle = "Hue / Sat / Bright / Contrast",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hue:", fontSize = 10.sp, color = palette.textSecondary)
                        Slider(
                            value = img.hue,
                            onValueChange = { h -> onUpdateImageLayer { it.copy(hue = h) } },
                            valueRange = -180f..180f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Text("Sat:", fontSize = 10.sp, color = palette.textSecondary)
                        Slider(
                            value = img.saturation,
                            onValueChange = { s -> onUpdateImageLayer { it.copy(saturation = s) } },
                            valueRange = 0f..2f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bright:", fontSize = 10.sp, color = palette.textSecondary)
                        Slider(
                            value = img.brightness,
                            onValueChange = { b -> onUpdateImageLayer { it.copy(brightness = b) } },
                            valueRange = -50f..50f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Text("Contrast:", fontSize = 10.sp, color = palette.textSecondary)
                        Slider(
                            value = img.contrast,
                            onValueChange = { c -> onUpdateImageLayer { it.copy(contrast = c) } },
                            valueRange = 0.5f..1.8f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                    }
                }

                ObjectToolSubEditor.ROTATE_3D -> {
                    val rx = if (selectedLayer is ImageLayer) selectedLayer.rotationX else (selectedLayer as ShapeLayer).rotationX
                    val ry = if (selectedLayer is ImageLayer) selectedLayer.rotationY else (selectedLayer as ShapeLayer).rotationY
                    PixelLabSubEditorHeader(
                        title = "چرخش سه‌بعدی (3D Rotate)",
                        subtitle = "X:${rx.roundToInt()}° Y:${ry.roundToInt()}°",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("X:", fontSize = 11.sp, color = palette.textSecondary)
                        Slider(
                            value = rx,
                            onValueChange = { v ->
                                if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(rotationX = v) }
                                else if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(rotationX = v) }
                            },
                            valueRange = -65f..65f,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        )
                        Text("Y:", fontSize = 11.sp, color = palette.textSecondary)
                        Slider(
                            value = ry,
                            onValueChange = { v ->
                                if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(rotationY = v) }
                                else if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(rotationY = v) }
                            },
                            valueRange = -65f..65f,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        )
                    }
                }

                ObjectToolSubEditor.SHAPE_3D -> {
                    val isEn = if (selectedLayer is ShapeLayer) selectedLayer.is3dExtruded else (selectedLayer as ImageLayer).is3dShapeEnabled
                    val curDepth = if (selectedLayer is ShapeLayer) selectedLayer.depth3d else (selectedLayer as ImageLayer).depth3d
                    val curAngle = if (selectedLayer is ShapeLayer) selectedLayer.obliqueAngle3d else (selectedLayer as ImageLayer).obliqueAngle3d
                    PixelLabSubEditorHeader(
                        title = "حجم سه‌بعدی شکل و استیکر (3D Shape Extrusion)",
                        subtitle = if (isEn) "عمق: $curDepth" else "غیرفعال",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isEn,
                            onCheckedChange = { en ->
                                if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(is3dExtruded = en) }
                                else if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(is3dShapeEnabled = en) }
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عمق:", fontSize = 10.sp, color = palette.textSecondary)
                        Slider(
                            value = curDepth.toFloat(),
                            onValueChange = { d ->
                                if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(is3dExtruded = true, depth3d = d.toInt()) }
                                else if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(is3dShapeEnabled = true, depth3d = d.toInt()) }
                            },
                            valueRange = 1f..50f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Text("زاویه:", fontSize = 10.sp, color = palette.textSecondary)
                        Slider(
                            value = curAngle,
                            onValueChange = { ang ->
                                if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(is3dExtruded = true, obliqueAngle3d = ang) }
                                else if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(is3dShapeEnabled = true, obliqueAngle3d = ang) }
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                    }
                }

                ObjectToolSubEditor.CROP_RADIUS -> {
                    if (selectedLayer is ImageLayer) {
                        val sourceBmp = selectedLayer.originalBitmap ?: selectedLayer.bitmap
                        var selectedPreset by remember(selectedLayer.id) { mutableStateOf(CropAspectPreset.FREE) }
                        var cropL by remember(selectedLayer.id) { mutableStateOf(0f) }
                        var cropT by remember(selectedLayer.id) { mutableStateOf(0f) }
                        var cropR by remember(selectedLayer.id) { mutableStateOf(1f) }
                        var cropB by remember(selectedLayer.id) { mutableStateOf(1f) }

                        fun applyPresetRatio(preset: CropAspectPreset) {
                            selectedPreset = preset
                            val rw = preset.ratioWidth
                            val rh = preset.ratioHeight
                            if (rw == null || rh == null || sourceBmp == null) {
                                cropL = 0f
                                cropT = 0f
                                cropR = 1f
                                cropB = 1f
                                return
                            }
                            val imgRatio = sourceBmp.width.toFloat().coerceAtLeast(1f) / sourceBmp.height.toFloat().coerceAtLeast(1f)
                            val targetRatio = rw / rh
                            if (targetRatio > imgRatio) {
                                // Full width, crop height centered
                                val normH = (imgRatio / targetRatio).coerceIn(0.1f, 1f)
                                cropL = 0f
                                cropR = 1f
                                cropT = (1f - normH) * 0.5f
                                cropB = cropT + normH
                            } else {
                                // Full height, crop width centered
                                val normW = (targetRatio / imgRatio).coerceIn(0.1f, 1f)
                                cropT = 0f
                                cropB = 1f
                                cropL = (1f - normW) * 0.5f
                                cropR = cropL + normW
                            }
                        }

                        PixelLabSubEditorHeader(
                            title = "برش مستطیلی تعاملی، قرینه‌سازی و ماسک (Crop & Flip)",
                            subtitle = selectedPreset.labelFa,
                            onApply = {
                                if (cropL > 0.005f || cropT > 0.005f || cropR < 0.995f || cropB < 0.995f) {
                                    onApplyImageCrop(cropL, cropT, cropR, cropB)
                                }
                                activeObjectEditor = null
                            },
                            onDiscard = { activeObjectEditor = null }
                        )

                        // Row 1: Aspect Ratio Presets (Free, 1:1, 4:5, 16:9, 9:16) + Flip H & Flip V
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(CropAspectPreset.entries) { preset ->
                                val isSel = selectedPreset == preset
                                Surface(
                                    color = if (isSel) palette.primaryAccent.copy(alpha = 0.25f) else palette.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSel) palette.primaryAccent else palette.border),
                                    modifier = Modifier
                                        .clickable { applyPresetRatio(preset) }
                                        .testTag("crop_preset_${preset.name}")
                                ) {
                                    Text(
                                        text = preset.labelFa,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) palette.primaryAccent else palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            item {
                                Surface(
                                    color = if (selectedLayer.flipHorizontal) palette.secondaryAccent.copy(alpha = 0.25f) else palette.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (selectedLayer.flipHorizontal) palette.secondaryAccent else palette.border),
                                    modifier = Modifier
                                        .clickable { onToggleImageFlip(true, false) }
                                        .testTag("crop_flip_horizontal_btn")
                                ) {
                                    Text(
                                        text = "↔️ قرینه افقی (Flip H)",
                                        fontSize = 10.sp,
                                        color = if (selectedLayer.flipHorizontal) palette.secondaryAccent else palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            item {
                                Surface(
                                    color = if (selectedLayer.flipVertical) palette.secondaryAccent.copy(alpha = 0.25f) else palette.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (selectedLayer.flipVertical) palette.secondaryAccent else palette.border),
                                    modifier = Modifier
                                        .clickable { onToggleImageFlip(false, true) }
                                        .testTag("crop_flip_vertical_btn")
                                ) {
                                    Text(
                                        text = "↕️ قرینه عمودی (Flip V)",
                                        fontSize = 10.sp,
                                        color = if (selectedLayer.flipVertical) palette.secondaryAccent else palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Row 2: Interactive Touch Crop Box + Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Touch-adjustable Rectangular Crop Frame
                            Box(
                                modifier = Modifier
                                    .width(126.dp)
                                    .height(82.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0B0F17))
                                    .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                                    .testTag("interactive_crop_box_canvas")
                                    .pointerInput(selectedPreset) {
                                        var activeDragCorner = 0 // 0=move all, 1=TL, 2=TR, 3=BL, 4=BR
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val nx = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                                val ny = (offset.y / size.height.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                                                val thresh = 0.22f
                                                activeDragCorner = when {
                                                    abs(nx - cropL) < thresh && abs(ny - cropT) < thresh -> 1
                                                    abs(nx - cropR) < thresh && abs(ny - cropT) < thresh -> 2
                                                    abs(nx - cropL) < thresh && abs(ny - cropB) < thresh -> 3
                                                    abs(nx - cropR) < thresh && abs(ny - cropB) < thresh -> 4
                                                    else -> 0
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val dx = dragAmount.x / size.width.toFloat().coerceAtLeast(1f)
                                                val dy = dragAmount.y / size.height.toFloat().coerceAtLeast(1f)
                                                val minSpan = 0.12f
                                                if (selectedPreset != CropAspectPreset.FREE && activeDragCorner != 0) {
                                                    // Keep fixed aspect ratio when resizing corners
                                                    val currentW = (cropR - cropL).coerceAtLeast(minSpan)
                                                    val currentH = (cropB - cropT).coerceAtLeast(minSpan)
                                                    val aspect = currentW / currentH
                                                    val deltaW = if (activeDragCorner == 1 || activeDragCorner == 3) -dx else dx
                                                    val newW = (currentW + deltaW).coerceIn(minSpan, 1f)
                                                    val newH = (newW / aspect).coerceIn(minSpan, 1f)
                                                    val adjW = newH * aspect
                                                    val cx = (cropL + cropR) * 0.5f
                                                    val cy = (cropT + cropB) * 0.5f
                                                    cropL = (cx - adjW * 0.5f).coerceIn(0f, 1f - adjW)
                                                    cropR = (cropL + adjW).coerceIn(minSpan, 1f)
                                                    cropT = (cy - newH * 0.5f).coerceIn(0f, 1f - newH)
                                                    cropB = (cropT + newH).coerceIn(minSpan, 1f)
                                                } else {
                                                    when (activeDragCorner) {
                                                        1 -> {
                                                            cropL = (cropL + dx).coerceIn(0f, cropR - minSpan)
                                                            cropT = (cropT + dy).coerceIn(0f, cropB - minSpan)
                                                        }
                                                        2 -> {
                                                            cropR = (cropR + dx).coerceIn(cropL + minSpan, 1f)
                                                            cropT = (cropT + dy).coerceIn(0f, cropB - minSpan)
                                                        }
                                                        3 -> {
                                                            cropL = (cropL + dx).coerceIn(0f, cropR - minSpan)
                                                            cropB = (cropB + dy).coerceIn(cropT + minSpan, 1f)
                                                        }
                                                        4 -> {
                                                            cropR = (cropR + dx).coerceIn(cropL + minSpan, 1f)
                                                            cropB = (cropB + dy).coerceIn(cropT + minSpan, 1f)
                                                        }
                                                        else -> {
                                                            val w = cropR - cropL
                                                            val h = cropB - cropT
                                                            cropL = (cropL + dx).coerceIn(0f, 1f - w)
                                                            cropR = cropL + w
                                                            cropT = (cropT + dy).coerceIn(0f, 1f - h)
                                                            cropB = cropT + h
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (sourceBmp != null) {
                                        drawImage(
                                            image = sourceBmp.asImageBitmap(),
                                            dstOffset = IntOffset.Zero,
                                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                            alpha = 0.55f
                                        )
                                    }
                                    val lx = cropL * size.width
                                    val ty = cropT * size.height
                                    val rx = cropR * size.width
                                    val by = cropB * size.height
                                    val cw = (rx - lx).coerceAtLeast(4f)
                                    val ch = (by - ty).coerceAtLeast(4f)

                                    // Highlighted crop window
                                    drawRect(
                                        color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                                        topLeft = Offset(lx, ty),
                                        size = Size(cw, ch)
                                    )
                                    drawRect(
                                        color = Color(0xFF00E5FF),
                                        topLeft = Offset(lx, ty),
                                        size = Size(cw, ch),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    // Rule of thirds lines inside crop rect
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.45f),
                                        start = Offset(lx + cw / 3f, ty),
                                        end = Offset(lx + cw / 3f, by),
                                        strokeWidth = 1f
                                    )
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.45f),
                                        start = Offset(lx + 2f * cw / 3f, ty),
                                        end = Offset(lx + 2f * cw / 3f, by),
                                        strokeWidth = 1f
                                    )
                                    // 4 Corner handles
                                    val handleR = 5.dp.toPx()
                                    listOf(
                                        Offset(lx, ty),
                                        Offset(rx, ty),
                                        Offset(lx, by),
                                        Offset(rx, by)
                                    ).forEach { pt ->
                                        drawCircle(color = Color(0xFF00E5FF), radius = handleR, center = pt)
                                        drawCircle(color = Color.Black, radius = handleR * 0.45f, center = pt)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            onApplyImageCrop(cropL, cropT, cropR, cropB)
                                            cropL = 0f
                                            cropT = 0f
                                            cropR = 1f
                                            cropB = 1f
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.primaryAccent,
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("apply_image_crop_button")
                                    ) {
                                        Text("✂️ اعمال برش با حفظ کیفیت", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (selectedLayer.originalBitmap != null) {
                                        OutlinedButton(
                                            onClick = {
                                                onResetImageCrop()
                                                cropL = 0f
                                                cropT = 0f
                                                cropR = 1f
                                                cropB = 1f
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("تصویر اصلی", fontSize = 10.sp)
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("دایره‌ای:", fontSize = 10.sp, color = palette.textSecondary)
                                    Switch(
                                        checked = selectedLayer.isCircularMask,
                                        onCheckedChange = { c -> onUpdateImageLayer { it.copy(isCircularMask = c) } }
                                    )
                                    Text("گردی:", fontSize = 10.sp, color = palette.textSecondary)
                                    Slider(
                                        value = selectedLayer.cornerRadius,
                                        onValueChange = { r -> onUpdateImageLayer { it.copy(cornerRadius = r) } },
                                        valueRange = 0f..80f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    } else if (selectedLayer is ShapeLayer) {
                        PixelLabSubEditorHeader(
                            title = "گردی گوشه شکل (Corner Radius)",
                            onApply = { activeObjectEditor = null },
                            onDiscard = { activeObjectEditor = null }
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("گردی گوشه شکل:", fontSize = 11.sp, color = palette.textSecondary)
                            Slider(
                                value = selectedLayer.cornerRadius,
                                onValueChange = { r -> onUpdateShapeLayer { it.copy(cornerRadius = r) } },
                                valueRange = 0f..80f,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                ObjectToolSubEditor.SIZE -> {
                    val curScale = selectedLayer.scaleX
                    PixelLabSubEditorHeader(
                        title = "اندازه و مقیاس (Relative Size)",
                        subtitle = "${(curScale * 100).roundToInt()}%",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    Slider(
                        value = curScale,
                        onValueChange = { sc ->
                            if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(scaleX = sc, scaleY = sc) }
                            else if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(scaleX = sc, scaleY = sc) }
                        },
                        valueRange = 0.15f..3.5f,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                ObjectToolSubEditor.OPACITY -> {
                    PixelLabSubEditorHeader(
                        title = "شفافیت (Opacity)",
                        subtitle = "${(selectedLayer.opacity * 100).roundToInt()}%",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                    Slider(
                        value = selectedLayer.opacity,
                        onValueChange = { op ->
                            if (selectedLayer is ImageLayer) onUpdateImageLayer { it.copy(opacity = op) }
                            else if (selectedLayer is ShapeLayer) onUpdateShapeLayer { it.copy(opacity = op) }
                        },
                        valueRange = 0.05f..1f,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                else -> {
                    PixelLabSubEditorHeader(
                        title = activeObjectEditor?.name ?: "",
                        onApply = { activeObjectEditor = null },
                        onDiscard = { activeObjectEditor = null }
                    )
                }
            }
        }
        return
    }

    // 4. Contextual PixelLab Pro 2-Row Grid when an ImageLayer or ShapeLayer is selected!
    if (!forceCreationToolbar && (selectedLayer is ImageLayer || selectedLayer is ShapeLayer)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Col 1: + Add New & Delete
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PixelLabToolItem(label = "+ جدید", icon = Icons.Default.Add, onClick = { forceCreationToolbar = true })
                PixelLabToolItem(label = "Delete", icon = Icons.Default.Delete, onClick = onDeleteLayer)
            }
            // Col 2: Copy & To Front
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PixelLabToolItem(label = "Copy", icon = Icons.Default.ContentCopy, onClick = onDuplicateLayer)
                PixelLabToolItem(label = "To Front", icon = Icons.Default.VerticalAlignTop, onClick = onBringToFront)
            }
            // Col 3: To Back & Relative Size
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PixelLabToolItem(label = "To Back", icon = Icons.Default.VerticalAlignBottom, onClick = onSendToBack)
                PixelLabToolItem(label = "Size", icon = Icons.Default.FormatSize, onClick = { activeObjectEditor = ObjectToolSubEditor.SIZE })
            }
            // Col 4: Relative 6-Grid & Color/Tint
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PixelLabToolItem(
                    label = "Relative",
                    icon = Icons.Default.ZoomOutMap,
                    testTag = "tool_obj_relative_pos",
                    onClick = { activeObjectEditor = ObjectToolSubEditor.RELATIVE_POSITION }
                )
                PixelLabToolItem(
                    label = "Color",
                    icon = Icons.Default.Palette,
                    isActive = (selectedLayer as? ImageLayer)?.colorOverlayEnabled == true,
                    onClick = { activeObjectEditor = ObjectToolSubEditor.COLOR_TINT }
                )
            }
            // Col 5: Stroke & Shadow
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val hasStroke = if (selectedLayer is ImageLayer) selectedLayer.stroke.enabled else (selectedLayer as ShapeLayer).stroke.enabled
                val hasShadow = if (selectedLayer is ImageLayer) selectedLayer.shadow.enabled else (selectedLayer as ShapeLayer).shadow.enabled
                PixelLabToolItem(label = "Stroke", icon = Icons.Default.LineWeight, isActive = hasStroke, onClick = { activeObjectEditor = ObjectToolSubEditor.STROKE })
                PixelLabToolItem(label = "Shadow", icon = Icons.Default.FilterDrama, isActive = hasShadow, onClick = { activeObjectEditor = ObjectToolSubEditor.SHADOW })
            }
            // Col 6: 4-Point Perspective & Bezier Mask
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val hasPersp = if (selectedLayer is ImageLayer) selectedLayer.perspectiveQuad.enabled else (selectedLayer as ShapeLayer).perspectiveQuad.enabled
                val hasMask = if (selectedLayer is ImageLayer) selectedLayer.maskMode != MaskPresetMode.NONE else (selectedLayer as ShapeLayer).maskMode != MaskPresetMode.NONE
                PixelLabToolItem(
                    label = "Perspective",
                    icon = Icons.Default.Crop,
                    isActive = hasPersp,
                    testTag = "tool_obj_perspective",
                    onClick = { activeObjectEditor = ObjectToolSubEditor.PERSPECTIVE }
                )
                PixelLabToolItem(
                    label = "Mask",
                    icon = Icons.Default.Gesture,
                    isActive = hasMask,
                    testTag = "tool_obj_mask",
                    onClick = { activeObjectEditor = ObjectToolSubEditor.MASK }
                )
            }
            if (selectedLayer is ImageLayer) {
                // Col 7: Erase Color (Chroma Key) & Interactive Manual Eraser
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PixelLabToolItem(
                        label = "Erase Color",
                        icon = Icons.Default.InvertColors,
                        isActive = selectedLayer.eraseColorEnabled,
                        onClick = { activeObjectEditor = ObjectToolSubEditor.ERASE_COLOR }
                    )
                    PixelLabToolItem(
                        label = "Eraser",
                        icon = Icons.Default.Brush,
                        isActive = isEraserMode || selectedLayer.eraserStrokes.isNotEmpty(),
                        testTag = "tool_obj_manual_eraser",
                        onClick = {
                            onSetEraserMode(true)
                            activeObjectEditor = ObjectToolSubEditor.MANUAL_ERASER
                        }
                    )
                }
                // Col 8: 3D Shape & Color Filters
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PixelLabToolItem(
                        label = "3D Shape",
                        icon = Icons.Default.ViewInAr,
                        isActive = selectedLayer.is3dShapeEnabled,
                        onClick = { activeObjectEditor = ObjectToolSubEditor.SHAPE_3D }
                    )
                    PixelLabToolItem(
                        label = "Filters",
                        icon = Icons.Default.Tune,
                        onClick = { activeObjectEditor = ObjectToolSubEditor.COLOR_FILTERS }
                    )
                }
                // Col 9: Flip H, Flip V & Crop
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PixelLabToolItem(
                        label = "Flip H",
                        icon = Icons.Default.Flip,
                        isActive = selectedLayer.flipHorizontal,
                        testTag = "tool_obj_flip_h",
                        onClick = { onToggleImageFlip(true, false) }
                    )
                    PixelLabToolItem(
                        label = "Flip V",
                        icon = Icons.Default.Flip,
                        isActive = selectedLayer.flipVertical,
                        testTag = "tool_obj_flip_v",
                        onClick = { onToggleImageFlip(false, true) }
                    )
                }
                // Col 9B: Interactive Crop & Radius
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PixelLabToolItem(
                        label = "Crop",
                        icon = Icons.Default.Crop,
                        isActive = selectedLayer.isCircularMask || selectedLayer.cornerRadius > 0f || selectedLayer.originalBitmap != null,
                        testTag = "tool_obj_crop",
                        onClick = { activeObjectEditor = ObjectToolSubEditor.CROP_RADIUS }
                    )
                    PixelLabToolItem(
                        label = "Size",
                        icon = Icons.Default.FormatSize,
                        onClick = { activeObjectEditor = ObjectToolSubEditor.SIZE }
                    )
                }
            } else if (selectedLayer is ShapeLayer) {
                // Col 7: Bezier Curve Editor & 3D Shape
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PixelLabToolItem(
                        label = "Bezier",
                        icon = Icons.Default.Gesture,
                        isActive = selectedLayer.shapeKind == ShapeKind.BEZIER_PATH,
                        testTag = "tool_obj_bezier_editor",
                        onClick = { activeObjectEditor = ObjectToolSubEditor.BEZIER_EDITOR }
                    )
                    PixelLabToolItem(
                        label = "3D Shape",
                        icon = Icons.Default.ViewInAr,
                        isActive = selectedLayer.is3dExtruded,
                        onClick = { activeObjectEditor = ObjectToolSubEditor.SHAPE_3D }
                    )
                }
            }
            // Col 10: 3D Rotate & Opacity
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PixelLabToolItem(label = "3D Rotate", icon = Icons.Default.Rotate90DegreesCcw, onClick = { activeObjectEditor = ObjectToolSubEditor.ROTATE_3D })
                PixelLabToolItem(label = "Opacity", icon = Icons.Default.Opacity, onClick = { activeObjectEditor = ObjectToolSubEditor.OPACITY })
            }
        }
        return
    }

    // 5. Default PixelLab Hexagon Creation Tools (Stickers, Import, Draw, Shapes, Bezier, Arrow, Circle, Hexagon, Star)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelLabToolItem(
            label = "Stickers",
            icon = Icons.Default.Star,
            onClick = { showStickersSheet = true }
        )
        PixelLabToolItem(
            label = "Import",
            icon = Icons.Default.AddPhotoAlternate,
            onClick = {
                forceCreationToolbar = false
                onPickGalleryImageLayer()
            }
        )
        PixelLabToolItem(
            label = "Draw",
            icon = Icons.Default.Brush,
            isActive = isDrawingMode,
            onClick = {
                forceCreationToolbar = false
                onSetDrawingMode(!isDrawingMode)
            }
        )
        PixelLabToolItem(
            label = "Shapes",
            icon = Icons.Default.FormatShapes,
            onClick = {
                forceCreationToolbar = false
                onAddShape(ShapeKind.RECTANGLE)
            }
        )
        PixelLabToolItem(
            label = "Bezier",
            icon = Icons.Default.Gesture,
            badgeText = "PRO",
            testTag = "tool_create_bezier",
            onClick = {
                forceCreationToolbar = false
                onAddShape(ShapeKind.BEZIER_PATH)
                activeObjectEditor = ObjectToolSubEditor.BEZIER_EDITOR
            }
        )
        PixelLabToolItem(
            label = "Arrow",
            icon = Icons.Default.NearMe,
            onClick = {
                forceCreationToolbar = false
                onAddShape(ShapeKind.ARROW)
            }
        )
        PixelLabToolItem(
            label = "Circle",
            icon = Icons.Default.Circle,
            onClick = {
                forceCreationToolbar = false
                onAddShape(ShapeKind.CIRCLE)
            }
        )
        PixelLabToolItem(
            label = "Hexagon",
            icon = Icons.Default.Gesture,
            onClick = {
                forceCreationToolbar = false
                onAddShape(ShapeKind.HEXAGON)
            }
        )
        PixelLabToolItem(
            label = "Star",
            icon = Icons.Default.Star,
            onClick = {
                forceCreationToolbar = false
                onAddShape(ShapeKind.STAR)
            }
        )
    }
}

// ============================================================================
// TAB 4: PIXELLAB DUAL SQUARES CANVAS BACKGROUND INSPECTOR
// ============================================================================
@Composable
fun CanvasBackgroundInspector(
    background: CanvasBackground,
    canvasWidth: Int = 1080,
    canvasHeight: Int = 1080,
    onUpdateBackground: ((CanvasBackground) -> CanvasBackground) -> Unit,
    onPickBackgroundGalleryImage: () -> Unit,
    onSelectAspectPreset: (CanvasAspectPreset) -> Unit = {},
    onOpenCustomSizeDialog: () -> Unit = {},
    onResetCanvas: () -> Unit = {}
) {
    val palette = LocalStudioPalette.current
    var activeEditor by remember { mutableStateOf<String?>(null) }

    if (activeEditor == "COLOR") {
        Column(modifier = Modifier.fillMaxSize()) {
            PixelLabSubEditorHeader(
                title = "رنگ پس‌زمینه (Background Color)",
                onApply = { activeEditor = null },
                onDiscard = { activeEditor = null }
            )
            LazyRow(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PIXELLAB_SWATCHES) { hex ->
                    val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable {
                                onUpdateBackground { it.copy(type = BackgroundType.COLOR, primaryColorHex = hex, backgroundBitmap = null) }
                            }
                    )
                }
            }
        }
        return
    }

    if (activeEditor == "GRADIENT") {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            PixelLabSubEditorHeader(
                title = "گرادینت پس‌زمینه (Gradient Builder)",
                subtitle = if (background.isRadialGradient) "شعاعی (Radial)" else "خطی (${background.gradientAngle.roundToInt()}°)",
                onApply = { activeEditor = null },
                onDiscard = { activeEditor = null }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (!background.isRadialGradient) palette.primaryAccent else palette.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        onUpdateBackground { it.copy(type = BackgroundType.GRADIENT, isRadialGradient = false, backgroundBitmap = null) }
                    }
                ) {
                    Text("خطی (Linear)", fontSize = 11.sp, color = if (!background.isRadialGradient) Color.Black else palette.textPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Surface(
                    color = if (background.isRadialGradient) palette.primaryAccent else palette.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        onUpdateBackground { it.copy(type = BackgroundType.GRADIENT, isRadialGradient = true, backgroundBitmap = null) }
                    }
                ) {
                    Text("شعاعی (Radial)", fontSize = 11.sp, color = if (background.isRadialGradient) Color.Black else palette.textPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                if (!background.isRadialGradient) {
                    Slider(
                        value = background.gradientAngle,
                        onValueChange = { ang -> onUpdateBackground { it.copy(type = BackgroundType.GRADIENT, gradientAngle = ang, backgroundBitmap = null) } },
                        valueRange = 0f..360f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            LazyRow(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PIXELLAB_SWATCHES) { hex ->
                    val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable {
                                onUpdateBackground { it.copy(type = BackgroundType.GRADIENT, primaryColorHex = hex, backgroundBitmap = null) }
                            }
                    )
                }
            }
            LazyRow(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PIXELLAB_SWATCHES.reversed()) { hex ->
                    val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable {
                                onUpdateBackground { it.copy(type = BackgroundType.GRADIENT, secondaryColorHex = hex, backgroundBitmap = null) }
                            }
                    )
                }
            }
        }
        return
    }

    if (activeEditor == "IMAGE_SIZE") {
        Column(modifier = Modifier.fillMaxSize()) {
            PixelLabSubEditorHeader(
                title = "ابعاد بوم (Image Size: ${canvasWidth}×${canvasHeight} px)",
                onApply = { activeEditor = null },
                onDiscard = { activeEditor = null }
            )
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .clickable {
                                activeEditor = null
                                onOpenCustomSizeDialog()
                            }
                            .testTag("bg_custom_canvas_size_button"),
                        colors = CardDefaults.cardColors(containerColor = palette.primaryAccent.copy(alpha = 0.2f)),
                        border = BorderStroke(1.5.dp, palette.primaryAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ابعاد سفارشی (px)", fontWeight = FontWeight.Bold, color = palette.primaryAccent, fontSize = 12.sp)
                            Text("${canvasWidth}×${canvasHeight} px", color = palette.textPrimary, fontSize = 10.sp)
                        }
                    }
                }
                items(DEFAULT_ASPECT_PRESETS) { preset ->
                    Card(
                        modifier = Modifier.clickable {
                            onSelectAspectPreset(preset)
                            activeEditor = null
                        },
                        colors = CardDefaults.cardColors(containerColor = palette.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(preset.ratioLabel, fontWeight = FontWeight.Bold, color = palette.primaryAccent, fontSize = 12.sp)
                            Text(preset.nameFa, color = palette.textPrimary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        return
    }

    // PixelLab Dual-Square Tools (Color, Gradient, Transparent, Image Size, From Gallery, Reset Canvas)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelLabToolItem(
            label = "Color",
            icon = Icons.Default.ColorLens,
            isActive = background.type == BackgroundType.COLOR,
            onClick = { activeEditor = "COLOR" }
        )

        PixelLabToolItem(
            label = "Gradient",
            icon = Icons.Default.Palette,
            isActive = background.type == BackgroundType.GRADIENT,
            onClick = { activeEditor = "GRADIENT" }
        )

        PixelLabToolItem(
            label = "Transparent",
            icon = Icons.Default.InvertColors,
            isActive = background.type == BackgroundType.TRANSPARENT,
            onClick = {
                onUpdateBackground {
                    it.copy(
                        type = if (it.type == BackgroundType.TRANSPARENT) BackgroundType.COLOR else BackgroundType.TRANSPARENT,
                        backgroundBitmap = null
                    )
                }
            }
        )

        PixelLabToolItem(
            label = "Image Size",
            icon = Icons.Default.Crop,
            onClick = { activeEditor = "IMAGE_SIZE" }
        )

        PixelLabToolItem(
            label = "From Gallery",
            icon = Icons.Default.AddPhotoAlternate,
            onClick = onPickBackgroundGalleryImage
        )

        PixelLabToolItem(
            label = "پاکسازی بوم",
            icon = Icons.Default.Delete,
            onClick = onResetCanvas
        )
    }
}

// ============================================================================
// TAB 5: PIXELLAB MAGIC WAND POST-FX INSPECTOR
// ============================================================================
@Composable
fun PostFxInspector(
    postFx: CanvasPostFx,
    onUpdatePostFx: ((CanvasPostFx) -> CanvasPostFx) -> Unit
) {
    val palette = LocalStudioPalette.current
    var activeFxTool by remember { mutableStateOf<String?>(null) }

    if (activeFxTool != null) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (activeFxTool) {
                "VIGNETTE" -> {
                    PixelLabSubEditorHeader(
                        title = "وینیت (Vignette)",
                        subtitle = if (postFx.vignetteEnabled) "${(postFx.vignetteIntensity * 100).roundToInt()}%" else "خاموش",
                        onApply = { activeFxTool = null },
                        onDiscard = { activeFxTool = null }
                    )
                    Slider(
                        value = postFx.vignetteIntensity,
                        onValueChange = { v -> onUpdatePostFx { it.copy(vignetteEnabled = v > 0f, vignetteIntensity = v) } },
                        valueRange = 0f..1f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                    )
                }
                "NOISE" -> {
                    PixelLabSubEditorHeader(
                        title = "نویز (Noise)",
                        subtitle = if (postFx.noiseEnabled) "${(postFx.noiseIntensity * 100).roundToInt()}%" else "خاموش",
                        onApply = { activeFxTool = null },
                        onDiscard = { activeFxTool = null }
                    )
                    Slider(
                        value = postFx.noiseIntensity,
                        onValueChange = { n -> onUpdatePostFx { it.copy(noiseEnabled = n > 0f, noiseIntensity = n) } },
                        valueRange = 0f..1f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                    )
                }
                "BRIGHTNESS" -> {
                    PixelLabSubEditorHeader(
                        title = "روشنایی (Brightness)",
                        subtitle = "${postFx.brightness.roundToInt()}%",
                        onApply = { activeFxTool = null },
                        onDiscard = { activeFxTool = null }
                    )
                    Slider(
                        value = postFx.brightness,
                        onValueChange = { b -> onUpdatePostFx { it.copy(brightness = b) } },
                        valueRange = -50f..50f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                    )
                }
                "CONTRAST" -> {
                    PixelLabSubEditorHeader(
                        title = "کنتراست (Contrast)",
                        subtitle = "${(postFx.contrast * 100).roundToInt()}%",
                        onApply = { activeFxTool = null },
                        onDiscard = { activeFxTool = null }
                    )
                    Slider(
                        value = postFx.contrast,
                        onValueChange = { c -> onUpdatePostFx { it.copy(contrast = c) } },
                        valueRange = 0.5f..1.8f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                    )
                }
                "HUE" -> {
                    PixelLabSubEditorHeader(
                        title = "رنگ‌پایگی (Hue)",
                        subtitle = "${postFx.hue.roundToInt()}°",
                        onApply = { activeFxTool = null },
                        onDiscard = { activeFxTool = null }
                    )
                    Slider(
                        value = postFx.hue,
                        onValueChange = { h -> onUpdatePostFx { it.copy(hue = h) } },
                        valueRange = -180f..180f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                    )
                }
                "SATURATION" -> {
                    PixelLabSubEditorHeader(
                        title = "غلظت رنگ (Saturation)",
                        subtitle = "${(postFx.saturation * 100).roundToInt()}%",
                        onApply = { activeFxTool = null },
                        onDiscard = { activeFxTool = null }
                    )
                    Slider(
                        value = postFx.saturation,
                        onValueChange = { s -> onUpdatePostFx { it.copy(saturation = s) } },
                        valueRange = 0f..2.0f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(thumbColor = palette.primaryAccent, activeTrackColor = palette.primaryAccent)
                    )
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelLabToolItem(
            label = "Vignette",
            icon = Icons.Default.Tonality,
            isActive = postFx.vignetteEnabled,
            onClick = { activeFxTool = "VIGNETTE" }
        )

        PixelLabToolItem(
            label = "Noise",
            icon = Icons.Default.Grain,
            isActive = postFx.noiseEnabled,
            onClick = { activeFxTool = "NOISE" }
        )

        PixelLabToolItem(
            label = "Stripes",
            icon = Icons.Default.LineWeight,
            isActive = postFx.stripesEnabled,
            onClick = {
                onUpdatePostFx {
                    it.copy(stripesEnabled = !it.stripesEnabled, stripesAlpha = 0.12f)
                }
            }
        )

        PixelLabToolItem(
            label = "Brightness",
            icon = Icons.Default.WbSunny,
            onClick = { activeFxTool = "BRIGHTNESS" }
        )

        PixelLabToolItem(
            label = "Contrast",
            icon = Icons.Default.Tune,
            onClick = { activeFxTool = "CONTRAST" }
        )

        PixelLabToolItem(
            label = "Hue",
            icon = Icons.Default.Palette,
            onClick = { activeFxTool = "HUE" }
        )

        PixelLabToolItem(
            label = "Saturation",
            icon = Icons.Default.InvertColors,
            onClick = { activeFxTool = "SATURATION" }
        )
    }
}

// ============================================================================
// GEMINI AI STUDIO INSPECTOR (COMMAND-LINE PROMPT BAR + INLINE REASONING + MODEL SELECTOR)
// ============================================================================
@Composable
fun GeminiAiStudioInspector(
    aiState: AiTaskUiState,
    activeLearnedStyle: PlpLearnedStyleDNA? = null,
    selectedGeminiModel: String = com.example.ui.agent.GeminiModelPreset.GEMINI_3_8_FLASH.modelId,
    activeTransactionSteps: List<com.example.ui.agent.AgentExecutionStep> = emptyList(),
    onSelectGeminiModel: (String) -> Unit = {},
    onGenerateAutoLayout: (String) -> Unit,
    onGenerateAiImage: (String, Boolean) -> Unit,
    onGenerateAiSticker: (String) -> Unit,
    onEditWithAi: (String) -> Unit,
    onDismissAiStatus: () -> Unit,
    onExecuteAgentCommand: (String) -> Unit,
    onOpenFullAgentSheet: () -> Unit,
    onLearnCurrentCanvasStyle: () -> Unit = {},
    onGenerateFromLearnedStyle: (PlpLearnedStyleDNA, String, String) -> Unit = { _, _, _ -> },
    onApplyLearnedStyleToCanvas: (PlpLearnedStyleDNA) -> Unit = {},
    onRequestAutoLayout: () -> Unit = {},
    isAntiGravityActive: Boolean = false,
    onToggleAntiGravity: () -> Unit = {}
) {
    val palette = LocalStudioPalette.current
    var promptText by remember { mutableStateOf("") }
    var showReasoningDetails by remember { mutableStateOf(true) }
    val isBusy = aiState is AiTaskUiState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Command-Line Style Prompt Bar (`>_`)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                leadingIcon = {
                    Text(
                        text = ">_",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = palette.primaryAccent,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                },
                placeholder = {
                    Text(
                        text = if (isBusy) {
                            (aiState as? AiTaskUiState.Loading)?.statusText ?: "در حال اجرای زنجیره ابزارها..."
                        } else {
                            "دستور طراحی به ایجنت (${com.example.ui.agent.formatModelShortBadge(selectedGeminiModel)})..."
                        },
                        fontSize = 11.sp,
                        color = if (isBusy) palette.primaryAccent else palette.textSecondary
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("agent_inline_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primaryAccent,
                    unfocusedBorderColor = palette.border,
                    focusedContainerColor = Color(0xFF070B12),
                    unfocusedContainerColor = Color(0xFF070B12)
                )
            )

            Button(
                onClick = {
                    if (promptText.isNotBlank()) {
                        onExecuteAgentCommand(promptText)
                        promptText = ""
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent, contentColor = Color.Black),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("agent_inline_execute_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("اجرا", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            IconButton(
                onClick = onOpenFullAgentSheet,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surfaceVariant)
            ) {
                Icon(Icons.Default.Tune, contentDescription = "تنظیمات کامل ایجنت", tint = palette.secondaryAccent)
            }
        }

        // 2. Inline Manual Gemini Model Switcher Row (Never Force-Locked to 3.5-flash-lite)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "مدل فعال:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textSecondary
            )
            com.example.ui.agent.GeminiModelPreset.entries.forEach { preset ->
                val isSelected = selectedGeminiModel.equals(preset.modelId, ignoreCase = true)
                Surface(
                    color = if (isSelected) palette.primaryAccent.copy(alpha = 0.22f) else palette.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) palette.primaryAccent else palette.border
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectGeminiModel(preset.modelId) }
                        .testTag("inline_model_chip_${preset.name}")
                ) {
                    Text(
                        text = if (isSelected) "✓ ${preset.shortBadge}" else preset.shortBadge,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) palette.primaryAccent else palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // 3. Inline Expandable Reasoning Card when steps exist
        if (activeTransactionSteps.isNotEmpty()) {
            Surface(
                color = Color(0xFF0A101D),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, palette.primaryAccent.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showReasoningDetails = !showReasoningDetails }
                    .testTag("inline_reasoning_steps_box")
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🧠 زنجیره استدلال زنده (${activeTransactionSteps.size} گام Function Calling)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryAccent
                        )
                        Text(
                            text = if (showReasoningDetails) "بستن ▲" else "جزئیات ▼",
                            fontSize = 10.sp,
                            color = palette.textSecondary
                        )
                    }
                    if (showReasoningDetails) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeTransactionSteps.forEachIndexed { idx, step ->
                                Surface(
                                    color = Color(0xFF141D2F),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${idx + 1}. ${step.titleFa}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "(${step.toolName})",
                                            fontSize = 9.sp,
                                            color = Color(0xFFC084FC)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Quick Action & Creative Flow Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Learn Current Canvas / PLP Style Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E1065))
                    .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(8.dp))
                    .clickable { onLearnCurrentCanvasStyle() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("agent_learn_plp_style_chip")
            ) {
                Text(
                    text = "🧠 یادگیری استایل این بوم",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE9D5FF),
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Clone / Build New Design with Active Learned PLP Style
            if (activeLearnedStyle != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF064E3B))
                        .border(1.dp, Color(0xFF34D399), RoundedCornerShape(8.dp))
                        .clickable {
                            val h = promptText.trim().ifBlank { "ترفند جدید هوش مصنوعی" }
                            promptText = ""
                            onGenerateFromLearnedStyle(
                                activeLearnedStyle,
                                h,
                                "طراحی‌شده با استایل ${activeLearnedStyle.styleName}"
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("agent_clone_plp_style_chip")
                ) {
                    Text(
                        text = "✨ ساخت مشابه «${activeLearnedStyle.styleName.take(16)}»",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA7F3D0),
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1B4B))
                        .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(8.dp))
                        .clickable { onApplyLearnedStyleToCanvas(activeLearnedStyle) }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("agent_apply_plp_style_chip")
                ) {
                    Text(
                        text = "🎨 انتقال استایل PLP به بوم",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC7D2FE),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F2B3B))
                    .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .clickable { onRequestAutoLayout() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "📐 چیدمان هوشمند طلایی",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00F0FF),
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isAntiGravityActive) Color(0xFF4C1D95) else Color(0xFF1E293B))
                    .border(
                        1.dp,
                        if (isAntiGravityActive) Color(0xFF38BDF8) else Color(0xFF334155),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleAntiGravity() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isAntiGravityActive) "🛸 جاذبه صفر (فعال)" else "🛸 آنتی‌گرویتی",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAntiGravityActive) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                    fontWeight = if (isAntiGravityActive) FontWeight.Bold else FontWeight.Normal
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { onExecuteAgentCommand("قفل همه لایه‌ها رو باز کن") }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "🔓 باز کردن قفل لایه‌ها",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFBBF24)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { onExecuteAgentCommand("طرح فعلی بوم رو نقد بصری کن و با تایپوگرافی سه‌بعدی بهبود بده") }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "✨ نقد و ارتقای طرح",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC4B5FD)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { onExecuteAgentCommand("پوستر تبلیغاتی حرفه‌ای با تیتر ۳بعدی و افکت نوری بساز") }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "🔥 پوستر فروش ویژه",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFCA5A5)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable {
                        val bgPrompt = promptText.ifBlank { "Dark futuristic neon studio background with subtle glowing geometric depth, 8k graphic design wallpaper" }
                        onGenerateAiImage(bgPrompt, true)
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "🌌 تولید پس‌زمینه AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF67E8F9)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable {
                        val stickerPrompt = promptText.ifBlank { "Golden 3D luxury crown emblem badge" }
                        onGenerateAiSticker(stickerPrompt)
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "🏷️ ساخت استیکر AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF86EFAC)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
                    .clickable {
                        val editPrompt = promptText.ifBlank { "Enhance studio lighting, vibrant contrast and cinematic glow" }
                        onEditWithAi(editPrompt)
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "🪄 ویرایش تصویر AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFDE047)
                )
            }
        }
    }
}



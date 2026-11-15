package com.example.ui.agent

/**
 * Selectable Gemini models in Settings so the app is never tied to a fixed/old model name.
 */
enum class GeminiModelPreset(
    val modelId: String,
    val shortBadge: String,
    val titleFa: String,
    val subtitleFa: String
) {
    GEMINI_3_8_FLASH(
        modelId = "gemini-3.8-flash",
        shortBadge = "3.8 Flash",
        titleFa = "Gemini 3.8 Flash (پیش‌فرض هوشمند)",
        subtitleFa = "متوازن، سریع و دقیق برای طراحی و Function Calling"
    ),
    GEMINI_4_ARGON(
        modelId = "gemini-4-argon",
        shortBadge = "4 Argon",
        titleFa = "Gemini 4 Argon (استدلال پیشرفته)",
        subtitleFa = "قدرتمندترین مدل استدلال برای ترکیب‌بندی‌های پیچیده چندمرحله‌ای"
    ),
    GEMINI_2_5_PRO(
        modelId = "gemini-2.5-pro",
        shortBadge = "2.5 Pro",
        titleFa = "Gemini 2.5 Pro (طراحی حرفه‌ای)",
        subtitleFa = "دقت بالا در چیدمان، تایپوگرافی و نقد بصری بوم"
    ),
    GEMINI_2_5_FLASH(
        modelId = "gemini-2.5-flash",
        shortBadge = "2.5 Flash",
        titleFa = "Gemini 2.5 Flash (سریع و پایدار)",
        subtitleFa = "سرعت پاسخگویی بالا و پایداری عالی در اجرای ابزارها"
    ),
    GEMINI_3_5_FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        shortBadge = "3.5 Lite",
        titleFa = "Gemini 3.5 Flash-Lite (سبک)",
        subtitleFa = "فوق‌سریع و سبک برای ویرایش‌های آنی و پرتعداد"
    )
}

fun findGeminiModelPreset(id: String): GeminiModelPreset? =
    GeminiModelPreset.entries.find { it.modelId.equals(id.trim(), ignoreCase = true) }

fun formatModelShortBadge(modelId: String): String {
    val preset = findGeminiModelPreset(modelId)
    if (preset != null) return preset.shortBadge
    return modelId.removePrefix("gemini-").replace("-preview", "").take(14)
}

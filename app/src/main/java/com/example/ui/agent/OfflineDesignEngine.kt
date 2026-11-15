package com.example.ui.agent

import com.example.data.BackgroundType
import com.example.data.CanvasBackground
import com.example.data.ImageLayer
import com.example.data.ShapeKind
import com.example.data.ShapeLayer
import com.example.data.StudioLayer
import com.example.data.StudioProject
import com.example.data.TextLayer
import org.json.JSONArray
import org.json.JSONObject

/**
 * High-resilience, intelligent Persian graphic design & layer management engine.
 * Processes natural language commands (including Persian half-spaces, digits, visual critique,
 * multi-layer poster generation, and targeted layer manipulation) without corrupting layer text.
 */
object OfflineDesignEngine {

    /**
     * Normalizes Persian/Arabic text so half-spaces (ZWNJ \u200c), Persian/Arabic digits,
     * and Arabic Yeh/Kaf variants match consistently across all commands.
     */
    fun normalizePersianCommand(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '\u200C', '\u200B', '\u00A0' -> sb.append(' ')
                'ي', 'ى' -> sb.append('ی')
                'ك' -> sb.append('ک')
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                else -> sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
            .replace(Regex("(\\d)\\s*بعدی"), "$1 بعدی")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Resolves which layer the user is referring to in natural language
     * (e.g. "تیتر", "زیرنویس", "شکل", "استیکر", or the currently selected layer).
     */
    private fun resolveTargetLayerFromCommand(
        normalizedCmd: String,
        project: StudioProject,
        selectedLayerId: String?
    ): StudioLayer? {
        val textLayers = project.layers.filterIsInstance<TextLayer>()
        val shapeLayers = project.layers.filterIsInstance<ShapeLayer>()
        val imageLayers = project.layers.filterIsInstance<ImageLayer>()

        if (normalizedCmd.contains("زیرنویس") || normalizedCmd.contains("زیرتیتر") ||
            normalizedCmd.contains("توضیحات") || normalizedCmd.contains("متن دوم") ||
            normalizedCmd.contains("متن پایین")
        ) {
            if (textLayers.size >= 2) {
                return textLayers.minByOrNull { it.fontSizeSp } ?: textLayers.last()
            }
        }

        if (normalizedCmd.contains("تیتر") || normalizedCmd.contains("عنوان اصلی") ||
            normalizedCmd.contains("متن اصلی") || normalizedCmd.contains("سرتیتر")
        ) {
            if (textLayers.isNotEmpty()) {
                return textLayers.maxByOrNull { it.fontSizeSp } ?: textLayers.first()
            }
        }

        if (normalizedCmd.contains("شکل") || normalizedCmd.contains("کادر") ||
            normalizedCmd.contains("شش ضلعی") || normalizedCmd.contains("دایره") ||
            normalizedCmd.contains("مستطیل") || normalizedCmd.contains("ستاره")
        ) {
            if (shapeLayers.isNotEmpty()) {
                return shapeLayers.find { it.id == selectedLayerId } ?: shapeLayers.last()
            }
        }

        if (normalizedCmd.contains("عکس") || normalizedCmd.contains("تصویر") ||
            normalizedCmd.contains("لوگو")
        ) {
            if (imageLayers.isNotEmpty()) {
                return imageLayers.find { it.id == selectedLayerId } ?: imageLayers.last()
            }
        }

        return project.layers.find { it.id == selectedLayerId } ?: project.layers.lastOrNull()
    }

    /**
     * Checks if a user input is a design/styling/layer instruction rather than literal text
     * to be written onto a layer.
     */
    fun isLikelyDesignInstruction(normalizedCmd: String): Boolean {
        val instructionKeywords = listOf(
            "نقد", "بهبود", "اصلاح", "زیباتر", "بررسی", "ارتقا", "پیشنهاد",
            "طرح", "بوم", "لایه", "پوستر", "استوری", "کاور", "تامبنیل", "تامنیل", "بنر",
            "سه بعدی", "3 بعدی", "3d", "دوبعدی", "2 بعدی", "2d",
            "سایه", "استروک", "دورخط", "حاشیه", "درخشش", "نئون", "طلایی", "لوکس", "سایبرپانک",
            "پس زمینه", "بکگراند", "گرادیان", "گرادینت", "وینیت", "افکت",
            "چیدمان", "تراز", "مرتب", "وسط چین", "راست چین", "چپ چین",
            "بزرگ", "کوچک", "درشت", "ریز", "بچرخون", "چرخش", "زاویه", "شفافیت", "کمرنگ", "پررنگ",
            "قفل", "مخفی", "پنهان", "حذف", "پاک", "کپی", "تکثیر", "ادغام", "بیار جلو", "ببر عقب", "ببر پشت",
            "ببر بالا", "ببر پایین", "ببر چپ", "ببر راست",
            "فونت", "لاله زار", "وزیر", "امیری", "انحنا", "خمیده", "انعکاس",
            "ضد جاذبه", "آنتی گرویتی", "بی وزنی", "شناور",
            "یاد بگیر", "یادگیری", "استایل plp", "مشابه", "مثلش",
            "بساز", "طراحی کن", "تغییر بده", "اعمال کن", "تنظیم کن", "بده", "کنید"
        )
        return instructionKeywords.any { normalizedCmd.contains(it) } || normalizedCmd.length > 36
    }

    private fun extractHeadlineAndSubtitleFromPrompt(rawCommand: String): Pair<String, String> {
        val afterColon = rawCommand.substringAfter(":", "").trim()
        val candidate = if (afterColon.isNotBlank()) afterColon else {
            rawCommand
                .replace(Regex("(?i)با استایل یادگرفته شده|با استایل یاد گرفته شده|مثل استایل یادگرفته شده|مثل پروژه plp|مشابه استایل|یک طرح|یک پوستر|برای|برام|بساز|درست کن|طراحی کن"), "")
                .trim()
        }
        if (candidate.contains("|")) {
            val parts = candidate.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            return (parts.getOrNull(0) ?: "طراحی با استایل PLP") to (parts.getOrNull(1) ?: "")
        }
        if (candidate.contains(" - ")) {
            val parts = candidate.split(" - ").map { it.trim() }.filter { it.isNotEmpty() }
            return (parts.getOrNull(0) ?: "طراحی با استایل PLP") to (parts.getOrNull(1) ?: "")
        }
        return candidate.ifBlank { "طراحی اختصاصی با استایل PLP" } to "نسخه بازآفرینی‌شده با موتور یادگیری استایل"
    }

    fun processCommand(
        userCommand: String,
        project: StudioProject,
        selectedLayerId: String?,
        activeLearnedStyle: PlpLearnedStyleDNA? = null
    ): AgentInferenceResult {
        val raw = userCommand.trim()
        val cmd = normalizePersianCommand(raw)
        val actions = JSONArray()
        var replyFa = "دستور شما با موفقیت روی بوم اجرا شد."
        var critiqueFa: String? = "طرح از نظر ترازبندی، خوانایی تایپوگرافی و سلسله‌مراتب لایه‌ها بررسی شد."
        val suggestions = mutableListOf<String>()
        var newTitle: String? = null

        val targetLayer = resolveTargetLayerFromCommand(cmd, project, selectedLayerId)

        when {
            // 0-A. Learn Style from Current PLP / Canvas Project
            cmd.contains("یاد بگیر") || cmd.contains("یادگیری استایل") ||
            (cmd.contains("ذخیره") && cmd.contains("استایل")) ||
            cmd.contains("learn style") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "LEARN_CURRENT_PLP_STYLE")
                })
                replyFa = "کل DNA استایل پروژه فعلی «${project.title}» (شامل تنظیمات ۳بعدی، پالت رنگ، دورخط، سایه، کادر کپسولی و چیدمان لایه‌ها) استخراج و در حافظه استایل‌های ایجنت ذخیره شد."
                critiqueFa = "اکنون می‌توانید با دستور «با استایل یادگرفته‌شده بساز: متن دلخواه» طرح‌های جدیدی دقیقاً به همین سبک بسازید یا استایل را روی بوم دیگری اعمال کنید."
                suggestions.add("با استایل یادگرفته‌شده بساز: آموزش هوش مصنوعی | قسمت اول")
                suggestions.add("استایل یادگرفته‌شده رو روی بوم فعلی اعمال کن")
            }

            // 0-B. Create New Project Cloning the Learned PLP Style DNA
            (cmd.contains("استایل یاد") || cmd.contains("پروژه plp") || cmd.contains("مثلش") || cmd.contains("مشابه استایل")) &&
            (cmd.contains("بساز") || cmd.contains("درست کن") || cmd.contains("طراحی کن") || raw.contains(":")) -> {
                val (headlineText, subtitleText) = extractHeadlineAndSubtitleFromPrompt(raw)
                val styleName = activeLearnedStyle?.styleName ?: "استایل PLP یادگرفته‌شده"
                actions.put(JSONObject().apply {
                    put("actionType", "CREATE_FROM_LEARNED_PLP_STYLE")
                    put("text", headlineText)
                    put("subtitleText", subtitleText)
                })
                newTitle = "$styleName - ${headlineText.take(18)}"
                replyFa = "طرح جدید با تیتر «$headlineText» دقیقاً بر اساس DNA استایل «$styleName» (شامل عمق ۳بعدی، رنگ‌بندی، دورخط، کادر کپسولی و چیدمان لایه‌ها) ساخته شد."
                critiqueFa = "ساختار بصری، زاویه نورپردازی ۳بعدی، کنتراست پس‌زمینه و کادر زیرنویس ۱۰۰٪ مطابق پروژه PLP مرجع بازسازی گردید."
                suggestions.add("استایل یادگرفته‌شده رو روی لایه‌های فعلی اعمال کن")
                suggestions.add("طرح فعلی بوم رو نقد بصری کن و با تایپوگرافی سه‌بعدی بهبود بده")
            }

            // 0-C. Transfer Learned PLP Style DNA onto Existing Canvas Layers (Preserves existing text!)
            (cmd.contains("استایل یاد") || cmd.contains("استایل plp")) &&
            (cmd.contains("اعمال") || cmd.contains("انتقال") || cmd.contains("بوم فعلی") || cmd.contains("لایه")) -> {
                val styleName = activeLearnedStyle?.styleName ?: "استایل PLP یادگرفته‌شده"
                actions.put(JSONObject().apply {
                    put("actionType", "APPLY_LEARNED_PLP_STYLE")
                })
                replyFa = "استایل «$styleName» بدون تغییر متن لایه‌های شما، روی تیتر، زیرنویس، اشکال و پس‌زمینه بوم فعلی اعمال شد."
                critiqueFa = "متن‌های موجود حفظ شدند و تمام ویژگی‌های سه‌بعدی، دورخط، سایه و گرادیان پروژه PLP مرجع به لایه‌های فعلی منتقل گردید."
                suggestions.add("چیدمان هوشمند لایه‌ها با نسبت طلایی")
                suggestions.add("با استایل یادگرفته‌شده بساز: تیتر جدید | زیرنویس جدید")
            }

            // 0. Layer Management & Canvas Cleanup Commands
            cmd.contains("حذف همه") || cmd.contains("پاک کردن همه") || cmd.contains("پاکسازی") ||
            cmd.contains("ریست بوم") || cmd.contains("خالی کردن بوم") ||
            cmd.contains("clear all") || cmd.contains("reset canvas") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "RESET_CANVAS")
                })
                replyFa = "تمام لایه‌ها و تصویر پس‌زمینه پاک‌سازی شدند و بوم به حالت آماده طراحی برگشت."
                critiqueFa = "بوم کاملاً تمیز و بدون لایه یا تصویر باقی‌مانده آماده شروع طراحی جدید است."
                suggestions.add("پوستر تبلیغاتی حرفه‌ای با تیتر ۳بعدی و افکت نوری بساز")
                suggestions.add("پوستر سایبرپانک نئونی طراحی کن")
            }

            cmd.contains("باز کردن قفل") || cmd.contains("قفل همه") || cmd.contains("آزاد کردن") ||
            cmd.contains("unlock") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "UNLOCK_ALL_LAYERS")
                })
                replyFa = "قفل تمام لایه‌های پروژه باز شد و اکنون همه لایه‌ها روی بوم قابل انتخاب و جابه‌جایی هستند."
                suggestions.add("طرح فعلی بوم رو نقد بصری کن و با تایپوگرافی سه‌بعدی بهبود بده")
                suggestions.add("چیدمان هوشمند لایه‌ها")
            }

            // 1. Visual Critique & Smart Design Optimization (NEVER overwrites layer text!)
            cmd.contains("نقد") || cmd.contains("تحلیل بصری") || cmd.contains("زیبایی شناسی") ||
            cmd.contains("پیشنهادهای خود") || cmd.contains("زیباتر") ||
            (cmd.contains("بهبود") && (cmd.contains("طرح") || cmd.contains("بوم") || cmd.contains("تایپوگرافی") || cmd.contains("کنتراست"))) ||
            (cmd.contains("اصلاح") && (cmd.contains("طرح") || cmd.contains("بوم") || cmd.contains("رنگ") || cmd.contains("چیدمان"))) ||
            cmd.contains("critique") -> {
                val textLayers = project.layers.filterIsInstance<TextLayer>()
                val shapeLayers = project.layers.filterIsInstance<ShapeLayer>()
                val lockedCount = project.layers.count { it.isLocked }

                if (lockedCount > 0) {
                    actions.put(JSONObject().apply {
                        put("actionType", "UNLOCK_ALL_LAYERS")
                    })
                }

                // Upgrade background if it's plain white or low-depth flat color without image
                val bg = project.background
                if (bg.backgroundBitmap == null && bg.type == BackgroundType.COLOR &&
                    (bg.primaryColorHex.equals("#FFFFFF", ignoreCase = true) || bg.primaryColorHex.equals("#F8FAFC", ignoreCase = true))
                ) {
                    actions.put(JSONObject().apply {
                        put("actionType", "SET_CANVAS_BACKGROUND")
                        put("primaryColorHex", "#090D16")
                        put("secondaryColorHex", "#1E1B4B")
                        put("useGradient", true)
                    })
                }

                if (textLayers.isEmpty() && shapeLayers.isEmpty() && project.layers.isEmpty()) {
                    // Canvas is completely empty: create a balanced 3D showcase without using raw command as text
                    actions.put(JSONObject().apply {
                        put("actionType", "SET_CANVAS_BACKGROUND")
                        put("primaryColorHex", "#090D16")
                        put("secondaryColorHex", "#1E1B4B")
                        put("useGradient", true)
                    })
                    actions.put(JSONObject().apply {
                        put("actionType", "ADD_TEXT_LAYER")
                        put("text", "پیکسل‌لب استودیو پرو")
                        put("layerName", "تیتر اصلی سه‌بعدی")
                        put("fontId", "lalezar")
                        put("fontSizeSp", 54.0)
                        put("primaryColorHex", "#38BDF8")
                        put("secondaryColorHex", "#818CF8")
                        put("useGradient", true)
                        put("is3dEnabled", true)
                        put("depth3d", 24)
                        put("strokeEnabled", true)
                        put("strokeColorHex", "#090D16")
                        put("strokeWidth", 6.0)
                        put("shadowEnabled", true)
                        put("shadowColorHex", "#38BDF8")
                        put("shadowBlur", 22.0)
                        put("x", 0.0)
                        put("y", -0.06)
                    })
                    actions.put(JSONObject().apply {
                        put("actionType", "SET_POST_FX")
                        put("vignetteEnabled", true)
                    })
                    replyFa = "بوم خالی بود؛ یک ترکیب‌بندی استاندارد با پس‌زمینه گرادیان عمیق و تیتر سه‌بعدی درخشان ایجاد شد."
                    critiqueFa = "نقد بصری: برای ایجاد نقطه کانونی قوی، کنتراست پس‌زمینه تیره با تایپوگرافی سه‌بعدی فیروزه‌ای-بنفش و افکت وینیت ترکیب شد."
                } else {
                    // Enhance existing layers while strictly preserving their text content
                    val headline = textLayers.maxByOrNull { it.fontSizeSp }
                    val otherTexts = textLayers.filter { it.id != headline?.id }

                    if (headline != null) {
                        actions.put(JSONObject().apply {
                            put("actionType", "UPDATE_TEXT_BY_ID")
                            put("targetLayerId", headline.id)
                            // Note: DO NOT set "text" so existing headline text is 100% preserved!
                            put("is3dEnabled", true)
                            put("depth3d", headline.depth3d.coerceAtLeast(22))
                            put("obliqueAngle3d", 45.0)
                            put("strokeEnabled", true)
                            put("strokeColorHex", "#090D16")
                            put("strokeWidth", headline.stroke.width.coerceAtLeast(6f).toDouble())
                            put("shadowEnabled", true)
                            put("shadowColorHex", if (headline.useGradientFill) headline.colorHex else "#00F0FF")
                            put("shadowBlur", 24.0)
                            put("embossEnabled", true)
                            if (!headline.useGradientFill && headline.colorHex.equals("#FFFFFF", ignoreCase = true)) {
                                put("primaryColorHex", "#FDE047")
                                put("secondaryColorHex", "#F59E0B")
                                put("useGradient", true)
                            }
                            if ( kotlin.math.abs(headline.x) < 0.08f) {
                                put("x", 0.0)
                            }
                        })
                    }

                    otherTexts.forEach { sub ->
                        actions.put(JSONObject().apply {
                            put("actionType", "UPDATE_TEXT_BY_ID")
                            put("targetLayerId", sub.id)
                            // Strictly preserve sub.text
                            put("shadowEnabled", true)
                            put("shadowColorHex", "#000000")
                            put("shadowBlur", 14.0)
                            put("strokeEnabled", true)
                            put("strokeColorHex", "#0F172A")
                            put("strokeWidth", sub.stroke.width.coerceAtLeast(3.5f).toDouble())
                            if (kotlin.math.abs(sub.x) < 0.08f) {
                                put("x", 0.0)
                            }
                        })
                    }

                    if (cmd.contains("استیکر")) {
                        actions.put(JSONObject().apply {
                            put("actionType", "ADD_BUILTIN_STICKER")
                            put("stickerId", "crown_gold")
                            put("x", 0.0)
                            put("y", -0.32)
                            put("scale", 0.78)
                        })
                    }

                    actions.put(JSONObject().apply {
                        put("actionType", "SET_POST_FX")
                        put("vignetteEnabled", true)
                    })

                    val headlineLabel = headline?.text?.take(22) ?: "اصلی"
                    replyFa = "نقد بصری بوم انجام شد و بدون تغییر متن لایه‌ها، عمق سه‌بعدی، کنتراست، سایه‌زنی و تراز لایه‌ها ارتقا یافت."
                    critiqueFa = buildString {
                        append("تحلیل تخصصی بوم (${project.layers.size} لایه): ")
                        if (headline != null) {
                            append("۱. لایه تیتر «$headlineLabel» با اکستروژن سه‌بعدی (عمق ۲۲)، دورخط تیره و درخشش محیطی تقویت شد تا نقطه کانونی طرح برجسته شود. ")
                        }
                        if (otherTexts.isNotEmpty()) {
                            append("۲. خوانایی ${otherTexts.size} لایه متنی فرعی با سایه نرم و تفکیک از پس‌زمینه بهبود یافت. ")
                        }
                        append("۳. افکت وینیت سینمایی برای هدایت تمرکز چشم مخاطب به مرکز کادر فعال گردید.")
                    }
                }
                suggestions.add("چیدمان هوشمند لایه‌ها با نسبت طلایی")
                suggestions.add("تغییر استایل تیتر به طلایی لوکس")
                suggestions.add("افزودن استیکر تاج طلایی")
            }

            // 2. Promotional / Commercial / Discount Poster Templates
            cmd.contains("پوستر تبلیغاتی") || cmd.contains("فروش ویژه") || cmd.contains("تخفیف") ||
            cmd.contains("حراج") || cmd.contains("بنر تبلیغاتی") || cmd.contains("پوستر فروش") -> {
                actions.put(JSONObject().apply { put("actionType", "CLEAR_ALL_LAYERS") })
                actions.put(JSONObject().apply {
                    put("actionType", "SET_CANVAS_BACKGROUND")
                    put("primaryColorHex", "#180620")
                    put("secondaryColorHex", "#090D16")
                    put("useGradient", true)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "ADD_SHAPE_LAYER")
                    put("shapeKind", "BADGE")
                    put("layerName", "نشان حراج ویژه")
                    put("primaryColorHex", "#F59E0B")
                    put("secondaryColorHex", "#B45309")
                    put("useGradient", true)
                    put("opacity", 0.28)
                    put("scale", 1.35)
                    put("x", 0.0)
                    put("y", -0.06)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "ADD_BUILTIN_STICKER")
                    put("stickerId", "fire")
                    put("x", 0.0)
                    put("y", -0.28)
                    put("scale", 0.85)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "ADD_TEXT_LAYER")
                    put("text", "فروش ویژه فصل")
                    put("layerName", "تیتر اصلی تبلیغاتی")
                    put("fontId", "lalezar")
                    put("fontSizeSp", 58.0)
                    put("primaryColorHex", "#FDE047")
                    put("secondaryColorHex", "#F97316")
                    put("useGradient", true)
                    put("is3dEnabled", true)
                    put("depth3d", 28)
                    put("embossEnabled", true)
                    put("strokeEnabled", true)
                    put("strokeColorHex", "#270905")
                    put("strokeWidth", 7.0)
                    put("shadowEnabled", true)
                    put("shadowColorHex", "#F97316")
                    put("shadowBlur", 26.0)
                    put("x", 0.0)
                    put("y", -0.05)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "ADD_TEXT_LAYER")
                    put("text", "تخفیف استثنایی تا ۵۰٪ • زمان محدود")
                    put("layerName", "زیرنویس پیشنهاد ویژه")
                    put("fontId", "vazirmatn")
                    put("fontSizeSp", 24.0)
                    put("primaryColorHex", "#FFFFFF")
                    put("backgroundPillEnabled", true)
                    put("backgroundPillColorHex", "#CC7C2D12")
                    put("shadowEnabled", true)
                    put("shadowColorHex", "#000000")
                    put("shadowBlur", 14.0)
                    put("x", 0.0)
                    put("y", 0.22)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "SET_POST_FX")
                    put("vignetteEnabled", true)
                })
                newTitle = "پوستر تبلیغاتی فروش ویژه"
                replyFa = "پوستر تبلیغاتی حرفه‌ای با تیتر سه‌بعدی طلایی-آتشین، نشان پس‌زمینه، استیکر و زیرنویس کپسولی ساخته شد."
                critiqueFa = "ترکیب رنگ گرم طلایی-نارنجی روی پس‌زمینه تیره یاقوتی، بیشترین نرخ جلب توجه (CTR) را برای پوسترهای فروش ویژه ایجاد می‌کند."
                suggestions.add("تغییر ابعاد به استوری اینستاگرام (۹:۱۶)")
                suggestions.add("ویرایش متن تیتر اصلی")
            }

            // 3. Gaming / Stream / Tournament Cover
            cmd.contains("گیمینگ") || cmd.contains("بازی") || cmd.contains("استریم") ||
            cmd.contains("تورنمنت") || cmd.contains("گیم") || cmd.contains("gaming") -> {
                actions.put(JSONObject().apply { put("actionType", "CLEAR_ALL_LAYERS") })
                actions.put(JSONObject().apply {
                    put("actionType", "SET_CANVAS_BACKGROUND")
                    put("primaryColorHex", "#050816")
                    put("secondaryColorHex", "#1E1B4B")
                    put("useGradient", true)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "ADD_SHAPE_LAYER")
                    put("shapeKind", "HEXAGON")
                    put("layerName", "شیلد گیمینگ")
                    put("primaryColorHex", "#10B981")
                    put("secondaryColorHex", "#3B82F6")
                    put("useGradient", true)
                    put("opacity", 0.32)
                    put("scale", 1.3)
                    put("x", 0.0)
                    put("y", -0.05)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "ADD_TEXT_LAYER")
                    put("text", "نبرد قهرمانان")
                    put("layerName", "تیتر گیمینگ")
                    put("fontId", "lalezar")
                    put("fontSizeSp", 58.0)
                    put("primaryColorHex", "#34D399")
                    put("secondaryColorHex", "#06B6D4")
                    put("useGradient", true)
                    put("is3dEnabled", true)
                    put("depth3d", 30)
                    put("strokeEnabled", true)
                    put("strokeColorHex", "#022C22")
                    put("strokeWidth", 8.0)
                    put("shadowEnabled", true)
                    put("shadowColorHex", "#10B981")
                    put("shadowBlur", 28.0)
                    put("x", 0.0)
                    put("y", -0.06)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "ADD_TEXT_LAYER")
                    put("text", "استریم زنده اختصاصی • تورنمنت حرفه‌ای")
                    put("layerName", "زیرنویس گیمینگ")
                    put("fontId", "vazirmatn")
                    put("fontSizeSp", 22.0)
                    put("primaryColorHex", "#F8FAFC")
                    put("backgroundPillEnabled", true)
                    put("backgroundPillColorHex", "#CC064E3B")
                    put("x", 0.0)
                    put("y", 0.22)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "SET_POST_FX")
                    put("vignetteEnabled", true)
                    put("noiseEnabled", true)
                })
                newTitle = "کاور گیمینگ حرفه‌ای"
                replyFa = "کاور گیمینگ با تایپوگرافی سه‌بعدی نئون زمردی، شیلد شش‌ضلعی و افکت نوری ساخته شد."
                suggestions.add("تغییر ابعاد به تامنیل یوتیوب (۱۶:۹)")
                suggestions.add("افزودن استیکر موشک")
            }

            // 4. Layer Merge / Delete / Duplicate / Order / Visibility / Lock
            cmd.contains("ادغام") || cmd.contains("merge") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "MERGE_VISIBLE_LAYERS")
                })
                replyFa = "لایه‌های قابل مشاهده با موفقیت به یک لایه تصویری شفاف ادغام شدند."
            }

            (cmd.contains("حذف") || cmd.contains("پاک کن") || cmd.contains("پاک کردن لایه") || cmd.contains("delete")) &&
            !cmd.contains("سه بعدی") && !cmd.contains("3 بعدی") && !cmd.contains("سایه") &&
            !cmd.contains("استروک") && !cmd.contains("انحنا") && !cmd.contains("انعکاس") && !cmd.contains("کادر پشت") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "DELETE_SELECTED_LAYER")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                })
                replyFa = "لایه «${targetLayer?.name ?: "انتخاب‌شده"}» از روی بوم حذف گردید."
                suggestions.add("بازگشت (Undo) در صورت نیاز")
                suggestions.add("حذف همه لایه‌ها")
            }

            cmd.contains("کپی") || cmd.contains("تکثیر") || cmd.contains("duplicate") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "DUPLICATE_SELECTED_LAYER")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                })
                replyFa = "یک نسخه کپی از لایه «${targetLayer?.name ?: "انتخاب‌شده"}» روی بوم ایجاد شد."
            }

            cmd.contains("بیار جلو") || cmd.contains("بیاور جلو") || cmd.contains("رو بیاور") ||
            cmd.contains("بالاترین") || cmd.contains("روی همه") || cmd.contains("to front") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "REORDER_LAYER")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("orderDirection", "TO_FRONT")
                })
                replyFa = "لایه «${targetLayer?.name ?: "انتخاب‌شده"}» به بالاترین سطح (روی همه لایه‌ها) منتقل شد."
            }

            cmd.contains("ببر عقب") || cmd.contains("ببر پشت") || cmd.contains("پشت همه") ||
            cmd.contains("پایین ترین") || cmd.contains("زیر همه") || cmd.contains("to back") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "REORDER_LAYER")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("orderDirection", "TO_BACK")
                })
                replyFa = "لایه «${targetLayer?.name ?: "انتخاب‌شده"}» به پایین‌ترین سطح (پشت سایر لایه‌ها) منتقل شد."
            }

            cmd.contains("مخفی") || cmd.contains("پنهان") || cmd.contains("چشم") || cmd.contains("hide") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "TOGGLE_LAYER_VISIBILITY")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                })
                replyFa = "وضعیت نمایش لایه «${targetLayer?.name ?: "انتخاب‌شده"}» تغییر یافت."
            }

            cmd.contains("قفل") || cmd.contains("lock") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "TOGGLE_LAYER_LOCK")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                })
                replyFa = "وضعیت قفل لایه «${targetLayer?.name ?: "انتخاب‌شده"}» تغییر کرد."
            }

            // 5. Directional Movement, Alignment, Rotation, Scale & Opacity
            cmd.contains("وسط چین") || cmd.contains("مرکز بوم") || cmd.contains("وسط بوم") || cmd.contains("در مرکز") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "TRANSFORM_SELECTED_LAYER")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("x", 0.0)
                    put("y", 0.0)
                })
                replyFa = "لایه «${targetLayer?.name ?: "انتخاب‌شده"}» دقیقاً در مرکز افقی و عمودی بوم تراز شد."
            }

            cmd.contains("ببر بالا") || cmd.contains("سمت بالا") || cmd.contains("بالای بوم") || cmd.contains("کمی بالاتر") -> {
                val curY = targetLayer?.y ?: 0f
                val nextY = if (cmd.contains("بالای بوم")) -0.32 else (curY - 0.12f).coerceIn(-0.45f, 0.45f).toDouble()
                actions.put(JSONObject().apply {
                    put("actionType", "TRANSFORM_SELECTED_LAYER")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("y", nextY)
                })
                replyFa = "لایه «${targetLayer?.name ?: "انتخاب‌شده"}» به سمت بالای بوم منتقل شد."
            }

            cmd.contains("ببر پایین") || cmd.contains("سمت پایین") || cmd.contains("پایین بوم") || cmd.contains("کمی پایین") -> {
                val curY = targetLayer?.y ?: 0f
                val nextY = if (cmd.contains("پایین بوم")) 0.32 else (curY + 0.12f).coerceIn(-0.45f, 0.45f).toDouble()
                actions.put(JSONObject().apply {
                    put("actionType", "TRANSFORM_SELECTED_LAYER")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("y", nextY)
                })
                replyFa = "لایه «${targetLayer?.name ?: "انتخاب‌شده"}» به سمت پایین بوم منتقل شد."
            }

            cmd.contains("ببر راست") || cmd.contains("سمت راست") || cmd.contains("راست چین") -> {
                val curX = targetLayer?.x ?: 0f
                val nextX = if (cmd.contains("راست چین")) 0.22 else (curX + 0.12f).coerceIn(-0.45f, 0.45f).toDouble()
                actions.put(JSONObject().apply {
                    put("actionType", "TRANSFORM_SELECTED_LAYER")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("x", nextX)
                })
                replyFa = "لایه «${targetLayer?.name ?: "انتخاب‌شده"}» به سمت راست منتقل شد."
            }

            cmd.contains("ببر چپ") || cmd.contains("سمت چپ") || cmd.contains("چپ چین") -> {
                val curX = targetLayer?.x ?: 0f
                val nextX = if (cmd.contains("چپ چین")) -0.22 else (curX - 0.12f).coerceIn(-0.45f, 0.45f).toDouble()
                actions.put(JSONObject().apply {
                    put("actionType", "TRANSFORM_SELECTED_LAYER")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("x", nextX)
                })
                replyFa = "لایه «${targetLayer?.name ?: "انتخاب‌شده"}» به سمت چپ منتقل شد."
            }

            cmd.contains("بچرخون") || cmd.contains("چرخش") || cmd.contains("زاویه") || cmd.contains("چرخاندن") -> {
                val degMatch = Regex("(-?\\d+)").find(cmd)?.groupValues?.get(1)?.toDoubleOrNull()
                val targetAngle = when {
                    cmd.contains("صاف") || cmd.contains("صفر") || cmd.contains("بدون چرخش") -> 0.0
                    degMatch != null -> degMatch.coerceIn(-180.0, 180.0)
                    else -> ((targetLayer?.rotationZ ?: 0f) + 15f).toDouble()
                }
                actions.put(JSONObject().apply {
                    put("actionType", "TRANSFORM_SELECTED_LAYER")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("rotationZ", targetAngle)
                })
                replyFa = "زاویه چرخش لایه «${targetLayer?.name ?: "انتخاب‌شده"}» روی ${targetAngle.toInt()} درجه تنظیم شد."
            }

            cmd.contains("شفافیت") || cmd.contains("کمرنگ") || cmd.contains("پررنگ") || cmd.contains("opacity") -> {
                val numMatch = Regex("(\\d+)").find(cmd)?.groupValues?.get(1)?.toDoubleOrNull()
                val newOpacity = when {
                    numMatch != null -> (if (numMatch > 1.0) numMatch / 100.0 else numMatch).coerceIn(0.15, 1.0)
                    cmd.contains("کمرنگ") || cmd.contains("کم") || cmd.contains("نیمه شفاف") -> 0.55
                    else -> 1.0
                }
                actions.put(JSONObject().apply {
                    put("actionType", "TRANSFORM_SELECTED_LAYER")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("opacity", newOpacity)
                })
                replyFa = "شفافیت لایه «${targetLayer?.name ?: "انتخاب‌شده"}» روی ${(newOpacity * 100).toInt()}٪ تنظیم شد."
            }

            cmd.contains("بزرگتر") || cmd.contains("بزرگ تر") || cmd.contains("درشت تر") || cmd.contains("بزرگ کن") || cmd.contains("درشت کن") -> {
                if (targetLayer is TextLayer) {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_TEXT_BY_ID")
                        put("targetLayerId", targetLayer.id)
                        put("fontSizeSp", (targetLayer.fontSizeSp * 1.25f).coerceAtMost(120f).toDouble())
                    })
                } else if (targetLayer != null) {
                    actions.put(JSONObject().apply {
                        put("actionType", "TRANSFORM_SELECTED_LAYER")
                        put("targetLayerId", targetLayer.id)
                        put("scale", (targetLayer.scaleX * 1.25f).coerceAtMost(3.2f).toDouble())
                    })
                }
                replyFa = "اندازه لایه «${targetLayer?.name ?: "انتخاب‌شده"}» ۲۵٪ بزرگ‌تر شد."
            }

            cmd.contains("کوچکتر") || cmd.contains("کوچک تر") || cmd.contains("ریزتر") || cmd.contains("کوچک کن") || cmd.contains("ریز کن") -> {
                if (targetLayer is TextLayer) {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_TEXT_BY_ID")
                        put("targetLayerId", targetLayer.id)
                        put("fontSizeSp", (targetLayer.fontSizeSp * 0.8f).coerceAtLeast(16f).toDouble())
                    })
                } else if (targetLayer != null) {
                    actions.put(JSONObject().apply {
                        put("actionType", "TRANSFORM_SELECTED_LAYER")
                        put("targetLayerId", targetLayer.id)
                        put("scale", (targetLayer.scaleX * 0.8f).coerceAtLeast(0.25f).toDouble())
                    })
                }
                replyFa = "اندازه لایه «${targetLayer?.name ?: "انتخاب‌شده"}» ۲۰٪ کوچک‌تر شد."
            }

            // 6. Aspect Ratio / Canvas Dimensions
            cmd.contains("استوری") || cmd.contains("9:16") || cmd.contains("9 به 16") || cmd.contains("ریلز") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "SET_CANVAS_ASPECT")
                    put("aspectRatioLabel", "9:16")
                })
                replyFa = "ابعاد بوم طراحی بر اساس نسبت استاندارد استوری اینستاگرام و ریلز (۹:۱۶ - ۱۰۸۰×۱۹۲۰) تنظیم شد."
                critiqueFa = "نسبت عمودی ۹:۱۶ برای استوری اینستاگرام و ویدیوهای ریلز ایده‌آل است."
                suggestions.add("چیدمان هوشمند لایه‌ها")
                suggestions.add("پوستر تبلیغاتی حرفه‌ای با تیتر ۳بعدی و افکت نوری بساز")
            }

            cmd.contains("یوتیوب") || cmd.contains("تامبنیل") || cmd.contains("تامنیل") || cmd.contains("16:9") || cmd.contains("16 به 9") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "SET_CANVAS_ASPECT")
                    put("aspectRatioLabel", "16:9")
                })
                replyFa = "ابعاد بوم طراحی برای کاور و تامنیل ویدیوی یوتیوب (۱۶:۹ - ۱۹۲۰×۱۰۸۰) تنظیم شد."
                suggestions.add("طراحی کاور گیمینگ")
                suggestions.add("تاریک کردن حاشیه‌ها با افکت وینیت")
            }

            cmd.contains("پست") || cmd.contains("مربع") || cmd.contains("1:1") || cmd.contains("1 به 1") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "SET_CANVAS_ASPECT")
                    put("aspectRatioLabel", "1:1")
                })
                replyFa = "ابعاد بوم طراحی به نسبت مربعی استاندارد اینستاگرام (۱:۱ - ۱۰۸۰×۱۰۸۰) تغییر یافت."
                suggestions.add("وسط‌چین کردن عناصر اصلی")
            }

            cmd.contains("پرتره") || cmd.contains("4:5") || cmd.contains("4 به 5") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "SET_CANVAS_ASPECT")
                    put("aspectRatioLabel", "4:5")
                })
                replyFa = "ابعاد بوم طراحی به نسبت عمودی بهینه فید اینستاگرام (۴:۵ - ۱۰۸۰×۱۳۵۰) تنظیم شد."
            }

            cmd.contains("چیدمان") || cmd.contains("تراز") || cmd.contains("مرتب") ||
            cmd.contains("نسبت طلایی") || cmd.contains("layout") || cmd.contains("پیش نمایش") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "AUTO_LAYOUT_HARMONIZE")
                })
                replyFa = "پیش‌نمایش چیدمان هوشمند متقارن با نسبت طلایی و تراز بصری آماده شد. لطفاً تغییرات را روی بوم مشاهده و تایید نمایید."
                critiqueFa = "تمام لایه‌ها طبق قواعد یک‌سوم و کادربندی طلایی با فواصل استاندارد تراز شدند."
                suggestions.add("طرح فعلی بوم رو نقد بصری کن و با تایپوگرافی سه‌بعدی بهبود بده")
                suggestions.add("تغییر رنگ به طلایی لوکس")
            }

            // 7. Anti-Gravity & Zero-G Physics Simulation Agent
            cmd.contains("آنتی گروتی") || cmd.contains("آنتی گرویتی") || cmd.contains("ضد جاذبه") ||
            cmd.contains("بی وزنی") || cmd.contains("معلق") ||
            cmd.contains("فضا") || cmd.contains("شناور") || cmd.contains("anti-gravity") || cmd.contains("zero-g") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "TOGGLE_ANTI_GRAVITY")
                })
                if (targetLayer is TextLayer) {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_TEXT_BY_ID")
                        put("targetLayerId", targetLayer.id)
                        put("is3dEnabled", true)
                        put("depth3d", 22)
                        put("shadow3dEnabled", true)
                        put("reflectionEnabled", true)
                        put("y", -0.08)
                    })
                }
                replyFa = "ایجنت ضدجاذبه (Anti-Gravity Zero-G) با موفقیت فعال شد! اکنون لایه‌ها با فیزیک بی‌وزنی شناور هستند."
                suggestions.add("تنظیم زاویه و عمق متن سه‌بعدی")
                suggestions.add("اضافه کردن استیکر فضایی موشک")
            }

            // 8. Full Poster Templates from Scratch
            cmd.contains("سایبرپانک") || cmd.contains("cyberpunk") || cmd.contains("سایبر") -> {
                actions.put(JSONObject().apply { put("actionType", "CLEAR_ALL_LAYERS") })
                actions.put(JSONObject().apply {
                    put("actionType", "SET_CANVAS_BACKGROUND")
                    put("primaryColorHex", "#0B061A")
                    put("secondaryColorHex", "#1F0B42")
                    put("useGradient", true)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "ADD_SHAPE_LAYER")
                    put("shapeKind", "HEXAGON")
                    put("layerName", "المان سایبرپانک")
                    put("primaryColorHex", "#7000FF")
                    put("secondaryColorHex", "#00F0FF")
                    put("useGradient", true)
                    put("opacity", 0.35)
                    put("x", 0.0)
                    put("y", -0.05)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "ADD_TEXT_LAYER")
                    put("text", "CYBERPUNK 2077")
                    put("layerName", "تیتر سایبرپانک")
                    put("fontId", "lalezar")
                    put("fontSizeSp", 54.0)
                    put("primaryColorHex", "#00F0FF")
                    put("secondaryColorHex", "#FF007F")
                    put("useGradient", true)
                    put("is3dEnabled", true)
                    put("depth3d", 28)
                    put("strokeEnabled", true)
                    put("strokeColorHex", "#050515")
                    put("strokeWidth", 8.0)
                    put("shadowEnabled", true)
                    put("shadowColorHex", "#00F0FF")
                    put("shadowBlur", 24.0)
                    put("x", 0.0)
                    put("y", -0.06)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "ADD_TEXT_LAYER")
                    put("text", "آینده در دستان توست • نسخه نئونی")
                    put("layerName", "زیرنویس")
                    put("fontId", "vazirmatn")
                    put("fontSizeSp", 22.0)
                    put("primaryColorHex", "#FFFFFF")
                    put("backgroundPillEnabled", true)
                    put("backgroundPillColorHex", "#CC16082F")
                    put("x", 0.0)
                    put("y", 0.22)
                })
                actions.put(JSONObject().apply {
                    put("actionType", "SET_POST_FX")
                    put("vignetteEnabled", true)
                    put("noiseEnabled", true)
                })
                newTitle = "پوستر سایبرپانک نئونی"
                replyFa = "طرح پوستر سایبرپانک مدرن با تایپوگرافی ۳بعدی نئونی، پس‌زمینه کهکشانی و افکت وینیت ساخته شد."
            }

            cmd.contains("طلا") || cmd.contains("طلایی") || cmd.contains("لوکس") || cmd.contains("gold") -> {
                if (targetLayer is TextLayer && !cmd.contains("پوستر")) {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_TEXT_BY_ID")
                        put("targetLayerId", targetLayer.id)
                        put("primaryColorHex", "#FFD700")
                        put("secondaryColorHex", "#B45309")
                        put("useGradient", true)
                        put("is3dEnabled", true)
                        put("depth3d", 26)
                        put("embossEnabled", true)
                        put("strokeEnabled", true)
                        put("strokeColorHex", "#78350F")
                        put("strokeWidth", 6.0)
                        put("shadowEnabled", true)
                        put("shadowColorHex", "#D97706")
                        put("shadowBlur", 18.0)
                        put("texturePreset", "GOLD_FOIL")
                    })
                    replyFa = "استایل طلایی لوکس ۳بعدی متالیک به همراه برجسته‌سازی (Emboss) و سایه روی لایه «${targetLayer.name}» اعمال شد."
                } else {
                    actions.put(JSONObject().apply { put("actionType", "CLEAR_ALL_LAYERS") })
                    actions.put(JSONObject().apply {
                        put("actionType", "SET_CANVAS_BACKGROUND")
                        put("primaryColorHex", "#0A0A0A")
                        put("secondaryColorHex", "#1A1710")
                        put("useGradient", true)
                    })
                    actions.put(JSONObject().apply {
                        put("actionType", "ADD_TEXT_LAYER")
                        put("text", "طراحی لوکس طلایی")
                        put("fontId", "lalezar")
                        put("fontSizeSp", 56.0)
                        put("primaryColorHex", "#FCD34D")
                        put("secondaryColorHex", "#B45309")
                        put("useGradient", true)
                        put("is3dEnabled", true)
                        put("depth3d", 26)
                        put("embossEnabled", true)
                        put("strokeEnabled", true)
                        put("strokeColorHex", "#451A03")
                        put("strokeWidth", 6.0)
                        put("shadowEnabled", true)
                        put("shadowColorHex", "#F59E0B")
                        put("shadowBlur", 20.0)
                        put("x", 0.0)
                        put("y", -0.05)
                    })
                    actions.put(JSONObject().apply {
                        put("actionType", "ADD_BUILTIN_STICKER")
                        put("stickerId", "crown_gold")
                        put("x", 0.0)
                        put("y", -0.22)
                    })
                    newTitle = "پوستر طلایی لوکس"
                    replyFa = "پوستر طلایی لوکس سلطنتی با تایپوگرافی سه‌بعدی و استیکر تاج طلایی ایجاد شد."
                }
            }

            cmd.contains("نئون") || cmd.contains("neon") || cmd.contains("تابلو نئون") -> {
                if (targetLayer is TextLayer && !cmd.contains("پوستر")) {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_TEXT_BY_ID")
                        put("targetLayerId", targetLayer.id)
                        put("primaryColorHex", "#FFFFFF")
                        put("strokeEnabled", true)
                        put("strokeColorHex", "#F43F5E")
                        put("strokeWidth", 8.0)
                        put("shadowEnabled", true)
                        put("shadowColorHex", "#F43F5E")
                        put("shadowBlur", 30.0)
                    })
                    replyFa = "افکت تابلو نئون با درخشش بیرونی قرمز-سرخابی و هسته سفید درخشان روی لایه «${targetLayer.name}» اعمال شد."
                } else {
                    actions.put(JSONObject().apply {
                        put("actionType", "SET_CANVAS_BACKGROUND")
                        put("primaryColorHex", "#05050A")
                        put("secondaryColorHex", "#120B24")
                        put("useGradient", true)
                    })
                    actions.put(JSONObject().apply {
                        put("actionType", "ADD_TEXT_LAYER")
                        put("text", "NEON NIGHTS")
                        put("fontId", "lalezar")
                        put("fontSizeSp", 52.0)
                        put("primaryColorHex", "#FFFFFF")
                        put("strokeEnabled", true)
                        put("strokeColorHex", "#06B6D4")
                        put("strokeWidth", 7.0)
                        put("shadowEnabled", true)
                        put("shadowColorHex", "#06B6D4")
                        put("shadowBlur", 32.0)
                        put("x", 0.0)
                        put("y", 0.0)
                    })
                    replyFa = "طرح نئونی با درخشش فیروزه‌ای و پس‌زمینه شب ایجاد شد."
                }
            }

            // 9. 3D Text / Curve / Shadow / Stroke / Background Pill Modifiers
            cmd.contains("حذف سه بعدی") || cmd.contains("بدون سه بعدی") || cmd.contains("دوبعدی") || cmd.contains("2 بعدی") || cmd.contains("2d") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "UPDATE_SELECTED_TEXT")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("is3dEnabled", false)
                })
                replyFa = "افکت سه‌بعدی لایه غیرفعال شد و به حالت دوبعدی صاف برگشت."
            }

            cmd.contains("سه بعدی") || cmd.contains("3 بعدی") || cmd.contains("3d") || cmd.contains("عمق") -> {
                val depthMatch = Regex("(\\d+)").find(cmd.replace("3 بعدی", ""))?.groupValues?.get(1)?.toIntOrNull()
                val depthVal = depthMatch?.coerceIn(4, 50) ?: 24
                actions.put(JSONObject().apply {
                    put("actionType", "UPDATE_SELECTED_TEXT")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("is3dEnabled", true)
                    put("depth3d", depthVal)
                    put("obliqueAngle3d", 45.0)
                    put("strokeEnabled", true)
                    put("strokeWidth", 5.0)
                    put("shadowEnabled", true)
                    put("shadowBlur", 18.0)
                    put("embossEnabled", true)
                })
                replyFa = "حالت سه‌بعدی با عمق $depthVal، زاویه ۴۵ درجه، برجسته‌سازی متالیک و سایه روی لایه اعمال شد."
            }

            cmd.contains("انحنا") || cmd.contains("خمیده") || cmd.contains("دایره") || cmd.contains("curve") -> {
                val angle = if (cmd.contains("صاف") || cmd.contains("بدون") || cmd.contains("حذف")) 0.0 else 75.0
                actions.put(JSONObject().apply {
                    put("actionType", "UPDATE_SELECTED_TEXT")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("curveAngle", angle)
                })
                replyFa = if (angle == 0.0) "انحنای متن حذف و به خط صاف تبدیل شد." else "انحنای دایره‌ای ۷۵ درجه بر روی متن انتخاب‌شده اعمال شد."
            }

            cmd.contains("انعکاس") || cmd.contains("reflection") || cmd.contains("آینه") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "UPDATE_SELECTED_TEXT")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("reflectionEnabled", !cmd.contains("حذف") && !cmd.contains("بدون"))
                })
                replyFa = "تنظیمات افکت انعکاس زمینی (Reflection) روی لایه اعمال شد."
            }

            cmd.contains("کادر پشت") || cmd.contains("پشت متن") || cmd.contains("هایلایت") || cmd.contains("pill") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "UPDATE_SELECTED_TEXT")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("backgroundPillEnabled", !cmd.contains("حذف") && !cmd.contains("بدون"))
                    put("backgroundPillColorHex", "#CC0F172A")
                })
                replyFa = "کادر پس‌زمینه متن (Background Pill) روی لایه تنظیم شد."
            }

            cmd.contains("سایه") || cmd.contains("shadow") || cmd.contains("درخشش") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "UPDATE_SELECTED_TEXT")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("shadowEnabled", !cmd.contains("حذف") && !cmd.contains("بدون"))
                    put("shadowBlur", 22.0)
                    put("shadowColorHex", "#000000")
                })
                replyFa = "سایه بیرونی با شعاع ۲۲ روی لایه تنظیم شد."
            }

            cmd.contains("استروک") || cmd.contains("دورخط") || cmd.contains("stroke") || cmd.contains("حاشیه") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "UPDATE_SELECTED_TEXT")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("strokeEnabled", !cmd.contains("حذف") && !cmd.contains("بدون"))
                    put("strokeWidth", 6.0)
                    put("strokeColorHex", "#111827")
                })
                replyFa = "دورخط (Stroke) به ضخامت ۶ روی لایه تنظیم شد."
            }

            // 10. Color changes (supports background vs selected layer)
            cmd.contains("قرمز") -> {
                if (cmd.contains("پس زمینه") || cmd.contains("بوم") || cmd.contains("بکگراند")) {
                    actions.put(JSONObject().apply {
                        put("actionType", "SET_CANVAS_BACKGROUND")
                        put("primaryColorHex", "#991B1B")
                        put("secondaryColorHex", "#450A0A")
                        put("useGradient", true)
                    })
                    replyFa = "پس‌زمینه بوم به گرادیان قرمز یاقوتی تغییر یافت."
                } else {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_SELECTED_TEXT")
                        targetLayer?.id?.let { put("targetLayerId", it) }
                        put("primaryColorHex", "#EF4444")
                        put("secondaryColorHex", "#991B1B")
                    })
                    replyFa = "رنگ لایه به قرمز پررنگ تغییر یافت."
                }
            }

            cmd.contains("آبی") || cmd.contains("فیروزه") -> {
                if (cmd.contains("پس زمینه") || cmd.contains("بوم") || cmd.contains("بکگراند")) {
                    actions.put(JSONObject().apply {
                        put("actionType", "SET_CANVAS_BACKGROUND")
                        put("primaryColorHex", "#0F172A")
                        put("secondaryColorHex", "#0284C7")
                        put("useGradient", true)
                    })
                    replyFa = "پس‌زمینه بوم به گرادیان آبی اقیانوسی تغییر یافت."
                } else {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_SELECTED_TEXT")
                        targetLayer?.id?.let { put("targetLayerId", it) }
                        put("primaryColorHex", "#38BDF8")
                        put("secondaryColorHex", "#0369A1")
                    })
                    replyFa = "رنگ لایه به آبی اقیانوسی تنظیم شد."
                }
            }

            cmd.contains("سبز") || cmd.contains("زمردی") -> {
                if (cmd.contains("پس زمینه") || cmd.contains("بوم") || cmd.contains("بکگراند")) {
                    actions.put(JSONObject().apply {
                        put("actionType", "SET_CANVAS_BACKGROUND")
                        put("primaryColorHex", "#064E3B")
                        put("secondaryColorHex", "#022C22")
                        put("useGradient", true)
                    })
                    replyFa = "پس‌زمینه بوم به گرادیان سبز زمردی تیره تغییر یافت."
                } else {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_SELECTED_TEXT")
                        targetLayer?.id?.let { put("targetLayerId", it) }
                        put("primaryColorHex", "#22C55E")
                        put("secondaryColorHex", "#15803D")
                    })
                    replyFa = "رنگ لایه به سبز نئونی تغییر یافت."
                }
            }

            cmd.contains("زرد") || cmd.contains("نارنجی") -> {
                if (cmd.contains("پس زمینه") || cmd.contains("بوم") || cmd.contains("بکگراند")) {
                    actions.put(JSONObject().apply {
                        put("actionType", "SET_CANVAS_BACKGROUND")
                        put("primaryColorHex", "#FFEE00")
                        put("secondaryColorHex", "#D7B200")
                        put("useGradient", true)
                    })
                    replyFa = "پس‌زمینه بوم به گرادیان زرد طلایی تغییر یافت."
                } else {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_SELECTED_TEXT")
                        targetLayer?.id?.let { put("targetLayerId", it) }
                        put("primaryColorHex", "#FACC15")
                        put("secondaryColorHex", "#CA8A04")
                    })
                    replyFa = "رنگ لایه به زرد درخشان تغییر یافت."
                }
            }

            cmd.contains("بنفش") || cmd.contains("یاسی") -> {
                if (cmd.contains("پس زمینه") || cmd.contains("بوم") || cmd.contains("بکگراند")) {
                    actions.put(JSONObject().apply {
                        put("actionType", "SET_CANVAS_BACKGROUND")
                        put("primaryColorHex", "#2E1065")
                        put("secondaryColorHex", "#090D16")
                        put("useGradient", true)
                    })
                    replyFa = "پس‌زمینه بوم به گرادیان بنفش کهکشانی تغییر یافت."
                } else {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_SELECTED_TEXT")
                        targetLayer?.id?.let { put("targetLayerId", it) }
                        put("primaryColorHex", "#A855F7")
                        put("secondaryColorHex", "#6B21A8")
                    })
                    replyFa = "رنگ لایه به بنفش سلطنتی تغییر یافت."
                }
            }

            cmd.contains("سفید") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "UPDATE_SELECTED_TEXT")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("primaryColorHex", "#FFFFFF")
                    put("useGradient", false)
                })
                replyFa = "رنگ لایه به سفید خالص تنظیم شد."
            }

            cmd.contains("سیاه") || cmd.contains("مشکی") || cmd.contains("تیره") -> {
                if (cmd.contains("پس زمینه") || cmd.contains("بوم") || cmd.contains("بکگراند")) {
                    actions.put(JSONObject().apply {
                        put("actionType", "SET_CANVAS_BACKGROUND")
                        put("primaryColorHex", "#090D16")
                        put("secondaryColorHex", "#111827")
                        put("useGradient", true)
                    })
                    replyFa = "پس‌زمینه بوم به مشکی استودیویی تغییر یافت."
                } else {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_SELECTED_TEXT")
                        targetLayer?.id?.let { put("targetLayerId", it) }
                        put("primaryColorHex", "#111827")
                        put("useGradient", false)
                    })
                    replyFa = "رنگ لایه به مشکی تیره تغییر یافت."
                }
            }

            // 11. Fonts
            cmd.contains("لاله زار") || cmd.contains("lalezar") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "UPDATE_SELECTED_TEXT")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("fontId", "lalezar")
                })
                replyFa = "فونت لاله‌زار (تیتر درشت) بر روی لایه متن اعمال شد."
            }

            cmd.contains("وزیر") || cmd.contains("vazir") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "UPDATE_SELECTED_TEXT")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("fontId", "vazirmatn")
                })
                replyFa = "فونت وزیرمتن (خوانا و رسمی) برای لایه انتخاب شد."
            }

            cmd.contains("امیری") || cmd.contains("amiri") -> {
                actions.put(JSONObject().apply {
                    put("actionType", "UPDATE_SELECTED_TEXT")
                    targetLayer?.id?.let { put("targetLayerId", it) }
                    put("fontId", "amiri")
                })
                replyFa = "فونت اصیل و سنتی امیری روی متن اعمال شد."
            }

            // 12. Stickers & Badges
            cmd.contains("استیکر") || cmd.contains("تاج") || cmd.contains("موشک") || cmd.contains("الماس") ||
            cmd.contains("آتش") || cmd.contains("قلب") || cmd.contains("تیک") -> {
                val stickerId = when {
                    cmd.contains("تاج") -> "crown_gold"
                    cmd.contains("موشک") -> "rocket"
                    cmd.contains("آتش") -> "fire"
                    cmd.contains("الماس") -> "diamond"
                    cmd.contains("تیک") -> "verified"
                    cmd.contains("قلب") -> "heart"
                    else -> "badge_star"
                }
                actions.put(JSONObject().apply {
                    put("actionType", "ADD_BUILTIN_STICKER")
                    put("stickerId", stickerId)
                    put("x", 0.0)
                    put("y", -0.15)
                })
                replyFa = "استیکر و نشان برداری گرافیکی به صفحه اضافه شد."
            }

            // 13. Quotes & Motivational
            cmd.contains("جمله") || cmd.contains("انگیزشی") || cmd.contains("سخن") || cmd.contains("نقل") -> {
                val quotes = listOf(
                    "موفقیت یعنی حرکت از شکستی به شکست دیگر، بدون از دست دادن اشتیاق.",
                    "بزرگ‌ترین رویاهایت را بساز، آینده متعلق به کسانی است که به زیبایی رویاهایشان باور دارند.",
                    "شروع کن، حتی اگر کامل نیستی؛ در مسیر تکامل خواهی یافت.",
                    "امروز همان فردایی است که دیروز نگرانش بودی."
                )
                val chosenQuote = quotes.random()
                actions.put(JSONObject().apply {
                    put("actionType", "APPLY_QUOTE")
                    put("quoteText", chosenQuote)
                    put("fontId", "vazirmatn")
                    put("fontSizeSp", 26.0)
                })
                replyFa = "جمله انگیزشی زیبا با استایل خوش‌نویسی به بوم اضافه شد."
            }

            // 14. Explicit Text Creation or Replacement
            cmd.startsWith("بنویس ") || cmd.startsWith("متن ") || cmd.startsWith("تیتر ") ||
            cmd.contains("متن جدید") || cmd.contains("افزودن متن") -> {
                val extractedText = when {
                    raw.contains(":") -> raw.substringAfter(":").trim()
                    cmd.startsWith("بنویس ") || cmd.startsWith("متن ") || cmd.startsWith("تیتر ") -> raw.substringAfter(" ").trim()
                    else -> raw
                }.ifBlank { "متن جدید" }
                val isExplicitAdd = cmd.contains("جدید") || cmd.contains("اضافه") || cmd.contains("افزودن")
                if (!isExplicitAdd && targetLayer is TextLayer && (cmd.contains("تغییر") || cmd.contains("ویرایش"))) {
                    actions.put(JSONObject().apply {
                        put("actionType", "UPDATE_TEXT_BY_ID")
                        put("targetLayerId", targetLayer.id)
                        put("text", extractedText)
                    })
                    replyFa = "متن لایه انتخاب‌شده به «$extractedText» تغییر یافت."
                } else {
                    actions.put(JSONObject().apply {
                        put("actionType", "ADD_TEXT_LAYER")
                        put("text", extractedText)
                        put("fontId", "lalezar")
                        put("fontSizeSp", 46.0)
                        put("primaryColorHex", "#FFFFFF")
                        put("strokeEnabled", true)
                        put("strokeColorHex", "#0F172A")
                        put("strokeWidth", 5.0)
                        put("shadowEnabled", true)
                        put("x", 0.0)
                        put("y", 0.0)
                    })
                    replyFa = "لایه متن جدید «$extractedText» به بوم اضافه شد."
                }
            }

            // 15. Smart Fallback: Never overwrite existing layer text with a design instruction!
            else -> {
                if (isLikelyDesignInstruction(cmd)) {
                    if (targetLayer is TextLayer) {
                        actions.put(JSONObject().apply {
                            put("actionType", "UPDATE_TEXT_BY_ID")
                            put("targetLayerId", targetLayer.id)
                            // Strictly preserve targetLayer.text
                            put("is3dEnabled", true)
                            put("depth3d", 24)
                            put("strokeEnabled", true)
                            put("strokeColorHex", "#090D16")
                            put("strokeWidth", 6.0)
                            put("shadowEnabled", true)
                            put("shadowBlur", 22.0)
                            put("embossEnabled", true)
                        })
                        actions.put(JSONObject().apply {
                            put("actionType", "SET_POST_FX")
                            put("vignetteEnabled", true)
                        })
                        replyFa = "استایل سه‌بعدی، سایه‌زنی استودیویی و افکت نوری بدون تغییر متن روی لایه «${targetLayer.name}» اعمال شد."
                    } else if (project.layers.isNotEmpty()) {
                        actions.put(JSONObject().apply {
                            put("actionType", "AUTO_LAYOUT_HARMONIZE")
                        })
                        replyFa = "بهینه‌سازی هوشمند چیدمان و کنتراست لایه‌ها بر اساس دستور شما انجام شد."
                    } else {
                        actions.put(JSONObject().apply {
                            put("actionType", "SET_CANVAS_BACKGROUND")
                            put("primaryColorHex", "#0B0F19")
                            put("secondaryColorHex", "#1E1B4B")
                            put("useGradient", true)
                        })
                        actions.put(JSONObject().apply {
                            put("actionType", "ADD_TEXT_LAYER")
                            put("text", "طراحی حرفه‌ای پیکسل‌لب")
                            put("fontId", "lalezar")
                            put("fontSizeSp", 52.0)
                            put("primaryColorHex", "#38BDF8")
                            put("secondaryColorHex", "#A855F7")
                            put("useGradient", true)
                            put("is3dEnabled", true)
                            put("depth3d", 24)
                            put("strokeEnabled", true)
                            put("strokeColorHex", "#090D16")
                            put("strokeWidth", 6.0)
                            put("shadowEnabled", true)
                            put("x", 0.0)
                            put("y", 0.0)
                        })
                        replyFa = "طرح پایه با تیتر سه‌بعدی و پس‌زمینه گرادیان روی بوم ایجاد شد."
                    }
                } else {
                    // Short literal phrase (not a design instruction)
                    if (targetLayer is TextLayer) {
                        actions.put(JSONObject().apply {
                            put("actionType", "UPDATE_TEXT_BY_ID")
                            put("targetLayerId", targetLayer.id)
                            put("text", raw)
                        })
                        replyFa = "متن لایه انتخاب‌شده به «${raw.take(28)}» به‌روزرسانی شد."
                    } else {
                        actions.put(JSONObject().apply {
                            put("actionType", "ADD_TEXT_LAYER")
                            put("text", raw.take(40))
                            put("fontId", "lalezar")
                            put("fontSizeSp", 46.0)
                            put("primaryColorHex", "#F8FAFC")
                            put("strokeEnabled", true)
                            put("strokeColorHex", "#090D16")
                            put("strokeWidth", 6.0)
                            put("shadowEnabled", true)
                            put("x", 0.0)
                            put("y", 0.0)
                        })
                        replyFa = "لایه متن «${raw.take(28)}» به مرکز بوم اضافه شد."
                    }
                }
            }
        }

        return AgentInferenceResult(
            replyFa = replyFa,
            designCritiqueFa = critiqueFa,
            followUpSuggestionsFa = suggestions.ifEmpty {
                listOf("✨ نقد بصری طرح", "📐 چیدمان هوشمند طلایی", "تغییر استایل به طلایی لوکس")
            },
            projectTitle = newTitle,
            rawActionsJson = actions,
            latencyMs = 15L,
            usedCanvasVision = false,
            modelName = "Gemini Adaptive Engine"
        )
    }

    /**
     * Generates a harmonized, golden-ratio auto-layout with visual hierarchy,
     * calculated spacing, and neon 3D typography styling.
     */
    fun generateHarmonicAutoLayout(project: StudioProject): com.example.data.AgentLayoutSuggestion {
        if (project.layers.isEmpty()) {
            val titleLayer = TextLayer(
                name = "تیتر اصلی",
                text = "پیکسل‌لب استودیو",
                fontId = com.example.data.StudioFont.LALEZAR.id,
                fontSizeSp = 46f,
                colorHex = "#00F0FF",
                gradientSecondColorHex = "#A855F7",
                useGradientFill = true,
                is3dEnabled = true,
                depth3d = 20,
                shadow3dEnabled = true,
                reflectionEnabled = true,
                shadow = com.example.data.LayerShadow(enabled = true, colorHex = "#8000F0FF", blurRadius = 24f),
                x = 0.0f,
                y = -0.16f
            )
            val subLayer = TextLayer(
                name = "زیرتیتر توضیحات",
                text = "موتور گرافیکی هوشمند با چیدمان بر اساس نسبت طلایی",
                fontId = com.example.data.StudioFont.VAZIRMATN.id,
                fontSizeSp = 22f,
                colorHex = "#E2E8F0",
                shadow = com.example.data.LayerShadow(enabled = true, colorHex = "#AA000000", blurRadius = 12f),
                x = 0.0f,
                y = 0.08f
            )
            val badgeLayer = ShapeLayer(
                name = "کادر نشان طلایی",
                shapeKind = ShapeKind.HEXAGON,
                fillColorHex = "#7000FF",
                gradientSecondColorHex = "#00F0FF",
                useGradientFill = true,
                opacity = 0.35f,
                scaleX = 1.35f,
                scaleY = 1.35f,
                x = 0.0f,
                y = -0.04f
            )
            val suggested = project.copy(
                background = CanvasBackground(
                    type = BackgroundType.GRADIENT,
                    primaryColorHex = "#080612",
                    secondaryColorHex = "#180A2E"
                ),
                layers = listOf(badgeLayer, titleLayer, subLayer)
            )
            return com.example.data.AgentLayoutSuggestion(
                titleFa = "چیدمان پوستر نئونی طلایی",
                descriptionFa = "ترکیب‌بندی عمودی متوازن با تیتر سه‌بعدی برجسته، نشان هندسی و گرادیان تاریک",
                designHealthScore = 98,
                balanceTagFa = "کادربندی نسبت طلایی",
                suggestedProject = suggested,
                visualCritique = "تعادل میان المان هندسی پس‌زمینه و تایپوگرافی سه‌بعدی به خوانایی و جلوه مدرن پوستر کمک شایانی کرده است."
            )
        }

        val sortedLayers = project.layers.toMutableList()
        val textLayers = sortedLayers.filterIsInstance<TextLayer>()
        val shapeLayers = sortedLayers.filterIsInstance<ShapeLayer>()
        val imageLayers = sortedLayers.filterIsInstance<ImageLayer>()
        val otherLayers: List<StudioLayer> = sortedLayers.filter { it !is TextLayer && it !is ShapeLayer && it !is ImageLayer }

        val newLayers = mutableListOf<StudioLayer>()

        shapeLayers.forEach { shape ->
            newLayers.add(shape.copy(
                x = shape.x * 0.5f,
                y = shape.y,
                isLocked = false
            ))
        }

        imageLayers.forEach { img ->
            newLayers.add(img.copy(
                isLocked = false
            ))
        }

        otherLayers.forEach { other ->
            newLayers.add(other)
        }

        val count = textLayers.size
        val yStart = if (count == 1) 0.0f else -0.22f
        val ySpacing = if (count > 1) 0.44f / (count - 1).coerceAtLeast(1) else 0f

        textLayers.forEachIndexed { idx, text ->
            val isHeader = idx == 0
            val targetY = if (count <= 3) {
                if (count == 1) 0.0f else (yStart + idx * ySpacing).coerceIn(-0.38f, 0.38f)
            } else {
                text.y
            }
            newLayers.add(text.copy(
                x = if (count <= 3) 0.0f else text.x,
                y = targetY,
                isLocked = false,
                shadow = text.shadow.copy(
                    enabled = true,
                    blurRadius = if (isHeader) 22f else 12f
                )
            ))
        }

        val suggested = project.copy(
            background = if (project.background.type == BackgroundType.COLOR && project.background.primaryColorHex == "#FFFFFF") {
                CanvasBackground(
                    type = BackgroundType.GRADIENT,
                    primaryColorHex = "#0A0A12",
                    secondaryColorHex = "#161626"
                )
            } else project.background,
            layers = newLayers
        )

        return com.example.data.AgentLayoutSuggestion(
            titleFa = "چیدمان هوشمند متقارن و نسبت طلایی",
            descriptionFa = "تراز عناصر، باز کردن قفل لایه‌ها و تقویت کنتراست بصری تیترها",
            designHealthScore = 96,
            balanceTagFa = "چیدمان متعادل و متقارن",
            suggestedProject = suggested,
            visualCritique = "تمام لایه‌های متنی با تراز استاندارد بازآرایی شدند و سلسله‌مراتب بصری تیترها تقویت گردید."
        )
    }
}

package com.example.ui.agent

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.StudioProject
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import java.util.UUID

/**
 * Manages Gemini API interactions, secure key storage, model selection,
 * and the multi-turn Function Calling conversation loop with [GeminiCanvasAdapter].
 */
class GeminiServiceManager(
    private val context: Context?
) {

    companion object {
        private const val TAG = "GeminiServiceManager"
        private const val PREFS_NAME = "pixellab_agent_prefs"
        private const val KEY_CUSTOM_GEMINI_API_KEY = "custom_gemini_api_key"
        private const val KEY_SELECTED_GEMINI_MODEL = "selected_gemini_model"
        const val DEFAULT_MODEL = "gemini-3.8-flash"

        fun getSelectedModel(context: Context?): String {
            if (context == null) return DEFAULT_MODEL
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_SELECTED_GEMINI_MODEL, DEFAULT_MODEL)?.trim().orEmpty()
            return if (saved.isBlank()) DEFAULT_MODEL else saved
        }

        fun setSelectedModel(context: Context, modelName: String) {
            val clean = modelName.trim().ifBlank { DEFAULT_MODEL }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SELECTED_GEMINI_MODEL, clean).apply()
        }

        fun getCustomApiKey(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_CUSTOM_GEMINI_API_KEY, "")?.trim().orEmpty()
        }

        fun setCustomApiKey(context: Context, apiKey: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CUSTOM_GEMINI_API_KEY, apiKey.trim()).apply()
        }

        fun isSecureKeyConfigured(context: Context): Boolean {
            return getCustomApiKey(context).isNotBlank()
        }
    }

    val activeModelName: String
        get() = getSelectedModel(context)

    /**
     * Executes an end-to-end multi-turn Function Calling transaction with the given [canvasAdapter].
     */
    suspend fun executeTransaction(
        userCommand: String,
        canvasAdapter: GeminiCanvasAdapter,
        recentHistory: List<AgentChatMessage> = emptyList(),
        canvasVisionBitmap: Bitmap? = null,
        activeLearnedStyle: PlpLearnedStyleDNA? = null,
        onStepProgress: ((String) -> Unit)? = null
    ): AgentTransactionExecutionOutcome {
        val startTime = System.currentTimeMillis()
        val session = canvasAdapter.session
        val selectedModel = activeModelName

        // 1. Initial inspection step
        onStepProgress?.invoke("گام ۱/۴: واکاوی ساختار و لایه‌های فعلی بوم (inspect_document)...")
        canvasAdapter.executeTool(
            toolName = "inspect_document",
            args = mapOf("includeLayerSummary" to kotlinx.serialization.json.JsonPrimitive(true))
        )

        val canvasStateJson = PixelLabAgentEngine.serializeCanvasState(session.workingProject, session.workingSelectedLayerId)
        val systemPrompt = PixelLabAgentEngine.buildFunctionCallingSystemPrompt(
            canvasStateJson = canvasStateJson,
            recentHistory = recentHistory,
            activeLearnedStyle = activeLearnedStyle
        )

        var usedModelName = selectedModel
        val usedVision = canvasVisionBitmap != null
        var geminiReplyText: String? = null
        var geminiErrorFa: String? = null
        var cloudToolCallsCount = 0

        if (context != null) {
            // Strictly honor the user's selected model with retry on transient errors (never lock onto 3.5)
            val maxAttempts = 2
            for (attempt in 1..maxAttempts) {
                try {
                    onStepProgress?.invoke(
                        if (attempt == 1) {
                            "گام ۲/۴: اتصال به مدل $selectedModel با پشتیبانی از Function Calling..."
                        } else {
                            "گام ۲/۴: تلاش مجدد اتصال به مدل انتخابی $selectedModel (تلاش $attempt)..."
                        }
                    )

                    val generativeModel = Firebase.ai.generativeModel(
                        modelName = selectedModel,
                        tools = listOf(canvasAdapter.toolBundle),
                        systemInstruction = content { text(systemPrompt) }
                    )

                    val chat = generativeModel.startChat()
                    var currentResponse = withTimeout(45_000L) {
                        if (canvasVisionBitmap != null) {
                            chat.sendMessage(
                                content {
                                    image(canvasVisionBitmap)
                                    text(userCommand)
                                }
                            )
                        } else {
                            chat.sendMessage(userCommand)
                        }
                    }

                    var round = 0
                    while (currentResponse.functionCalls.isNotEmpty() && round < 8) {
                        round++
                        val calls = currentResponse.functionCalls
                        val responseParts = mutableListOf<FunctionResponsePart>()

                        for (call in calls) {
                            onStepProgress?.invoke("گام ۳/۴: اجرای ابزار «${call.name}» روی مدل $selectedModel...")
                            val toolOutput = canvasAdapter.executeTool(
                                toolName = call.name,
                                args = call.args
                            )
                            cloudToolCallsCount++
                            responseParts.add(FunctionResponsePart(call.name, toolOutput))
                        }

                        currentResponse = withTimeout(35_000L) {
                            chat.sendMessage(
                                content("user") {
                                    responseParts.forEach { part(it) }
                                }
                            )
                        }
                    }

                    usedModelName = selectedModel
                    geminiReplyText = currentResponse.text?.trim()
                    geminiErrorFa = null
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (err: Exception) {
                    val (catFa, det) = PixelLabAgentEngine.diagnoseError(err)
                    Log.w(TAG, "Model $selectedModel attempt $attempt warning: $catFa | $det", err)
                    AgentDiagnosticsLogger.warn(
                        tag = TAG,
                        message = "Model $selectedModel attempt $attempt issue: $catFa",
                        details = det
                    )
                    geminiErrorFa = "$catFa ($selectedModel)"
                }
            }
        }

        // Local deterministic fallback planner if offline or in Robolectric test environment
        val legacyActions = JSONArray()
        if (cloudToolCallsCount == 0 && !session.wasRolledBack) {
            onStepProgress?.invoke("گام ۳/۴: اجرای ابزارهای واقعی ویرایشگر از طریق آداپتور...")
            val localPlan = PixelLabAgentEngine.executeLocalRealToolsPlan(
                userCommand = userCommand,
                session = session,
                activeLearnedStyle = activeLearnedStyle
            )
            for (i in 0 until localPlan.rawActionsJson.length()) {
                legacyActions.put(localPlan.rawActionsJson.optJSONObject(i))
            }
            if (geminiReplyText.isNullOrBlank()) {
                geminiReplyText = localPlan.replyFa
            }
        }

        // Guarantee render_preview and validate_document
        if (!session.wasRolledBack) {
            if (session.steps.none { it.toolName == "render_preview" }) {
                onStepProgress?.invoke("گام ۴/۴: رندر پیش‌نمایش زنده بوم (render_preview)...")
                canvasAdapter.renderPreview(scaleMultiplier = 0.5f)
            }
            if (session.steps.none { it.toolName == "validate_document" }) {
                onStepProgress?.invoke("گام ۴/۴: اعتبارسنجی چیدمان و کنتراست (validate_document)...")
                canvasAdapter.validateDocument(autoFix = true)
            }
        }

        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
        val validationSummary = session.lastValidationReport?.summaryFa
        val finalReply = geminiReplyText?.takeIf { it.isNotBlank() }
            ?: "عملیات ویرایش بوم با موفقیت از طریق لایه واسط اعمال و اعتبارسنجی شد."

        return AgentTransactionExecutionOutcome(
            session = session,
            replyFa = finalReply,
            designCritiqueFa = validationSummary,
            followUpSuggestionsFa = listOf(
                "چیدمان لایه‌ها رو وسط‌چین و استاندارد کن",
                "تیتر اصلی رو طلایی سه‌بعدی با عمق ۲۸ کن",
                "یک پوستر تبلیغاتی ویژه تخفیف طراحی کن"
            ),
            modelName = usedModelName,
            latencyMs = latency,
            usedCanvasVision = usedVision,
            geminiErrorFa = geminiErrorFa,
            legacyActionsJson = legacyActions
        )
    }
}

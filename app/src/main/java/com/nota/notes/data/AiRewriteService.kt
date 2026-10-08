package com.nota.notes.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// إعادة الكتابة وتحسين التنسيق بالذكاء الاصطناعي (27 سبتمبر 2026):
// مفتاح المستخدم نفسه (BYOK) — يُخزَّن مشفرًا بـ TokenVault ولا يغادر الجهاز إلا لخادم المستخدم.
// متوافق مع أي خادم OpenAI-compatible (OpenAI / OpenRouter / NIM / خادم محلي) — بلا مكتبات جديدة.
object AiRewriteService {
    const val DEFAULT_ENDPOINT = "https://api.openai.com/v1"
    const val DEFAULT_MODEL = "gpt-4o-mini"

    sealed interface AiError {
        data object NoKey : AiError
        data object Empty : AiError
        data object Net : AiError
        data class Http(val code: Int) : AiError
        data object Parse : AiError
    }

    fun systemRewrite(lang: String): String = if (lang == "ar")
        "أنت محرر نصوص عربي. أعد كتابة نص المستخدم بأسلوب أوضح وأسلس مع الحفاظ على المعنى والمعلومات كاملة. " +
        "أخرج النص المعاد كتابته فقط — بلا مقدمات ولا شروح ولا علامات تنصيص إضافية."
    else
        "You are a text editor. Rewrite the user's text more clearly and fluently while preserving the full meaning and information. " +
        "Output only the rewritten text — no introductions, no explanations, no extra quotes."

    fun systemSummarize(lang: String): String = if (lang == "ar")
        "أنت ملخّص ملاحظات عربي. لخّص نص المستخدم في نقاط موجزة تحفظ كل المعلومات المهمة بلا إضافات. " +
        "أخرج التلخيص فقط — بلا مقدمات ولا شروح."
    else
        "You are a notes summarizer. Summarize the user's text in concise bullet points preserving all key information, adding nothing. " +
        "Output only the summary — no introductions, no explanations."

    const val PING_PROMPT = "Reply with exactly: ok"

    fun systemFormat(lang: String): String = if (lang == "ar")
        "أنت منسّق ملاحظات Markdown. نظّم نص المستخدم: عناوين (# ## ###) للأقسام، وقوائم نقطية (-) أو مرقمة (1.) للبنود، " +
        "وصناديق مهام (- [ ]) للمهام، مع الحفاظ على كل المعلومات وعدم إضافة محتوى جديد. " +
        "أخرج الـ Markdown فقط — بلا مقدمات ولا شروح ولا أسوار كود."
    else
        "You are a Markdown notes formatter. Organize the user's text: headings (# ## ###) for sections, " +
        "bulleted (-) or numbered (1.) lists for items, task boxes (- [ ]) for tasks, preserving all information " +
        "and adding no new content. Output only the Markdown — no introductions, no explanations, no code fences."

    suspend fun complete(
        endpoint: String,
        apiKey: String,
        model: String,
        system: String,
        user: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(Exception("nokey"))
        if (user.isBlank()) return@withContext Result.failure(Exception("empty"))
        try {
            val url = URL(endpoint.trimEnd('/') + "/chat/completions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 90_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
            }
            val body = JSONObject()
                .put("model", model.ifBlank { DEFAULT_MODEL })
                .put("temperature", 0.7)
                .put("max_tokens", 4000)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", user)))
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code != 200) {
                runCatching { conn.errorStream?.close() }
                conn.disconnect()
                return@withContext Result.failure(Exception("http:$code"))
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val content = runCatching {
                JSONObject(text).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message")
                    .getString("content").trim()
                    .removePrefix("```markdown").removePrefix("```").removeSuffix("```").trim()
            }.getOrNull()
            if (content.isNullOrBlank()) Result.failure(Exception("parse"))
            else Result.success(content)
        } catch (e: Exception) {
            val m = e.message ?: ""
            if (m.startsWith("http:") || m == "nokey" || m == "empty" || m == "parse")
                Result.failure(e)
            else Result.failure(Exception("net"))
        }
    }
}

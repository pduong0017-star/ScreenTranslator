package com.screentranslator.app.translate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface TranslationEngine {
    suspend fun translateBatch(
        texts: List<String>,
        targetLang: String = "Vietnamese",
        isComicMode: Boolean = true
    ): List<String>
}

/**
 * Intelligent Translator using Gemini / DeepSeek / OpenAI LLM APIs
 * for high-context natural manga/comic/chat translation.
 */
class SmartAiTranslator(
    private val apiKey: String = "",
    private val endpointUrl: String = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
) : TranslationEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun translateBatch(
        texts: List<String>,
        targetLang: String,
        isComicMode: Boolean
    ): List<String> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) return@withContext emptyList()

        if (apiKey.isBlank()) {
            // Fallback to free Google Translate web endpoint if no API key provided
            return@withContext texts.map { fallbackTranslate(it, "vi") }
        }

        try {
            val systemInstruction = if (isComicMode) {
                "Bạn là chuyên gia dịch truyện tranh (manga/manhwa/comic) và tin nhắn hội thoại. " +
                "Nhiệm vụ: Dịch danh sách các câu sau sang $targetLang. " +
                "Yêu cầu: Dịch mượt mà, tự nhiên theo văn phong truyện tranh/tin nhắn giới trẻ, xưng hô linh hoạt, giữ nguyên thứ tự. " +
                "Trả về duy nhất định dạng JSON mảng chuỗi (JSON array of strings) chứa các câu đã dịch, không thêm giải thích hay markdown codeblock."
            } else {
                "Translate the following list of sentences into $targetLang accurately. Return strictly a JSON array of strings."
            }

            val promptBuilder = StringBuilder()
            promptBuilder.append(systemInstruction).append("\n\nDanh sách câu cần dịch:\n")
            texts.forEachIndexed { index, text ->
                promptBuilder.append("${index + 1}. $text\n")
            }

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", promptBuilder.toString()))
                        })
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val urlWithKey = "$endpointUrl?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(urlWithKey)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext texts

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val textOutput = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val cleanJson = textOutput.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val jsonArray = JSONArray(cleanJson)
                val results = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    results.add(jsonArray.getString(i))
                }
                if (results.size == texts.size) return@withContext results
            }
            texts
        } catch (e: Exception) {
            e.printStackTrace()
            // If LLM call fails, fall back to free translator
            texts.map { fallbackTranslate(it, "vi") }
        }
    }

    private fun fallbackTranslate(text: String, targetLang: String): String {
        return try {
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=" +
                    java.net.URLEncoder.encode(text, "UTF-8")
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return text
            val jsonArray = JSONArray(body)
            val sentences = jsonArray.getJSONArray(0)
            val sb = StringBuilder()
            for (i in 0 until sentences.length()) {
                sb.append(sentences.getJSONArray(i).getString(0))
            }
            sb.toString()
        } catch (e: Exception) {
            text
        }
    }
}

package com.example.ai

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getRecommendations(
        userName: String,
        profileInfo: String,
        statsSummary: String,
        recentWorkouts: String,
        recentSleep: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key not configured properly. Please use the Secrets panel in AI Studio to set your GEMINI_API_KEY."
        }

        val prompt = """
            User: $userName
            Profile details: $profileInfo
            Today's steps/health: $statsSummary
            Recent Workouts: $recentWorkouts
            Recent Sleep logs: $recentSleep
            
            Please provide personalized, highly encouraging wellness recommendations:
            1. Daily goal adjustments or workout recommendations.
            2. Nutritional advice matching their daily targets.
            3. Lifestyle/Sleep tips based on sleep quality.
            Keep the tips concise, practical, and list them with bullet points.
        """.trimIndent()

        try {
            // Build request JSON using Android SDK native JSONObject
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val systemInstructionObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", "You are an elite, highly professional personal fitness and wellness coach named FitPulse Coach. Keep your advice structured, actionable, and visually clean (use bold terms and short bullet points).")
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put("systemInstruction", systemInstructionObj)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "${BASE_URL}v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed: code=${response.code} body=$errorBody")
                    return@withContext "Error: Failed to fetch recommendations from Coach. (Code: ${response.code})"
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    return@withContext "Coach received an empty response. Let's try again in a bit!"
                }

                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext "No advice generated yet. Rest up and try again shortly!"
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts == null || parts.length() == 0) {
                    return@withContext "The Coach seems lost in thought. Please request advice again."
                }

                val text = parts.getJSONObject(0).optString("text")
                if (text.isNullOrBlank()) {
                    return@withContext "The Coach gave silent advice. Try re-logging your workout details!"
                }

                return@withContext text
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during recommendation call", e)
            return@withContext "Connection Error: Failed to talk to FitPulse Coach. Error: ${e.message ?: "Unknown"}"
        }
    }
}

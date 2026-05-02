package com.example.llmapp

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceOptions
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.File

class LlamaServer(
    private val context: Context,
    private val onLog: (String) -> Unit
) : NanoHTTPD(8080) {

    private var llm: LlmInference? = null

    fun startWithModel() {
        // โหลดโมเดลจาก /sdcard/Download/model.bin
        val modelPath = "/sdcard/Download/model.bin"

        if (!File(modelPath).exists()) {
            onLog("⚠️ ไม่พบโมเดลที่ $modelPath")
            onLog("วางไฟล์ model.bin ใน Download แล้ว restart แอป")
        } else {
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setPreferredBackend(LlmInferenceOptions.Backend.GPU) // ใช้ GPU
                .build()

            llm = LlmInference.createFromOptions(context, options)
            onLog("✅ โหลดโมเดลสำเร็จ (GPU)")
        }

        start(SOCKET_READ_TIMEOUT, false)
        onLog("🚀 HTTP Server เริ่มที่ port 8080")
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        onLog("📥 ${method} $uri")

        return when {
            // GET /health - เช็คสถานะ
            uri == "/health" && method == Method.GET -> {
                val json = JSONObject()
                json.put("status", "ok")
                json.put("model_loaded", llm != null)
                json.put("port", 8080)
                jsonResponse(json.toString())
            }

            // POST /generate - ส่ง prompt รับ response
            uri == "/generate" && method == Method.POST -> {
                handleGenerate(session)
            }

            // GET /generate?prompt=xxx - GET version
            uri.startsWith("/generate") && method == Method.GET -> {
                val prompt = session.parameters["prompt"]?.firstOrNull() ?: ""
                generateResponse(prompt)
            }

            else -> {
                val json = JSONObject()
                json.put("error", "Not found")
                json.put("routes", listOf("/health", "/generate"))
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "application/json",
                    json.toString()
                )
            }
        }
    }

    private fun handleGenerate(session: IHTTPSession): Response {
        return try {
            val body = mutableMapOf<String, String>()
            session.parseBody(body)
            val jsonStr = body["postData"] ?: "{}"
            val json = JSONObject(jsonStr)
            val prompt = json.optString("prompt", "")

            if (prompt.isEmpty()) {
                val err = JSONObject()
                err.put("error", "กรุณาส่ง prompt")
                return jsonResponse(err.toString(), Response.Status.BAD_REQUEST)
            }

            generateResponse(prompt)
        } catch (e: Exception) {
            val err = JSONObject()
            err.put("error", e.message)
            jsonResponse(err.toString(), Response.Status.INTERNAL_ERROR)
        }
    }

    private fun generateResponse(prompt: String): Response {
        if (llm == null) {
            val err = JSONObject()
            err.put("error", "โมเดลยังไม่โหลด วางไฟล์ model.bin ใน /sdcard/Download/")
            return jsonResponse(err.toString(), Response.Status.INTERNAL_ERROR)
        }

        return try {
            onLog("🤔 กำลังคิด: ${prompt.take(50)}...")
            val result = llm!!.generateResponse(prompt)
            onLog("✅ ตอบแล้ว")

            val response = JSONObject()
            response.put("prompt", prompt)
            response.put("response", result)
            response.put("model", "mediapipe-gpu")

            jsonResponse(response.toString())
        } catch (e: Exception) {
            val err = JSONObject()
            err.put("error", e.message)
            jsonResponse(err.toString(), Response.Status.INTERNAL_ERROR)
        }
    }

    private fun jsonResponse(
        json: String,
        status: Response.Status = Response.Status.OK
    ): Response {
        return newFixedLengthResponse(status, "application/json", json)
    }
}

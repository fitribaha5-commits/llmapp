package com.example.llmapp

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.File

class LlamaServer(
    private val context: Context,
    private val onLog: (String) -> Unit
) : NanoHTTPD(8080) {

    private var llm: LlmInference? = null

    fun startWithModel() {
        val modelPath = "/sdcard/Download/model.bin"

        if (!File(modelPath).exists()) {
            onLog("⚠️ ไม่พบโมเดลที่ $modelPath")
        } else {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .build()

            llm = LlmInference.createFromOptions(context, options)
            onLog("✅ โหลดโมเดลสำเร็จ")
        }

        start(SOCKET_READ_TIMEOUT, false)
        onLog("🚀 Server เริ่มที่ port 8080")
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return when {
            uri == "/health" && method == Method.GET -> {
                val json = JSONObject()
                json.put("status", "ok")
                json.put("model_loaded", llm != null)
                jsonResponse(json.toString())
            }

            uri == "/generate" && method == Method.POST -> {
                handleGenerate(session)
            }

            uri.startsWith("/generate") && method == Method.GET -> {
                val prompt = session.parameters["prompt"]?.firstOrNull() ?: ""
                generateResponse(prompt)
            }

            else -> {
                val json = JSONObject()
                json.put("error", "Not found")
                jsonResponse(json.toString(), Response.Status.NOT_FOUND)
            }
        }
    }

    private fun handleGenerate(session: IHTTPSession): Response {
        return try {
            val body = mutableMapOf<String, String>()
            session.parseBody(body)
            val json = JSONObject(body["postData"] ?: "{}")
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
            err.put("error", "โมเดลยังไม่โหลด")
            return jsonResponse(err.toString(), Response.Status.INTERNAL_ERROR)
        }
        return try {
            onLog("🤔 กำลังคิด...")
            val result = llm!!.generateResponse(prompt)
            val response = JSONObject()
            response.put("prompt", prompt)
            response.put("response", result)
            jsonResponse(response.toString())
        } catch (e: Exception) {
            val err = JSONObject()
            err.put("error", e.message)
            jsonResponse(err.toString(), Response.Status.INTERNAL_ERROR)
        }
    }

    private fun jsonResponse(json: String, status: Response.Status = Response.Status.OK): Response {
        return newFixedLengthResponse(status, "application/json", json)
    }
}

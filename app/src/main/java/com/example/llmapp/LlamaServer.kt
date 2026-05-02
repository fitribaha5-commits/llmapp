package com.example.llmapp

import android.content.Context
import android.os.Environment
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
        // ลองหลาย path
        val paths = listOf(
            "/sdcard/Download/model.bin",
            "${Environment.getExternalStorageDirectory()}/Download/model.bin",
            "${context.getExternalFilesDir(null)}/model.bin",
            "${context.filesDir}/model.bin"
        )

        val modelPath = paths.firstOrNull { File(it).exists() }

        if (modelPath == null) {
            onLog("⚠️ ไม่พบโมเดล ลองวางไว้ที่:")
            paths.forEach { onLog("  - $it") }
        } else {
            onLog("✅ พบโมเดลที่ $modelPath")
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(1024)
                    .build()
                llm = LlmInference.createFromOptions(context, options)
                onLog("✅ โหลดโมเดลสำเร็จ")
            } catch (e: Exception) {
                onLog("❌ โหลดโมเดล error: ${e.message}")
            }
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
            uri == "/generate" && method == Method.POST -> handleGenerate(session)
            uri.startsWith("/generate") && method == Method.GET -> {
                val prompt = session.parameters["prompt"]?.firstOrNull() ?: ""
                generateResponse(prompt)
            }
            else -> {
                jsonResponse(JSONObject().put("error", "Not found").toString(), Response.Status.NOT_FOUND)
            }
        }
    }

    private fun handleGenerate(session: IHTTPSession): Response {
        return try {
            val body = mutableMapOf<String, String>()
            session.parseBody(body)
            val json = JSONObject(body["postData"] ?: "{}")
            val prompt = json.optString("prompt", "")
            if (prompt.isEmpty()) return jsonResponse(
                JSONObject().put("error", "กรุณาส่ง prompt").toString(),
                Response.Status.BAD_REQUEST
            )
            generateResponse(prompt)
        } catch (e: Exception) {
            jsonResponse(JSONObject().put("error", e.message).toString(), Response.Status.INTERNAL_ERROR)
        }
    }

    private fun generateResponse(prompt: String): Response {
        if (llm == null) return jsonResponse(
            JSONObject().put("error", "โมเดลยังไม่โหลด").toString(),
            Response.Status.INTERNAL_ERROR
        )
        return try {
            onLog("🤔 กำลังคิด...")
            val result = llm!!.generateResponse(prompt)
            val response = JSONObject()
            response.put("prompt", prompt)
            response.put("response", result)
            jsonResponse(response.toString())
        } catch (e: Exception) {
            jsonResponse(JSONObject().put("error", e.message).toString(), Response.Status.INTERNAL_ERROR)
        }
    }

    private fun jsonResponse(json: String, status: Response.Status = Response.Status.OK) =
        newFixedLengthResponse(status, "application/json", json)
}

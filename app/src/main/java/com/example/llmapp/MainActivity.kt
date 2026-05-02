package com.example.llmapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var server: LlamaServer
    private lateinit var statusText: TextView
    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)

        // ขอสิทธิ์ MANAGE_EXTERNAL_STORAGE (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                statusText.text = "⚠️ กรุณาให้สิทธิ์ Storage"
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, 100)
                return
            }
        }

        startServer()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            startServer()
        }
    }

    private fun startServer() {
        server = LlamaServer(this) { log ->
            runOnUiThread { logText.append("\n$log") }
        }
        try {
            server.startWithModel()
            statusText.text = "✅ Server รันอยู่ที่ port 8080"
        } catch (e: Exception) {
            statusText.text = "❌ Error: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::server.isInitialized) server.stop()
    }
}

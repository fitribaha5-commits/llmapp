package com.example.llmapp

import android.os.Bundle
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
        server.stop()
    }
}

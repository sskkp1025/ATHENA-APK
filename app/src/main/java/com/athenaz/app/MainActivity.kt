package com.athenaz.app

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var ipInput: EditText
    private lateinit var tokenInput: EditText
    private lateinit var tgTokenInput: EditText
    private lateinit var tgChatInput: EditText
    private lateinit var apiKeyInput: EditText
    private lateinit var apiSecInput: EditText
    private lateinit var codeInput: EditText
    private lateinit var logText: TextView

    // 🚀 페이지 3개 뷰 변수 선언
    private lateinit var pageDeploy: View
    private lateinit var pageStrategy: View
    private lateinit var pageTerminal: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipInput = findViewById(R.id.ipInput)
        tokenInput = findViewById(R.id.tokenInput)
        tgTokenInput = findViewById(R.id.tgTokenInput)
        tgChatInput = findViewById(R.id.tgChatInput)
        apiKeyInput = findViewById(R.id.apiKeyInput)
        apiSecInput = findViewById(R.id.apiSecInput)
        codeInput = findViewById(R.id.codeInput)
        logText = findViewById(R.id.logText)
        
        // 터미널 스크롤 가능하게 설정
        logText.movementMethod = ScrollingMovementMethod()

        // 🚀 페이지 맵핑
        pageDeploy = findViewById(R.id.page_deploy)
        pageStrategy = findViewById(R.id.page_strategy)
        pageTerminal = findViewById(R.id.page_terminal)

        // 🚀 하단 탭 바 클릭 이벤트 (페이지 전환)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            pageDeploy.visibility = View.GONE
            pageStrategy.visibility = View.GONE
            pageTerminal.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_deploy -> pageDeploy.visibility = View.VISIBLE
                R.id.nav_strategy -> pageStrategy.visibility = View.VISIBLE
                R.id.nav_terminal -> pageTerminal.visibility = View.VISIBLE
            }
            true
        }

        loadConfig()

        findViewById<Button>(R.id.deployBtn).setOnClickListener {
            saveConfig()
            val ip = ipInput.text.toString()
            val token = tokenInput.text.toString()
            log("[*] Deploying to $ip...")

            val jsonParam = JSONObject().apply {
                put("api_key", apiKeyInput.text.toString())
                put("api_sec", apiSecInput.text.toString())
                put("tg_token", tgTokenInput.text.toString())
                put("tg_chat", tgChatInput.text.toString())
                put("code", codeInput.text.toString())
            }
            sendPostRequest("http://$ip:8000/deploy", token, jsonParam.toString())
        }

        findViewById<Button>(R.id.stopBtn).setOnClickListener {
            val ip = ipInput.text.toString()
            val token = tokenInput.text.toString()
            log("[*] Stopping bot...")
            sendPostRequest("http://$ip:8000/stop", token, "")
        }
    }

    private fun sendPostRequest(urlString: String, token: String, jsonBody: String) {
        thread {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.doOutput = true
                conn.connectTimeout = 5000

                if (jsonBody.isNotEmpty()) {
                    OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }
                }

                val responseCode = conn.responseCode
                runOnUiThread {
                    if (responseCode == 200) log("[SUCCESS] Bot is running! (HTTP 200)")
                    else log("[FAIL] Server rejected (HTTP $responseCode)")
                }
            } catch (e: Exception) {
                runOnUiThread { log("[ERROR] Connection failed: ${e.message}") }
            }
        }
    }

    private fun log(msg: String) {
        runOnUiThread { logText.append("\n" + msg) }
    }

    private fun saveConfig() {
        val prefs = getSharedPreferences("AthenaPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("ip", ipInput.text.toString())
            putString("token", tokenInput.text.toString())
            putString("tgToken", tgTokenInput.text.toString())
            putString("tgChat", tgChatInput.text.toString())
            putString("apiKey", apiKeyInput.text.toString())
            putString("apiSec", apiSecInput.text.toString())
            putString("code", codeInput.text.toString())
            apply()
        }
    }

    private fun loadConfig() {
        val prefs = getSharedPreferences("AthenaPrefs", Context.MODE_PRIVATE)
        ipInput.setText(prefs.getString("ip", ""))
        tokenInput.setText(prefs.getString("token", "athena_secure_1234"))
        tgTokenInput.setText(prefs.getString("tgToken", ""))
        tgChatInput.setText(prefs.getString("tgChat", ""))
        apiKeyInput.setText(prefs.getString("apiKey", ""))
        apiSecInput.setText(prefs.getString("apiSec", ""))
        codeInput.setText(prefs.getString("code", ""))
    }
}

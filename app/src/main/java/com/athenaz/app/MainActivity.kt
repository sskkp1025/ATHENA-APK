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
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
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

    private lateinit var pageDeploy: View
    private lateinit var pageStrategy: View
    private lateinit var pageTerminal: View

    private var sshJob: Job? = null

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
        
        logText.movementMethod = ScrollingMovementMethod()

        pageDeploy = findViewById(R.id.page_deploy)
        pageStrategy = findViewById(R.id.page_strategy)
        pageTerminal = findViewById(R.id.page_terminal)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            pageDeploy.visibility = View.GONE
            pageStrategy.visibility = View.GONE
            pageTerminal.visibility = View.GONE
            
            sshJob?.cancel()

            when (item.itemId) {
                R.id.nav_deploy -> pageDeploy.visibility = View.VISIBLE
                R.id.nav_strategy -> pageStrategy.visibility = View.VISIBLE
                R.id.nav_terminal -> {
                    pageTerminal.visibility = View.VISIBLE
                    
                    val ip = ipInput.text.toString()
                    if (ip.isNotBlank()) {
                        connectToVultrTerminal(ip)
                    } else {
                        logText.text = "[SYSTEM] DEPLOY 탭에서 Vultr Server IP를 먼저 입력해주세요.\n"
                    }
                }
            }
            true
        }

        findViewById<Button>(R.id.clearStrategyBtn).setOnClickListener {
            codeInput.setText("")
        }

        findViewById<Button>(R.id.clearTerminalBtn).setOnClickListener {
            logText.text = "[SYSTEM] Native Terminal Ready.\n"
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

    private fun connectToVultrTerminal(ip: String) {
        logText.text = "[SYSTEM] $ip 서버에 SSH 연결을 시도합니다...\n"
        
        sshJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsch = JSch()
                
                val user = "root" 
                // ⚠️ 여기에 실제 서버 비밀번호를 입력하세요!
                val password = "!Vt6,pc_P2NcRx#6" 
                
                val session = jsch.getSession(user, ip, 22)
                session.setPassword(password)
                session.setConfig("StrictHostKeyChecking", "no")
                session.connect(10000)

                withContext(Dispatchers.Main) {
                    logText.append("[SYSTEM] Vultr 연결 성공! 최신 봇 로그를 탐색 중입니다...\n\n")
                }

                val channel = session.openChannel("exec") as ChannelExec
                
                // 🚀 서버에서 가장 최근에 업데이트된 로그 파일(.log 또는 .out)을 찾아 실시간으로 띄워주는 스마트 명령어
                val smartCommand = "sh -c 'tail -n 50 -f `ls -t /root/*.log /root/*.out /root/*/*.log /root/*/*.out 2>/dev/null | head -n 1`'"
                channel.setCommand(smartCommand) 

                val inStream = channel.inputStream
                channel.connect()

                val reader = BufferedReader(InputStreamReader(inStream))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    withContext(Dispatchers.Main) {
                        logText.append(line + "\n")
                        
                        val layout = logText.layout
                        if (layout != null) {
                            val scrollAmount = layout.getLineTop(logText.lineCount) - logText.height
                            if (scrollAmount > 0) {
                                logText.scrollTo(0, scrollAmount)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logText.append("\n[ERROR] 터미널 연결 실패: ${e.message}\n")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sshJob?.cancel()
    }
}

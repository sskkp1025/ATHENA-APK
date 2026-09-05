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

    // 🚀 SSH 통신을 백그라운드에서 제어할 코루틴 Job
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
            
            // 🚀 다른 탭으로 이동하면 서버 부하를 막기 위해 SSH 연결을 끊습니다.
            sshJob?.cancel() 

            when (item.itemId) {
                R.id.nav_deploy -> pageDeploy.visibility = View.VISIBLE
                R.id.nav_strategy -> pageStrategy.visibility = View.VISIBLE
                R.id.nav_terminal -> {
                    pageTerminal.visibility = View.VISIBLE
                    
                    // 🚀 첫 번째 탭(DEPLOY)에 입력된 IP를 그대로 가져옵니다.
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

        // 지우기 귀찮음 해결! CLEAR 버튼 기능 추가
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

    // =========================================================================
    // 🚀 새롭게 추가된 네이티브 터미널 연결 함수 (보기 전용)
    // =========================================================================
    private fun connectToVultrTerminal(ip: String) {
        logText.text = "[SYSTEM] $ip 서버에 SSH 연결을 시도합니다...\n"
        
        sshJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsch = JSch()
                
                // ⚠️ 여기에 Vultr 서버 접속 정보를 입력하세요
                val user = "root" 
                val password = "여기에_비밀번호_입력" // <--- 실제 비밀번호로 변경!
                
                val session = jsch.getSession(user, ip, 22)
                session.setPassword(password)
                session.setConfig("StrictHostKeyChecking", "no")
                session.connect(10000)

                withContext(Dispatchers.Main) {
                    logText.append("[SYSTEM] Vultr 연결 성공! 터미널 출력을 불러옵니다...\n\n")
                }

                val channel = session.openChannel("exec") as ChannelExec
                
                // ⚠️ 실행할 명령어 세팅 
                // 지금은 테스트용으로 실시간 프로세스(top)를 띄웁니다. 
                // 봇의 실시간 로그를 보시려면 "tail -f /경로/아테나로그파일.log" 로 변경하세요!
                channel.setCommand("top -b")

                val inStream = channel.inputStream
                channel.connect()

                val reader = BufferedReader(InputStreamReader(inStream))
                var line: String?

                // 서버에서 출력되는 텍스트를 실시간으로 한 줄씩 logText에 찍어줍니다.
                while (reader.readLine().also { line = it } != null) {
                    withContext(Dispatchers.Main) {
                        logText.append(line + "\n")
                        
                        // 내용이 추가될 때마다 텍스트 뷰를 맨 아래로 자동 스크롤
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
        // 앱을 끄면 켜져있던 SSH 터미널 연결도 깔끔하게 종료
        sshJob?.cancel() 
    }
}

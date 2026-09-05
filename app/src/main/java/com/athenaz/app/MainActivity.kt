package com.athenaz.app

import android.os.Bundle
import android.view.View
import android.widget.ScrollView
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
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    // 비동기 코루틴 작업을 관리하는 변수
    private var sshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val terminalScrollView = findViewById<ScrollView>(R.id.terminalScrollView)
        val terminalTextView = findViewById<TextView>(R.id.terminalTextView)

        bottomNavigationView.setOnItemSelectedListener { item ->
            // 하단 탭을 눌렀을 때의 동작
            when (item.itemId) {
                // TODO: bottom_nav_menu.xml에 설정된 세 번째 메뉴 아이디를 아래에 넣으세요
                // 예: R.id.menu_item_3
                item.itemId -> {
                    // 세 번째 탭을 누르면 터미널 화면을 표시
                    terminalScrollView.visibility = View.VISIBLE
                    
                    // 텍스트 초기화 및 기존 연결 취소 후 새 연결 시작
                    sshJob?.cancel()
                    terminalTextView.text = "Vultr 서버에 접속 시도 중...\n"
                    connectToVultrTerminal(terminalTextView)
                    true
                }
                else -> {
                    // 다른 탭을 누르면 터미널 화면 숨김
                    terminalScrollView.visibility = View.GONE
                    sshJob?.cancel() // 다른 탭으로 가면 불필요한 SSH 트래픽 정지
                    true
                }
            }
        }
    }

    private fun connectToVultrTerminal(terminalTextView: TextView) {
        sshJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsch = JSch()
                
                // ============================================
                // [필수 수정] 아래 Vultr 서버 접속 정보를 입력하세요
                // ============================================
                val host = "192.168.0.1"     // 서버 IP 주소
                val user = "root"            // 접속 계정
                val password = "비밀번호"     // 접속 비밀번호
                // ============================================

                val session = jsch.getSession(user, host, 22)
                session.setPassword(password)
                
                // 처음 접속 시 HostKey 확인 스킵 (앱에서 간편 접속용)
                session.setConfig("StrictHostKeyChecking", "no")
                session.connect(10000) // 10초 타임아웃

                withContext(Dispatchers.Main) {
                    terminalTextView.append("연결 성공! 로그 출력을 불러옵니다...\n\n")
                }

                val channel = session.openChannel("exec") as ChannelExec
                
                // ============================================
                // [필수 수정] 보고 싶은 명령어 (예: 서버 실시간 상태 보기)
                // ATHENA-Z 엔진 백테스팅이나 봇의 로그를 띄우려면 tail 명령어를 쓰세요.
                // 예: channel.setCommand("tail -f /경로/athena.log")
                // ============================================
                channel.setCommand("top -b") 

                val inStream = channel.inputStream
                channel.connect()

                val reader = BufferedReader(InputStreamReader(inStream))
                var line: String?

                // 서버 출력을 실시간으로 한 줄씩 읽어서 TextView에 추가
                while (reader.readLine().also { line = it } != null) {
                    withContext(Dispatchers.Main) {
                        terminalTextView.append(line + "\n")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    terminalTextView.append("\n[연결 오류]: ${e.message}\n")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 앱이 종료될 때 SSH 연결 자원을 깔끔하게 해제
        sshJob?.cancel()
    }
}

package com.athenaz.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val terminalWebView = findViewById<WebView>(R.id.terminalWebView)

        // 터미널 화면(ttyd)을 웹뷰에 띄우기 위해 필수적인 자바스크립트 허용 세팅
        terminalWebView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
        }

        // 하단 탭을 눌렀을 때의 동작 설정
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_tab1 -> {
                    // 1번째 탭: 터미널 숨김
                    terminalWebView.visibility = View.GONE
                    true
                }
                R.id.navigation_tab2 -> {
                    // 2번째 탭: 터미널 숨김
                    terminalWebView.visibility = View.GONE
                    true
                }
                R.id.navigation_tab3 -> {
                    // 3번째 탭: 터미널 화면 표시 및 AWS 서버 터미널(7681 포트) 접속
                    terminalWebView.visibility = View.VISIBLE
                    terminalWebView.loadUrl("http://45.76.195.208:7681")
                    true
                }
                else -> false
            }
        }
    }
}
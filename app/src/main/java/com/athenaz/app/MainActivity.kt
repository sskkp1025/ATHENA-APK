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

        terminalWebView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
        }

        // 대표님의 원래 메뉴 ID가 무엇이든 상관없이, 
        // 무조건 3번째 탭(인덱스 2)에서 웹뷰가 열리도록 동적 처리
        bottomNavigationView.setOnItemSelectedListener { item ->
            val menu = bottomNavigationView.menu
            
            // 클릭한 탭이 3번째 탭(index 2)일 경우
            if (item.itemId == menu.getItem(2).itemId) {
                terminalWebView.visibility = View.VISIBLE
                terminalWebView.loadUrl("http://45.76.195.208:7681")
                true
            } 
            // 1번째, 2번째 탭을 눌렀을 경우
            else {
                terminalWebView.visibility = View.GONE
                true
            }
        }
    }
}

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

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_tab1 -> {
                    terminalWebView.visibility = View.GONE
                    true
                }
                R.id.navigation_tab2 -> {
                    terminalWebView.visibility = View.GONE
                    true
                }
                R.id.navigation_tab3 -> {
                    terminalWebView.visibility = View.VISIBLE
                    terminalWebView.loadUrl("http://45.76.195.208:7681")
                    true
                }
                else -> false
            }
        }
    }
}
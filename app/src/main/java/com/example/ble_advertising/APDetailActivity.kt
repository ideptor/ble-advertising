package com.example.ble_advertising

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.example.ble_advertising.apmanger.APManager

class APDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ap_detail)
        val apManager: APManager? = APManager.getApManager(this)
        val textView = findViewById<AppCompatTextView>(R.id.apDetail)
        val sb = "SSID : ${apManager?.sSID} ${System.lineSeparator()}PASS : ${apManager?.password}"
        textView.text = sb
        findViewById<View>(R.id.btnTurnOff).setOnClickListener { v: View? ->
            apManager?.disableWifiAp()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
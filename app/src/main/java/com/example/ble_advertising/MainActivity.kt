package com.example.ble_advertising

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ble_advertising.apmanger.APManager
import com.example.ble_advertising.apmanger.DefaultFailureListener


class MainActivity : AppCompatActivity(), APManager.OnSuccessListener {

    var start = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btnTurnOn).setOnClickListener { v: View? ->
            Toast.makeText(this, "button clicked", Toast.LENGTH_LONG).show()
            start = System.currentTimeMillis()
            val apManager: APManager? = APManager.getApManager(this)
            apManager?.turnOnHotspot(
                this,
                this,
                DefaultFailureListener(this)
            )
        }
    }

    override fun onSuccess(ssid: String, password: String) {
        val durationSec = (System.currentTimeMillis()-start) / 1000.0
        Log.e("MEASURE", "Softap duraction(sec): $durationSec")
        Toast.makeText(this, "$ssid,$password", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, APDetailActivity::class.java))
    }
}
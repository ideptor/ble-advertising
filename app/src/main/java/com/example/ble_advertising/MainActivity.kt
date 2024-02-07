package com.example.ble_advertising

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ble_advertising.apmanger.APManager
import com.example.ble_advertising.apmanger.DefaultFailureListener
import com.example.ble_advertising.bleadv.bleAdvertising
import com.example.ble_advertising.bleadv.bleAdvertising2
import com.example.ble_advertising.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), APManager.OnSuccessListener {

    private lateinit var binding: ActivityMainBinding
    var start = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        binding.btnTurnOn.setOnClickListener { v: View? ->
            Toast.makeText(this, "button clicked", Toast.LENGTH_LONG).show()
            start = System.currentTimeMillis()
            val apManager: APManager? = APManager.getApManager(this)
            apManager?.turnOnHotspot(
                this,
                this,
                DefaultFailureListener(this)
            )
        }
        binding.btnBleAdvertising.setOnClickListener{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bleAdvertising2(this)
            } else {
                Toast.makeText(this, "SDK should be later that Build.VERSION_CODES.O ${Build.VERSION_CODES.O}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSuccess(ssid: String, password: String) {
        val durationSec = (System.currentTimeMillis()-start) / 1000.0
        Log.e("MEASURE", "Softap duraction(sec): $durationSec")
        Toast.makeText(this, "$ssid,$password", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, APDetailActivity::class.java))
    }


}
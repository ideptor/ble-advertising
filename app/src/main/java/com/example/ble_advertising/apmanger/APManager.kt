package com.example.ble_advertising.apmanger

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Random

/**
 * <h1>APManager - Access Point Manager</h1>
 * <P>
 * APManager is a singleton utility class that help to create mobile hotspot on android device
 * programmatically , without taking care of android version and permission requires
 * needed to do the same.It supports android 5.0 and later android version.
</P> *
 */
class APManager private constructor(context: Context) {

    val TAG = "APMANAGER"

    val utils: Utils

    /**
     * get ssid of recently created hotspot
     * @return SSID
     */
    var sSID: String? = null
        private set

    /**
     * get password of recently created hotspot
     * @return PASSWORD
     */
    var password: String? = null
        private set
    val wifiManager: WifiManager
    private val locationManager: LocationManager
    private var reservation: LocalOnlyHotspotReservation? = null

    init {
        wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        utils = Utils()
    }

    fun turnOnHotspot(
        context: Context,
        onSuccessListener: OnSuccessListener,
        onFailureListener: OnFailureListener
    ) {
        Log.w(TAG, "turnOnHotsopt")
        val providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        Log.w(TAG, "providerEnabled: $providerEnabled")
        Log.w(TAG, "isDeviceConnectedToWifi: $isDeviceConnectedToWifi")
        if (isDeviceConnectedToWifi) {
            onFailureListener.onFailure(ERROR_DISABLE_WIFI, null)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (utils.checkLocationPermission(context) && providerEnabled && !isWifiApEnabled) {
                Log.w(TAG, "into the if (utils.checkLocationPermission(context) && providerEnabled && !isWifiApEnabled)")
                try {
                    wifiManager.startLocalOnlyHotspot(object : LocalOnlyHotspotCallback() {
                        override fun onStarted(reservation: LocalOnlyHotspotReservation) {
                            Log.w(TAG, "onStarted")
                            super.onStarted(reservation)
                            this@APManager.reservation = reservation
                            try {
                                sSID = reservation.wifiConfiguration!!.SSID
                                password = reservation.wifiConfiguration!!.preSharedKey
                                Log.w(TAG, "sSID: $sSID, password: $password")
                                onSuccessListener.onSuccess(sSID.toString(), password.toString())
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onFailureListener.onFailure(ERROR_UNKNOWN, e)
                            }
                        }

                        override fun onFailed(reason: Int) {
                            super.onFailed(reason)
                            Log.w(TAG, "onFailed")
                            onFailureListener.onFailure(
                                if (reason == ERROR_TETHERING_DISALLOWED) ERROR_DISABLE_HOTSPOT else ERROR_UNKNOWN,
                                null
                            )
                        }
                    }, Handler(Looper.getMainLooper()))
                } catch (e: Exception) {
                    Log.w(TAG,"Failure: $e")
                    onFailureListener.onFailure(ERROR_UNKNOWN, e)
                }
            } else if (!providerEnabled) {
                onFailureListener.onFailure(ERROR_GPS_PROVIDER_DISABLED, null)
            } else if (isWifiApEnabled) {
                onFailureListener.onFailure(ERROR_DISABLE_HOTSPOT, null)
            } else {
                onFailureListener.onFailure(ERROR_LOCATION_PERMISSION_DENIED, null)
            }
        } else {
            if (!utils.checkLocationPermission(context)) {
                onFailureListener.onFailure(ERROR_LOCATION_PERMISSION_DENIED, null)
                return
            }
            if (!utils.checkWriteSettingPermission(context)) {
                onFailureListener.onFailure(ERROR_WRITE_SETTINGS_PERMISSION_REQUIRED, null)
                return
            }
            try {
                sSID = "AndroidAP_" + Random().nextInt(10000)
                password = randomPassword
                val wifiConfiguration = WifiConfiguration()
                wifiConfiguration.SSID = sSID
                wifiConfiguration.preSharedKey = password
                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                wifiManager.setWifiEnabled(false)
                setWifiApEnabled(wifiConfiguration, true)
                onSuccessListener.onSuccess(sSID!!, password!!)
            } catch (e: Exception) {
                e.printStackTrace()
                onFailureListener.onFailure(ERROR_LOCATION_PERMISSION_DENIED, e)
            }
        }
    }

    fun disableWifiAp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                reservation!!.close()
            } else {
                setWifiApEnabled(null, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val isWifiApEnabled: Boolean
        get() {
            try {
                val method = wifiManager.javaClass.getMethod("isWifiApEnabled")
                return method.invoke(wifiManager) as Boolean
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }
    val isDeviceConnectedToWifi: Boolean
        /**
         * Utility method to check device wifi is enabled and connected to any access point.
         *
         * @return connection status of wifi
         */
        get() = wifiManager.dhcpInfo.ipAddress != 0

    @Throws(Exception::class)
    private fun setWifiApEnabled(wifiConfiguration: WifiConfiguration?, enable: Boolean) {
        val method = wifiManager.javaClass.getMethod(
            "setWifiApEnabled",
            WifiConfiguration::class.java,
            Boolean::class.javaPrimitiveType
        )
        method.invoke(wifiManager, wifiConfiguration, enable)
    }

    interface OnFailureListener {
        fun onFailure(failureCode: Int, e: Exception?)
    }

    interface OnSuccessListener {
        fun onSuccess(ssid: String, password: String)
    }

    private val randomPassword: String
        private get() {
            try {
                val ms = MessageDigest.getInstance("MD5")
                val bytes = ByteArray(10)
                Random().nextBytes(bytes)
                val digest = ms.digest(bytes)
                val bigInteger = BigInteger(1, digest)
                return bigInteger.toString(16).substring(0, 10)
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
            return "jfs82433#$2"
        }

    class Utils {
        fun checkLocationPermission(context: Context?): Boolean {
            return ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun askLocationPermission(activity: Activity?, requestCode: Int) {
            ActivityCompat.requestPermissions(
                activity!!, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), requestCode
            )
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun askWriteSettingPermission(activity: Activity) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.setData(Uri.parse("package:" + activity.packageName))
            activity.startActivity(intent)
        }

        @RequiresApi(Build.VERSION_CODES.M)
        fun checkWriteSettingPermission(context: Context): Boolean {
            return Settings.System.canWrite(context)
        }

        val tetheringSettingIntent: Intent
            get() {
                val intent = Intent()
                intent.setClassName("com.android.settings", "com.android.settings.TetherSettings")
                return intent
            }

        fun askForGpsProvider(activity: Activity) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            activity.startActivity(intent)
        }

        fun askForDisableWifi(activity: Activity) {
            activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    companion object {
        private var apManager: APManager? = null

        /**
         * @param context should not be null
         * @return APManager
         */
        fun getApManager(context: Context): APManager? {
            if (apManager == null) {
                apManager = APManager(context)
            }
            return apManager
        }

        /**
         * Some android version requires gps provider to be in active mode to create access point (Hotspot).
         */
        const val ERROR_GPS_PROVIDER_DISABLED = 0
        const val ERROR_LOCATION_PERMISSION_DENIED = 4
        const val ERROR_DISABLE_HOTSPOT = 1
        const val ERROR_DISABLE_WIFI = 5
        const val ERROR_WRITE_SETTINGS_PERMISSION_REQUIRED = 6
        const val ERROR_UNKNOWN = 3
    }
}
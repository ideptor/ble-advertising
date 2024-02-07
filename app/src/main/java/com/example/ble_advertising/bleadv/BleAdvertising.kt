package com.example.ble_advertising.bleadv

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.AdvertisingSetParameters.Builder
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.util.UUID


// https://source.android.com/docs/core/connect/bluetooth/ble_advertising?hl=ko

@RequiresApi(Build.VERSION_CODES.O)
fun bleAdvertising2(context: Context) {
    val adapter = BluetoothAdapter.getDefaultAdapter()
    val advertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
    val TAG = "BLE_ADVERTISING"
    var currentAdvertisingSet: AdvertisingSet? = null

    // Check if all features are supported
    if (!adapter.isLe2MPhySupported) {
        Log.e(TAG, "2M PHY not supported!")
        return
    }
    if (!adapter.isLeExtendedAdvertisingSupported) {
        Log.e(TAG, "LE Extended Advertising not supported!")
        return
    }
    val maxDataLength = adapter.leMaximumAdvertisingDataLength

    val parameters = Builder()
        .setLegacyMode(false)
        .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
        .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
        .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
        .setSecondaryPhy(BluetoothDevice.PHY_LE_2M)

    val data = AdvertiseData.Builder().addServiceData(
        ParcelUuid(UUID.randomUUID()),
        "You should be able to fit large amounts of data up to maxDataLength. This goes".toByteArray()
    ).build();

    val callback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(
            advertisingSet: AdvertisingSet,
            txPower: Int,
            status: Int
        ) {
            Log.w(
                TAG, "onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                        + status
            )
            currentAdvertisingSet = advertisingSet
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
            Log.i(TAG, "onAdvertisingSetStopped():")
        }
    }

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADVERTISE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        Log.e(TAG, "no permission")
        return
    }
    advertiser.startAdvertisingSet(parameters.build(), data, null, null, null, callback);

    // After the set starts, you can modify the data and parameters of currentAdvertisingSet.
    currentAdvertisingSet?.setAdvertisingData(
        AdvertiseData.Builder().addServiceData(
            ParcelUuid(UUID.randomUUID()),
            "Without disabling the advertiser first, you can set the data, if new data is".toByteArray()
        ).build())

    // Wait for onAdvertisingDataSet callback...

    // Can also stop and restart the advertising
    currentAdvertisingSet?.enableAdvertising(false, 0, 0);
    // Wait for onAdvertisingEnabled callback...
    currentAdvertisingSet?.enableAdvertising(true, 0, 0);
    // Wait for onAdvertisingEnabled callback...

    // Or modify the parameters - i.e. lower the tx power
    currentAdvertisingSet?.enableAdvertising(false, 0, 0);
    // Wait for onAdvertisingEnabled callback...
    currentAdvertisingSet?.setAdvertisingParameters(parameters.setTxPowerLevel
        (AdvertisingSetParameters.TX_POWER_LOW).build());
    // Wait for onAdvertisingParametersUpdated callback...
    currentAdvertisingSet?.enableAdvertising(true, 0, 0);
    // Wait for onAdvertisingEnabled callback...

    // When done with the advertising:
    advertiser.stopAdvertisingSet(callback);
}

@RequiresApi(Build.VERSION_CODES.O)
fun bleAdvertising(context: Context) {
    val TAG = "BLE_ADVERTISING"
    var currentAdvertisingSet: AdvertisingSet? = null
    val advertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
    val parameters = Builder()
//        .setLegacyMode(true) // True by default, but set here as a reminder.
//        .setConnectable(true)
        .setConnectable(false)
//        .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
        .setInterval(AdvertisingSetParameters.INTERVAL_MIN)
        .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
        .setScannable(true)
        .build()

    Log.w(TAG, "parameters: $parameters")
    val data = AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .build()
    Log.w(TAG, "data: $data")
    val callback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(
            advertisingSet: AdvertisingSet,
            txPower: Int,
            status: Int
        ) {
            Log.w(
                TAG, "onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                        + status
            )
            currentAdvertisingSet = advertisingSet
        }

        override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
            Log.w(
                TAG,
                "onAdvertisingDataSet() :status:$status"
            )
        }

        override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {
            Log.w(
                TAG,
                "onScanResponseDataSet(): status:$status"
            )
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
            Log.e(TAG, "onAdvertisingSetStopped():")
        }
    }
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_ADVERTISE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        Log.e(TAG, "no permission")
        return
    }
    Log.w(TAG, "start advertising - advertiser: $advertiser")
    advertiser?.startAdvertisingSet(parameters, data, null, null, null, callback)

    // After onAdvertisingSetStarted callback is called, you can modify the
    // advertising data and scan response data:
    Log.w(TAG, "start advertising with devicename - currentAdvertisingSet: $currentAdvertisingSet")
    currentAdvertisingSet?.setAdvertisingData(
        AdvertiseData.Builder().setIncludeDeviceName(true).setIncludeTxPowerLevel(true).build()
    )
    // Wait for onAdvertisingDataSet callback...
    Log.w(TAG, "start advertising with uuid")
    currentAdvertisingSet?.setScanResponseData(
        AdvertiseData.Builder().addServiceUuid(ParcelUuid(UUID.randomUUID())).build()
    )
    // Wait for onScanResponseDataSet callback...

    // When done with the advertising:
    advertiser.stopAdvertisingSet(callback)
    Log.w(TAG, "finish advertising")
}


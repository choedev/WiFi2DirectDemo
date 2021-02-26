package com.theoopusone.wifi2directdemo.rxWifiDirect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject

class RxWifiDirectModel: RxWifiDirectStateType {
    private var p2pManager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null

    override val isEnableWifiDirectSubject: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
    override val peerDevicesSubject: BehaviorSubject<List<WifiP2pDevice>> = BehaviorSubject.createDefault(
        arrayListOf())
    override val connectionDevicesSubject: BehaviorSubject<List<WifiP2pInfo>> = BehaviorSubject.createDefault(
        arrayListOf())
    override val myDeviceInfoSubject: BehaviorSubject<List<WifiP2pDevice>> = BehaviorSubject.createDefault(
        arrayListOf())


    override fun init(context: Context) {
        if (!context.applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "==>> PackageManager.FEATURE_WIFI_DIRECT error")
            return
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            Log.e(TAG, "==>> Context.WIFI_SERVICE error")
            return
        }

        if (!wifiManager.isP2pSupported) {
            Log.e(TAG, "==>> wifiManager.isP2pSupported error")
            return
        }

        p2pManager = context.applicationContext.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        if (p2pManager == null) {
            Log.e(TAG, "==>> Context.WIFI_P2P_SERVICE) error")
            return
        }

        channel = p2pManager?.initialize(context, Looper.getMainLooper(), null)
        if (channel == null) {
            Log.e(TAG, "==>> channel initialize error")
            return
        }

        Log.d(TAG, "==> p2p init")
        registerWifiDirectBroadcastReceiver(context.applicationContext)
    }

    override fun release(context: Context) {
        unregisterWifiDirectBroadcastReceiver(context)
    }

    override fun startDiscover(context: Context): Observable<Boolean> {
        return Observable.create {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                it.onNext(false)
                it.onComplete()
                return@create
            }

            p2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    it.onNext(true)
                    it.onComplete()
                }

                override fun onFailure(reason: Int) {
                    it.onNext(false)
                    it.onComplete()
                }
            })
        }
    }

    override fun stopDiscover(): Observable<Boolean> {
        return Observable.create {
            p2pManager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    it.onNext(true)
                    it.onComplete()
                }

                override fun onFailure(reason: Int) {
                    it.onNext(false)
                    it.onComplete()
                }
            })
        }
    }

    override fun connect(context: Context, config: WifiP2pConfig): Observable<Boolean> {
        stopDiscover()

        return Observable.create {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                it.onNext(false)
                it.onComplete()
                return@create
            }

            p2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    it.onNext(true)
                    it.onComplete()
                }

                override fun onFailure(reason: Int) {
                    it.onNext(false)
                    it.onComplete()
                }
            })
        }
    }

    override fun disconnect(): Observable<Boolean> {
        return Observable.create {
            p2pManager?.cancelConnect(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    it.onNext(true)
                    it.onComplete()
                }

                override fun onFailure(reason: Int) {
                    it.onNext(false)
                    it.onComplete()
                }
            })
        }
    }

    private fun registerWifiDirectBroadcastReceiver(context: Context) {
        if (p2pManager == null || channel == null) {
            return
        }

        val filter = IntentFilter()
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        context.applicationContext.registerReceiver(broadcastReceiver, filter)
    }

    private fun unregisterWifiDirectBroadcastReceiver(context: Context) {
        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) {
                return
            }

            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    isEnableWifiDirectSubject.onNext(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    if (ActivityCompat.checkSelfPermission(context.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "onReceive => WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION no permission")
                        return
                    }
                    p2pManager?.requestPeers(channel, peerListListener)
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        p2pManager?.requestConnectionInfo(channel, peerConnectionListener)

                    } else {
                        connectionDevicesSubject.onNext(arrayListOf())
                    }
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)?.apply {
                        myDeviceInfoSubject.onNext(arrayListOf(this))
                    }
                }
                else -> {}
            }
        }
    }

    private val peerListListener = WifiP2pManager.PeerListListener { list -> peerDevicesSubject.onNext(list.deviceList.toList()) }

    private  val peerConnectionListener =
        WifiP2pManager.ConnectionInfoListener { info -> connectionDevicesSubject.onNext(arrayListOf(info)) }

    companion object {
        const val TAG = "##WifiDirect## RxWifiDirectModel"
    }
}

package com.theoopusone.wifi2directdemo.rxWifiDirect

import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject


interface RxWifiDirectStateType {
    val isEnableWifiDirectSubject: BehaviorSubject<Boolean>
    val peerDevicesSubject: BehaviorSubject<List<WifiP2pDevice>>
    val connectionDevicesSubject: BehaviorSubject<List<WifiP2pInfo>>
    val myDeviceInfoSubject: BehaviorSubject<List<WifiP2pDevice>>

    fun init(context: Context)
    fun release(context: Context)

    fun startDiscover(context: Context): Observable<Boolean>
    fun stopDiscover(): Observable<Boolean>

    fun connect(context: Context, config: WifiP2pConfig): Observable<Boolean>
    fun disconnect(): Observable<Boolean>
}

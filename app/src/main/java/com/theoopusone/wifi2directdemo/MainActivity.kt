package com.theoopusone.wifi2directdemo

import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.pedro.library.AutoPermissions
import com.pedro.library.AutoPermissionsListener
import com.theoopusone.wifi2directdemo.rxWifiDirect.RxWifiDirectModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), AutoPermissionsListener {
    private var wifiDirectModel = RxWifiDirectModel()
    private val disposeBag = CompositeDisposable()

    private val peerItemClickSubject = PublishSubject.create<WifiP2pDevice>()
    private val peerAdapter = PeerListAdapter(peerItemClickSubject)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initRecyclerView()
        initRxWifiDirect()

        AutoPermissions.loadAllPermissions(this, 1)
    }

    override fun finish() {
        super.finish()

        wifiDirectModel.stopDiscover()
        wifiDirectModel.disconnect()
        wifiDirectModel.release(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        AutoPermissions.parsePermissions(this, requestCode, permissions, this)
    }

    override fun onDenied(requestCode: Int, permissions: Array<String>) {
        Log.d(TAG, "==> onDenied")
    }

    override fun onGranted(requestCode: Int, permissions: Array<String>) {
        Log.d(TAG, "==> onGranted");
    }

    private fun initRxWifiDirect() {
        wifiDirectModel.init(this)

        val isEnableWifiDirect = wifiDirectModel.isEnableWifiDirectSubject

        isEnableWifiDirect
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Log.d(TAG, "isEnableWifiDirect ==> ${it}")
            }
            .apply {
                disposeBag.add(this)
            }

        isEnableWifiDirect
            .observeOn(AndroidSchedulers.mainThread())
            .filter { it }
            .flatMap { wifiDirectModel.startDiscover(this) }
            .subscribe {
                Log.d(TAG, "discover state ==> ${it}")
            }
            .apply {
                disposeBag.add(this)
            }

        wifiDirectModel.peerDevicesSubject
            .observeOn(AndroidSchedulers.mainThread())
            .filter { it.isNotEmpty() }
            .subscribe {
                Log.d(TAG, "peer ==> ${it.size}")
                peerAdapter.updateItems(it)
            }
            .apply {
                disposeBag.add(this)
            }


        wifiDirectModel.connectionDevicesSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Log.d(TAG, "connection change ==> ${it.size}")
                progress.visibility = View.INVISIBLE
            }
            .apply {
                disposeBag.add(this)
            }

        wifiDirectModel.myDeviceInfoSubject
            .observeOn(AndroidSchedulers.mainThread())
            .filter { it.isNotEmpty() }
            .subscribe {
                supportActionBar?.title = it.last().deviceName
            }
            .apply {
                disposeBag.add(this)
            }

        peerItemClickSubject
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                progress.visibility = View.VISIBLE
                val config = WifiP2pConfig().apply {
                    deviceAddress = it.deviceAddress
                    wps.setup = WpsInfo.PBC
                }
                wifiDirectModel.connect(this, config)
            }
            .subscribe {
                Log.d(TAG, "click peer connect result ==> $it")
            }
            .apply {
                disposeBag.add(this)
            }
    }

    private fun initRecyclerView() {
        peerList.adapter = peerAdapter
        peerList.layoutManager = LinearLayoutManager(this)
    }


    companion object {
        const val TAG = "##WifiDirect## MainActivity"
    }
}

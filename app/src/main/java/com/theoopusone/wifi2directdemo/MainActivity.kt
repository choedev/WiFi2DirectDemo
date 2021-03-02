package com.theoopusone.wifi2directdemo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.JobIntentService
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding4.view.clicks
import com.pedro.library.AutoPermissions
import com.pedro.library.AutoPermissionsListener
import com.theoopusone.wifi2directdemo.rxWifiDirect.RxWifiDirectModel
import com.theoopusone.wifi2directdemo.transfer.FileTransferService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import java.io.*
import java.net.ServerSocket


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

        getSendImagePath()

        AutoPermissions.loadAllPermissions(this, 1)

        startFileServer(applicationContext)
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

    private fun getSendImagePath(): String {
        return Uri.parse("android.resource://$packageName${File.separator}${R.raw.elon}").toString()
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

                if (it.isNotEmpty()) {
                    sendImage.visibility = View.VISIBLE
                } else {
                    sendImage.visibility = View.GONE
                }
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

        sendImage
            .clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(  {
                Log.d(TAG, "start send image")

//                val intent = Intent()
//                intent.action = Intent.ACTION_VIEW
//                intent.setDataAndType(Uri.parse(getSendImagePath()), "image/*")
//                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                this.startActivity(intent)

                val uri = Uri.parse(getSendImagePath())
                Log.d(TAG, "send image uri: ${uri.toString()}")
                val serviceIntent = Intent(this, FileTransferService::class.java)
                serviceIntent.action = FileTransferService.ACTION_SEND_FILE
                serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString())

                val address =
                    wifiDirectModel.connectionDevicesSubject.value.last().groupOwnerAddress.hostAddress
                Log.d(TAG, "send image address: $address")
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, address)
                serviceIntent.putExtra(FileTransferService.EXTRAS_OWNER_PORT, 8988)
                //this.startService(intent)

                JobIntentService.enqueueWork(this, FileTransferService::class.java, 0x500, serviceIntent)


                Log.d(TAG, "end send image")
            }, {
                it.printStackTrace()
            }).apply {
                disposeBag.add(this)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "start send image5")

        val uri = Uri.parse(getSendImagePath())
        val serviceIntent = Intent(this, FileTransferService::class.java)
        serviceIntent.action = FileTransferService.ACTION_SEND_FILE
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString())

        val address =
            wifiDirectModel.connectionDevicesSubject.value.last().groupOwnerAddress.hostAddress
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, address)
        serviceIntent.putExtra(FileTransferService.EXTRAS_OWNER_PORT, 8988)
        this.startService(intent)
    }

    private fun initRecyclerView() {
        peerList.adapter = peerAdapter
        peerList.layoutManager = LinearLayoutManager(this)
    }

    private fun startFileServer(context: Context) {
        doAsync {
            GlobalScope.launch {
                Log.d(TAG, "start file server")

                try {
                    val serverSocket = ServerSocket(8988)
                    val client = serverSocket.accept()
                    val file = File(context.getExternalFilesDir("received"), "wifi2shared-${System.currentTimeMillis()}.png")
                    val dir = File(file.parent)
                    if (!dir.exists()) {
                        dir.mkdir()
                    }
                    file.createNewFile()

                    val inputStream = client.getInputStream()
                    copyFile(inputStream, FileOutputStream(file))
                    serverSocket.close()

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun copyFile(inputStream: InputStream, outputStream: OutputStream): Boolean {
        val buffer = ByteArray(1024)
        var len: Int = 0
        try {
            while (true) {
                len = inputStream.read(buffer)
                if (len < 0) {
                    break
                }
                outputStream.write(buffer, 0, len)
            }
            outputStream.close()
            inputStream.close()

        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    companion object {
        const val TAG = "##WifiDirect## MainActivity"
    }
}

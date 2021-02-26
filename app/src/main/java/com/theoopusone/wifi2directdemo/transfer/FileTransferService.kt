package com.theoopusone.wifi2directdemo.transfer


import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.Socket

class FileTransferService: JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        if (intent.action != ACTION_SEND_FILE) {
            return
        }

        val fileUri = intent.extras?.getString(EXTRAS_FILE_PATH)
        val host = intent.extras?.getString(EXTRAS_GROUP_OWNER_ADDRESS)
        val port = intent.extras?.getInt(EXTRAS_OWNER_PORT)

        if (fileUri == null || host == null || port == null) {
            Log.e(TAG, "FileTransferService::onHandleWork uri: $fileUri, host: $host, port: $port")
            return
        }


        val socket = Socket()
        try {
            socket.bind(null)
            socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT)


        } catch (e: Exception) {
            e.printStackTrace()

        } finally {

        }
    }

    companion object {
        const val TAG = "##WifiDirect## FileTransferService"

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, FileTransferService::class.java, 1, intent)
        }

        const val SOCKET_TIMEOUT = 5000
        const val ACTION_SEND_FILE = "com.github.choedev.wifidirect.send_file"
        const val EXTRAS_FILE_PATH = "file_url"
        const val EXTRAS_GROUP_OWNER_ADDRESS = "go_host"
        const val EXTRAS_OWNER_PORT = "go_port"
    }
}
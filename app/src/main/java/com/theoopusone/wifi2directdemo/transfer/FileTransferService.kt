package com.theoopusone.wifi2directdemo.transfer

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.JobIntentService
import com.theoopusone.wifi2directdemo.rxWifiDirect.FileCopyType
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

class FileTransferService: JobIntentService(), FileCopyType {

    override fun onHandleWork(intent: Intent) {
        Log.d(TAG, "onHandleWork!")


        if (intent.action != ACTION_SEND_FILE) {
            Log.e(TAG, "action error: ${intent.action}")
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
            Log.d(TAG, "Opening client socket - ")

            socket.bind(null)
            socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT)

            Log.d(TAG, "Client socket - ${socket.isConnected}")
            val outputStream = socket.getOutputStream()
            val contentResolver  = applicationContext.contentResolver
            var inputStream: InputStream? = null
            try {
                inputStream = contentResolver.openInputStream(Uri.parse(fileUri))
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "InputStream error: ${e.message}")
                e.printStackTrace()
            }

            inputStream?.let {
                copyFile(inputStream, outputStream)
            }

            Log.d(TAG, "Client data written")

        } catch (e: IOException) {
            Log.e(TAG, "Socket error: ${e.message}")
            e.printStackTrace()

        } finally {
            if (socket.isConnected) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        const val TAG = "##WifiDirect## FileTransferService"

        const val SOCKET_TIMEOUT = 5000
        const val ACTION_SEND_FILE = "com.github.choedev.wifidirect.send_file"
        const val EXTRAS_FILE_PATH = "file_url"
        const val EXTRAS_GROUP_OWNER_ADDRESS = "go_host"
        const val EXTRAS_OWNER_PORT = "go_port"
    }
}